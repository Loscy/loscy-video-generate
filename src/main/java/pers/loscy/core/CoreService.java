package pers.loscy.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.ice20201109.Client;
import com.aliyun.ice20201109.models.*;
import com.aliyun.teaopenapi.models.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.loscy.AliUpload;
import pers.loscy.deepseek.DeepSeekAi;
import pers.loscy.image.ImageGenerate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 短视频生成核心服务
 * 整合DeepSeek AI脚本生成、阿里云文生图、阿里云ICE视频生成
 * @author 徐天
 * @create 2025/7/9 11:42
 */
public class CoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(CoreService.class);

    private static final String REGION_ID = "cn-shanghai";
    private static final String BUCKET = "loscyaivideo";
    private Client iceClient;
    
    // 服务实例
    private DeepSeekAi deepSeekAi;
    private ImageGenerate imageGenerate;
    private AliUpload aliUpload;
    
    // 异步图片生成线程池
    private ExecutorService imageGenerationExecutor;
    
    /**
     * 短视频脚本结构
     */
    public static class VideoScript {
        private String title;
        private List<Scene> scenes;
        
        public VideoScript() {}
        
        public VideoScript(String title, List<Scene> scenes) {
            this.title = title;
            this.scenes = scenes;
        }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<Scene> getScenes() { return scenes; }
        public void setScenes(List<Scene> scenes) { this.scenes = scenes; }
        
        @Override
        public String toString() {
            return "VideoScript{" +
                    "title='" + title + '\'' +
                    ", scenes=" + scenes +
                    '}';
        }
    }
    
    /**
     * 场景结构
     */
    public static class Scene {
        private String sceneName;
        private String imageDescription;
        private String scriptText;
        private String imageUrl;
        
        public Scene() {}
        
        public Scene(String sceneName, String imageDescription, String scriptText) {
            this.sceneName = sceneName;
            this.imageDescription = imageDescription;
            this.scriptText = scriptText;
        }
        
        public String getSceneName() { return sceneName; }
        public void setSceneName(String sceneName) { this.sceneName = sceneName; }
        public String getImageDescription() { return imageDescription; }
        public void setImageDescription(String imageDescription) { this.imageDescription = imageDescription; }
        public String getScriptText() { return scriptText; }
        public void setScriptText(String scriptText) { this.scriptText = scriptText; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        
        @Override
        public String toString() {
            return "Scene{" +
                    "sceneName='" + sceneName + '\'' +
                    ", imageDescription='" + imageDescription + '\'' +
                    ", scriptText='" + scriptText + '\'' +
                    ", imageUrl='" + imageUrl + '\'' +
                    '}';
        }
    }
    
    /**
     * 视频生成结果
     */
    public static class VideoGenerationResult {
        private String jobId;
        private String status;
        private String videoUrl;
        private VideoScript script;
        private List<String> imageUrls;
        private String errorMessage;
        
        public VideoGenerationResult() {}
        
        public String getJobId() { return jobId; }
        public void setJobId(String jobId) { this.jobId = jobId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getVideoUrl() { return videoUrl; }
        public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
        public VideoScript getScript() { return script; }
        public void setScript(VideoScript script) { this.script = script; }
        public List<String> getImageUrls() { return imageUrls; }
        public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        @Override
        public String toString() {
            return "VideoGenerationResult{" +
                    "jobId='" + jobId + '\'' +
                    ", status='" + status + '\'' +
                    ", videoUrl='" + videoUrl + '\'' +
                    ", script=" + script +
                    ", imageUrls=" + imageUrls +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }
    
    /**
     * 构造函数
     */
    public CoreService() {
        initServices();
    }

    /**
     * 初始化ICE客户端
     */
    private void initIceClient() throws Exception {
        com.aliyun.credentials.Client credentialClient = new com.aliyun.credentials.Client();

        Config config = new Config();
        config.setCredential(credentialClient);
        config.accessKeyId = "your_oss_access_key_id_here";
        config.accessKeySecret = "your_oss_access_key_secret_here";
        config.endpoint = "ice." + REGION_ID + ".aliyuncs.com";
        config.regionId = REGION_ID;

        iceClient = new Client(config);
    }
    
    /**
     * 初始化所有服务
     */
    private void initServices() {
        try {
            // 初始化DeepSeek AI
            deepSeekAi = new DeepSeekAi(pers.loscy.deepseek.DeepSeekConfig.getApiKey());
            logger.info("DeepSeek AI服务初始化成功");
            
            // 初始化文生图服务
            imageGenerate = new ImageGenerate();
            logger.info("文生图服务初始化成功");
            
            // 初始化OSS上传服务
            aliUpload = new AliUpload();
            logger.info("OSS上传服务初始化成功");

            // 初始化ICE视频生成客户端
            initIceClient();
            logger.info("ICE视频生成服务初始化成功");
            
            // 初始化异步图片生成线程池
            imageGenerationExecutor = Executors.newFixedThreadPool(5);
            logger.info("异步图片生成线程池初始化成功");
            
        } catch (Exception e) {
            logger.error("服务初始化失败", e);
            throw new RuntimeException("服务初始化失败", e);
        }
    }
    

    
    /**
     * 主要入口方法：根据文本生成短视频
     * @param inputText 输入的文本内容
     * @return 视频生成结果
     */
    public VideoGenerationResult generateVideoFromText(String inputText) {
        logger.info("开始生成短视频，输入文本: {}", inputText);
        
        VideoGenerationResult result = new VideoGenerationResult();
        
        try {
            // 步骤1: 使用DeepSeek生成短视频脚本
            logger.info("步骤1: 生成短视频脚本");
            VideoScript script = generateVideoScript(inputText);
            result.setScript(script);
            logger.info("脚本生成成功: {}", script);
            
            // 步骤2: 根据图片描述生成图片
            logger.info("步骤2: 生成图片");
            List<String> imageUrls = generateImages(script);
            result.setImageUrls(imageUrls);
            logger.info("图片生成成功，共{}张", imageUrls.size());
            
            // 步骤3: 模拟视频生成
            logger.info("步骤3: 模拟视频生成");
            String jobId = generateVideo(script, imageUrls);
            result.setJobId(jobId);
            result.setStatus("Processing");
            logger.info("视频生成任务提交成功，任务ID: {}", jobId);
            
            return result;
            
        } catch (Exception e) {
            logger.error("视频生成失败", e);
            result.setStatus("Failed");
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }
    
    /**
     * 使用DeepSeek生成短视频脚本
     * @param inputText 输入文本
     * @return 视频脚本
     */
    public VideoScript generateVideoScript(String inputText) throws IOException {
        String prompt = String.format(
            "请根据以下内容生成一个短视频脚本，要求：\n" +
            "1. 生成一个吸引人的标题 且 不能包含Emoji标签\n" +
            "2. 将内容分解为3-5个场景\n" +
            "3. 每个场景包含图片描述和对应的文案\n" +
            "4. 文案要简洁有力，适合短视频\n" +
            "5. 图片描述要具体，便于AI生成图片\n" +
            "6. 返回JSON格式不需要用markdown代码块标记 并且value中需要特殊符号需要转译，格式如下：\n" +
            "{\n" +
            "  \"title\": \"视频标题\",\n" +
            "  \"scenes\": [\n" +
            "    {\n" +
            "      \"sceneName\": \"开场\",\n" +
            "      \"imageDescription\": \"具体的图片描述\",\n" +
            "      \"scriptText\": \"对应的文案内容(要求35字以上)\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "\n" +
            "内容：%s",
            inputText
        );
        
        String response = deepSeekAi.simpleChat(prompt);
        logger.info("DeepSeek响应: {}", response);
        
        // 解析JSON响应
        try {
            // 尝试清理响应文本，移除可能的markdown代码块标记
            String cleanResponse = response.trim();
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.startsWith("```")) {
                cleanResponse = cleanResponse.substring(3);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            cleanResponse = cleanResponse.trim();
            
            logger.info("清理后的响应: {}", cleanResponse);
            
            JSONObject jsonResponse = JSON.parseObject(cleanResponse);
            VideoScript script = new VideoScript();
            
            // 获取标题
            String title = jsonResponse.getString("title");
            if (title == null || title.trim().isEmpty()) {
                throw new RuntimeException("JSON中缺少title字段");
            }
            script.setTitle(title);
            logger.info("解析到标题: {}", title);
            
            // 获取场景数组
            JSONArray scenesArray = jsonResponse.getJSONArray("scenes");
            if (scenesArray == null) {
                throw new RuntimeException("JSON中缺少scenes字段");
            }
            
            List<Scene> scenes = new ArrayList<>();
            for (int i = 0; i < scenesArray.size(); i++) {
                JSONObject sceneJson = scenesArray.getJSONObject(i);
                
                String sceneName = sceneJson.getString("sceneName");
                String imageDescription = sceneJson.getString("imageDescription");
                String scriptText = sceneJson.getString("scriptText");
                
                if (sceneName == null || imageDescription == null || scriptText == null) {
                    logger.warn("场景{}缺少必要字段: sceneName={}, imageDescription={}, scriptText={}", 
                              i, sceneName, imageDescription, scriptText);
                    continue;
                }
                
                Scene scene = new Scene(sceneName, imageDescription, scriptText);
                scenes.add(scene);
                logger.info("解析到场景{}: {}", i + 1, sceneName);
            }
            
            if (scenes.isEmpty()) {
                throw new RuntimeException("没有解析到有效的场景");
            }
            
            script.setScenes(scenes);
            logger.info("成功解析脚本，共{}个场景", scenes.size());
            return script;
            
        } catch (Exception e) {
            logger.error("解析脚本JSON失败", e);
            logger.error("原始响应内容: {}", response);
            throw e;
        }
    }
    
    /**
     * 根据脚本生成图片（异步并发）
     * @param script 视频脚本
     * @return 图片URL列表
     */
    public List<String> generateImages(VideoScript script) {
        List<String> imageUrls = new ArrayList<>();
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        logger.info("开始异步生成{}张图片", script.getScenes().size());
        
        // 为每个场景创建异步任务
        for (int i = 0; i < script.getScenes().size(); i++) {
            Scene scene = script.getScenes().get(i);
            final int sceneIndex = i;
            
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 添加递增延迟，避免请求频率限制
                    // 每个场景延迟时间递增：场景1延迟1秒，场景2延迟2秒，以此类推
                    int baseDelayMs = 1000; // 基础延迟1秒
                    int incrementalDelayMs = sceneIndex * 1000; // 每个场景递增1秒
                    int randomDelayMs = new Random().nextInt(1000); // 随机延迟0-1秒
                    int totalDelayMs = baseDelayMs + incrementalDelayMs + randomDelayMs;
                    
                    logger.info("场景{}等待{}ms后开始生成图片 (基础{}ms + 递增{}ms + 随机{}ms)", 
                              sceneIndex + 1, totalDelayMs, baseDelayMs, incrementalDelayMs, randomDelayMs);
                    Thread.sleep(totalDelayMs);
                    
                    logger.info("开始生成场景{}的图片: {}", sceneIndex + 1, scene.getImageDescription());
                    
                    // 调用文生图API
                    ImageGenerate.ImageGenerateResult imageResult = imageGenerate.generateImage(
                        scene.getImageDescription(),
                        "wanx2.1-t2i-turbo",  // 模型名称
                        1024,  // 宽度
                        768,  // 高度
                        20,    // 步数
                        null   // 随机种子
                    );
                    
                    // 下载图片并上传到OSS
                    String ossUrl = downloadAndUploadToOSS(imageResult.getImageUrl(), imageResult.getTaskId());
                    
                    logger.info("场景{}图片生成成功: {}", sceneIndex + 1, ossUrl);
                    return ossUrl;
                    
                } catch (Exception e) {
                    logger.error("生成场景{}图片失败: {}", sceneIndex + 1, scene.getImageDescription(), e);
                    // 使用默认图片
                    try {
                        String defaultImageUrl = aliUpload.uploadResourceFile("img/error-base.png");
                        logger.info("场景{}使用默认图片: {}", sceneIndex + 1, defaultImageUrl);
                        return defaultImageUrl;
                    } catch (Exception uploadException) {
                        logger.error("上传默认图片失败", uploadException);
                        return null;
                    }
                }
            }, imageGenerationExecutor);
            
            futures.add(future);
        }
        
        // 等待所有异步任务完成
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            // 设置超时时间（5分钟）
            allFutures.get(5, TimeUnit.MINUTES);
            
            // 收集结果
            for (int i = 0; i < futures.size(); i++) {
                try {
                    String imageUrl = futures.get(i).get();
                    if (imageUrl != null) {
                        imageUrls.add(imageUrl);
                        script.getScenes().get(i).setImageUrl(imageUrl);
                    } else {
                        // 如果获取失败，使用默认图片
                        String defaultImageUrl = aliUpload.uploadResourceFile("img/error-base.png");
                        imageUrls.add(defaultImageUrl);
                        script.getScenes().get(i).setImageUrl(defaultImageUrl);
                    }
                } catch (Exception e) {
                    logger.error("获取场景{}图片结果失败", i + 1, e);
                    // 使用默认图片
                    try {
                        String defaultImageUrl = aliUpload.uploadResourceFile("img/error-base.png");
                        imageUrls.add(defaultImageUrl);
                        script.getScenes().get(i).setImageUrl(defaultImageUrl);
                    } catch (Exception uploadException) {
                        logger.error("上传默认图片失败", uploadException);
                        // 添加空字符串作为占位符
                        imageUrls.add("");
                        script.getScenes().get(i).setImageUrl("");
                    }
                }
            }
            
            logger.info("所有图片生成完成，共生成{}张图片", imageUrls.size());
            
        } catch (Exception e) {
            logger.error("异步图片生成过程中发生错误", e);
            // 处理超时或其他异常，为未完成的场景使用默认图片
            for (int i = imageUrls.size(); i < script.getScenes().size(); i++) {
                try {
                    String defaultImageUrl = aliUpload.uploadResourceFile("img/error-base.png");
                    imageUrls.add(defaultImageUrl);
                    script.getScenes().get(i).setImageUrl(defaultImageUrl);
                } catch (Exception uploadException) {
                    logger.error("上传默认图片失败", uploadException);
                    imageUrls.add("");
                    script.getScenes().get(i).setImageUrl("");
                }
            }
        }
        
        return imageUrls;
    }
    
    /**
     * 下载图片并上传到OSS
     * @param imageUrl 原始图片URL
     * @param taskId 任务ID
     * @return OSS存储URL
     */
    private String downloadAndUploadToOSS(String imageUrl, String taskId) throws Exception {
        // 生成OSS对象名
        String timestamp = String.valueOf(System.currentTimeMillis());
        String objectName = "ai-generated/" + timestamp + "_" + taskId + ".jpg";
        
        // 下载图片并上传到OSS
        try (java.io.InputStream inputStream = new java.net.URL(imageUrl).openStream()) {
            // 创建临时文件
            java.io.File tempFile = java.io.File.createTempFile("ai_image_", ".jpg");
            tempFile.deleteOnExit();
            
            // 将输入流写入临时文件
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            // 上传临时文件到OSS
            String ossUrl = aliUpload.uploadLocalFile(tempFile.getAbsolutePath(), objectName);
            
            // 删除临时文件
            tempFile.delete();
            
            return ossUrl;
        }
    }



    /**
     * 生成短视频（使用脚本中已有的图片URL）
     * @param script 视频脚本
     * @return 任务ID
     */
    public String generateVideo(VideoScript script) throws Exception {
        // 从脚本中提取已有的图片URL
        List<String> imageUrls = new ArrayList<>();
        for (Scene scene : script.getScenes()) {
            if (scene.getImageUrl() != null && !scene.getImageUrl().trim().isEmpty()) {
                imageUrls.add(scene.getImageUrl());
            } else {
                throw new RuntimeException("没有可用的图片URL");
            }
        }
        // 生成视频
        return generateVideo(script, imageUrls);
    }
    
    /**
     * 生成短视频
     * @param script 视频脚本
     * @param imageUrls 图片URL列表
     * @return 任务ID
     */
    public String generateVideo(VideoScript script, List<String> imageUrls) throws Exception {
        // 构建媒体组数组
        JSONArray mediaGroupArray = new JSONArray();

        for (int i = 0; i < script.getScenes().size(); i++) {
            Scene scene = script.getScenes().get(i);
            String imageUrl = i < imageUrls.size() ? imageUrls.get(i) : imageUrls.get(0);

            JSONObject mediaGroup = new JSONObject();
            mediaGroup.put("GroupName", "part" + (i + 1));
            mediaGroup.put("MediaArray", Collections.singletonList(imageUrl));
            mediaGroup.put("SpeechTextArray", Collections.singletonList(scene.getScriptText()));
            mediaGroup.put("SplitMode", "NoSplit");

            mediaGroupArray.add(mediaGroup);
        }

        // 构建输入配置
        JSONObject inputConfig = new JSONObject();
        inputConfig.put("MediaGroupArray", mediaGroupArray);
        inputConfig.put("TitleArray", Collections.singletonList("Shippergrid 跨境资讯"));
        inputConfig.put("BackgroundMusicArray", Collections.singletonList(
                "https://loscyaivideo.oss-cn-shanghai.aliyuncs.com/M500001CqAt10mHS6V.mp3"
        ));
        inputConfig.put("BackgroundImageArray", Collections.singletonList("https://loscyaivideo.oss-cn-shanghai.aliyuncs.com/%E6%A8%A1%E7%89%88.gif"));

        // 构建输出配置
        JSONObject outputConfig = new JSONObject();
        String mediaUrl = "http://" + BUCKET + ".oss-" + REGION_ID + ".aliyuncs.com/ice_output/" +
                System.currentTimeMillis() + script.getTitle() + "_{index}.mp4";

        outputConfig.put("MediaURL", mediaUrl);
        outputConfig.put("Count", 1);
        outputConfig.put("Width", 720);   // 竖屏
        outputConfig.put("Height", 1280);

        // 构建编辑配置
        JSONObject editingConfig = new JSONObject();
        JSONObject processConfig = new JSONObject();
        processConfig.put("AllowVfxEffect", false);
        processConfig.put("AllowTransition", true);
        processConfig.put("TransitionList", Arrays.asList("bounce_up", "bounce_down"));
        processConfig.put("UseUniformTransition", false);
        processConfig.put("ImageDuration", 60);
        JSONObject speechConfig = new JSONObject();
        speechConfig.put("SpeechRate", 130);
        speechConfig.put("Voice", "zhilun");
        JSONObject titleConfig = new JSONObject();
        titleConfig.put("FontSize", 85);
        editingConfig.put("SpeechConfig", speechConfig);
        editingConfig.put("ProcessConfig", processConfig);
        editingConfig.put("TitleConfig", titleConfig);

        // 提交任务
        SubmitBatchMediaProducingJobRequest request = new SubmitBatchMediaProducingJobRequest();
        request.setInputConfig(inputConfig.toJSONString());
        request.setOutputConfig(outputConfig.toJSONString());
        request.setEditingConfig(editingConfig.toJSONString());

        SubmitBatchMediaProducingJobResponse response = iceClient.submitBatchMediaProducingJob(request);
        String jobId = response.getBody().getJobId();

        logger.info("视频生成任务提交成功，任务ID: {}", jobId);
        return jobId;
    }

    /**
     * 查询视频生成状态
     * @param jobId 任务ID
     * @return 视频生成结果
     */
    public VideoGenerationResult queryVideoStatus(String jobId) {
        VideoGenerationResult result = new VideoGenerationResult();
        result.setJobId(jobId);

        try {
            GetBatchMediaProducingJobRequest getRequest = new GetBatchMediaProducingJobRequest();
            getRequest.setJobId(jobId);

            GetBatchMediaProducingJobResponse getResponse = iceClient.getBatchMediaProducingJob(getRequest);
            String status = getResponse.getBody().getEditingBatchJob().getStatus();

            result.setStatus(status);

            if ("Finished".equals(status)) {
                List<GetBatchMediaProducingJobResponseBody.GetBatchMediaProducingJobResponseBodyEditingBatchJobSubJobList> subJobList = getResponse.getBody().getEditingBatchJob().getSubJobList();
                result.setVideoUrl(subJobList.get(0).getMediaURL());
                logger.info("原始报文 {}", JSON.toJSONString(getResponse.getBody()));
                logger.info("视频生成完成: {}", result.getVideoUrl());
            } else if ("Failed".equals(status)) {
                result.setErrorMessage("视频生成失败");
                logger.info("原始报文 {}", JSON.toJSONString(getResponse.getBody()));
                logger.error("视频生成失败，任务ID: {}", jobId);
            }

        } catch (Exception e) {
            logger.error("查询视频状态失败", e);
            result.setStatus("Error");
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * 等待视频生成完成
     * @param jobId 任务ID
     * @param timeoutSeconds 超时时间（秒）
     * @return 视频生成结果
     */
    public VideoGenerationResult waitForVideoCompletion(String jobId, int timeoutSeconds) {
        logger.info("等待视频生成完成，任务ID: {}, 超时时间: {}秒", jobId, timeoutSeconds);

        int maxRetries = timeoutSeconds / 3; // 每3秒查询一次
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Thread.sleep(3000);
                VideoGenerationResult result = queryVideoStatus(jobId);

                if ("Finished".equals(result.getStatus())) {
                    logger.info("视频生成完成");
                    return result;
                } else if ("Failed".equals(result.getStatus())) {
                    logger.error("视频生成失败");
                    return result;
                }

                logger.info("视频生成进度: {}/{}", retryCount + 1, maxRetries);
                retryCount++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                VideoGenerationResult result = new VideoGenerationResult();
                result.setJobId(jobId);
                result.setStatus("Interrupted");
                result.setErrorMessage("任务被中断");
                return result;
            }
        }

        // 超时
        VideoGenerationResult result = new VideoGenerationResult();
        result.setJobId(jobId);
        result.setStatus("Timeout");
        result.setErrorMessage("视频生成超时");
        return result;
    }

    /**
     * 关闭服务
     */
    public void close() {
        try {
            if (deepSeekAi != null) {
                deepSeekAi.close();
            }
            if (aliUpload != null) {
                aliUpload.close();
            }
            if (imageGenerationExecutor != null) {
                imageGenerationExecutor.shutdown();
                if (!imageGenerationExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    imageGenerationExecutor.shutdownNow();
                }
            }
            logger.info("所有服务已关闭");
        } catch (Exception e) {
            logger.error("关闭服务时发生错误", e);
        }
    }
} 