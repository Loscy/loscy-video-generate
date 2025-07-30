package pers.loscy.image;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 阿里云百炼文生图服务
 * @author 徐天
 * @create 2025/7/9 11:12
 */
public class ImageGenerate {

    private static final Logger logger = LoggerFactory.getLogger(ImageGenerate.class);

    private Properties config;
    private String apiKey;
    private String endpoint;
    private String region;

    public ImageGenerate() {
        initConfig();
    }

    /**
     * 初始化配置
     */
    private void initConfig() {
        config = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("oss-config.properties")) {
            if (input == null) {
                throw new RuntimeException("无法找到配置文件 oss-config.properties");
            }
            config.load(input);

            apiKey = getConfigValue("bailian.apiKey", "BAILIAN_API_KEY");
            endpoint = getConfigValue("bailian.endpoint", "BAILIAN_ENDPOINT");
            region = getConfigValue("bailian.region", "BAILIAN_REGION");

            if (!isConfigured(apiKey) || !isConfigured(endpoint) || !isConfigured(region)) {
                throw new RuntimeException("百炼配置信息不完整");
            }

            logger.info("百炼配置加载成功");
        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败", e);
        }
    }

    private String getConfigValue(String propertyName, String envName) {
        String value = System.getenv(envName);
        if (value == null || value.trim().isEmpty()) {
            value = config.getProperty(propertyName);
        }
        return value == null ? null : value.trim();
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty() && !value.trim().startsWith("your_");
    }

    /**
     * 文生图调用
     * @param prompt 提示词
     * @param modelName 模型名称，默认为 wanx2.1-t2i-turbo
     * @param width 图片宽度，默认1024
     * @param height 图片高度，默认1024
     * @param steps 生成步数，默认20
     * @param seed 随机种子，默认-1
     * @return 生成结果
     */
    public ImageGenerateResult generateImage(String prompt, String modelName, Integer width,
                                             Integer height, Integer steps, Long seed) {
        try {
            // 构建输入参数
            JSONObject input = new JSONObject();
            input.put("prompt", prompt);

            // 构建请求参数
            JSONObject parameters = new JSONObject();
            String size = (width != null ? width : 1024) + "*" + (height != null ? height : 1024);
            parameters.put("size", size);
            parameters.put("n", 1);

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", modelName != null ? modelName : "wanx2.1-t2i-turbo");
            requestBody.put("input", input);
            requestBody.put("parameters", parameters);

            logger.info("开始调用文生图API，参数: {}", requestBody.toJSONString());

            // 调用创建任务API
            String createTaskUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";
            String createTaskResponse = callBailianAPI(createTaskUrl, requestBody.toJSONString(), "POST");

            JSONObject createTaskJson = JSON.parseObject(createTaskResponse);
            JSONObject output = createTaskJson.getJSONObject("output");
            if (output == null) {
                logger.info("❌ 失败完整报文 {}", createTaskResponse);
                throw new RuntimeException("创建任务失败: 响应格式错误");
            }

            String taskId = output.getString("task_id");
            String taskStatus = output.getString("task_status");
            logger.info("任务状态: {}, 任务ID: {}", taskStatus, taskId);
            logger.info("文生图任务创建成功，任务ID: {}", taskId);

            // 轮询获取结果
            return pollTaskResult(taskId);

        } catch (Exception e) {
            logger.error("文生图调用失败", e);
            throw new RuntimeException("文生图调用失败", e);
        }
    }

    /**
     * 简化版文生图调用
     * @param prompt 提示词
     * @return 生成结果
     */
    public ImageGenerateResult generateImage(String prompt) {
        return generateImage(prompt, null, null, null, null, null);
    }

    /**
     * 轮询任务结果
     * @param taskId 任务ID
     * @return 生成结果
     */
    private ImageGenerateResult pollTaskResult(String taskId) {
        int maxRetries = 60; // 最大重试次数
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Thread.sleep(2000); // 等待2秒

                String getTaskUrl = "https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId;
                String getTaskResponse = callBailianAPI(getTaskUrl, null, "GET");

                JSONObject getTaskJson = JSON.parseObject(getTaskResponse);
                JSONObject output = getTaskJson.getJSONObject("output");
                if (output == null) {
                    throw new RuntimeException("获取任务状态失败: 响应格式错误");
                }

                String status = output.getString("task_status");
                logger.info("任务状态: {}, 任务ID: {}", status, taskId);

                if ("SUCCEEDED".equals(status)) {
                    // 任务成功，解析结果
                    return parseTaskResult(output);
                } else if ("FAILED".equals(status)) {
                    String errorMessage = output.getString("error_message");
                    throw new RuntimeException("文生图任务失败: " + (errorMessage != null ? errorMessage : "未知错误"));
                }
                // 其他状态继续等待

                retryCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("任务轮询被中断", e);
            } catch (Exception e) {
                logger.error("轮询任务结果失败", e);
                retryCount++;
            }
        }

        throw new RuntimeException("任务超时，请检查任务状态");
    }

    /**
     * 解析任务结果
     * @param output 任务输出数据
     * @return 生成结果
     */
    private ImageGenerateResult parseTaskResult(JSONObject output) {
        try {
            ImageGenerateResult imageResult = new ImageGenerateResult();
            imageResult.setTaskId(output.getString("task_id"));
            imageResult.setStatus(output.getString("task_status"));

            // 解析结果数据 - 根据实际API响应格式
            JSONArray results = output.getJSONArray("results");
            if (results != null && results.size() > 0) {
                JSONObject firstResult = results.getJSONObject(0);
                imageResult.setPrompt(firstResult.getString("orig_prompt"));
                imageResult.setImageUrl(firstResult.getString("url"));
                imageResult.setActualPrompt(firstResult.getString("actual_prompt"));
            }

            logger.info("文生图任务完成，任务ID: {}, 图片URL: {}",
                    imageResult.getTaskId(), imageResult.getImageUrl());
            return imageResult;

        } catch (Exception e) {
            logger.error("解析任务结果失败", e);
            throw new RuntimeException("解析任务结果失败", e);
        }
    }

    /**
     * 调用百炼API
     * @param url API地址
     * @param body 请求体
     * @param method HTTP方法
     * @return 响应结果
     */
    private String callBailianAPI(String url, String body, String method) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            org.apache.http.client.methods.HttpUriRequest request;

            if ("GET".equalsIgnoreCase(method)) {
                request = new org.apache.http.client.methods.HttpGet(url);
            } else {
                HttpPost httpPost = new HttpPost(url);
                // 设置请求体
                if (body != null && !body.isEmpty()) {
                    httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
                }
                request = httpPost;
            }

            // 设置请求头 - 使用官方API规范
            request.setHeader("Content-Type", "application/json");
            if ("POST".equalsIgnoreCase(method)) {
                request.setHeader("X-DashScope-Async", "enable");
            }
            request.setHeader("Authorization", "Bearer " + apiKey);

            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);

                logger.debug("API响应: {}", responseBody);
                return responseBody;
            }
        }
    }



    /**
     * 图片生成结果类
     */
    public static class ImageGenerateResult {
        private String taskId;
        private String status;
        private String prompt;
        private String actualPrompt;
        private String imageUrl;
        private Long seed;
        private String modelName;

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }

        public String getActualPrompt() { return actualPrompt; }
        public void setActualPrompt(String actualPrompt) { this.actualPrompt = actualPrompt; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public Long getSeed() { return seed; }
        public void setSeed(Long seed) { this.seed = seed; }

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        @Override
        public String toString() {
            return "ImageGenerateResult{" +
                    "taskId='" + taskId + '\'' +
                    ", status='" + status + '\'' +
                    ", prompt='" + prompt + '\'' +
                    ", actualPrompt='" + actualPrompt + '\'' +
                    ", imageUrl='" + imageUrl + '\'' +
                    ", seed=" + seed +
                    ", modelName='" + modelName + '\'' +
                    '}';
        }
    }
}
