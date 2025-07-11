package pers.loscy.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * DeepSeek AI API客户端
 * @author 徐天
 * @create 2025/7/9 11:32
 */
public class DeepSeekAi {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekAi.class);

    // API配置
    private static final String API_BASE_URL = "https://api.deepseek.com";
    private static final String CHAT_COMPLETIONS_ENDPOINT = "/v1/chat/completions";

    private final String apiKey;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     * @param apiKey DeepSeek API密钥
     */
    public DeepSeekAi(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
        // 配置ObjectMapper忽略未知字段
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 聊天消息类
     */
    public static class Message {
        private String role;
        private String content;

        // 默认构造函数，用于Jackson反序列化
        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    /**
     * 聊天响应类
     */
    public static class ChatResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;

        // 默认构造函数，用于Jackson反序列化
        public ChatResponse() {}

        public static class Choice {
            private int index;
            private Message message;
            private String finishReason;

            // 默认构造函数，用于Jackson反序列化
            public Choice() {}

            public int getIndex() { return index; }
            public void setIndex(int index) { this.index = index; }
            public Message getMessage() { return message; }
            public void setMessage(Message message) { this.message = message; }
            public String getFinishReason() { return finishReason; }
            public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
        }

        public static class Usage {
            private int promptTokens;
            private int completionTokens;
            private int totalTokens;

            // 默认构造函数，用于Jackson反序列化
            public Usage() {}

            public int getPromptTokens() { return promptTokens; }
            public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
            public int getCompletionTokens() { return completionTokens; }
            public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
            public int getTotalTokens() { return totalTokens; }
            public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getObject() { return object; }
        public void setObject(String object) { this.object = object; }
        public long getCreated() { return created; }
        public void setCreated(long created) { this.created = created; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<Choice> getChoices() { return choices; }
        public void setChoices(List<Choice> choices) { this.choices = choices; }
        public Usage getUsage() { return usage; }
        public void setUsage(Usage usage) { this.usage = usage; }
    }

    /**
     * 聊天完成请求参数
     */
    public static class ChatRequest {
        private String model = "deepseek-chat";
        private List<Message> messages;
        private double temperature = 0.7;
        private int maxTokens = 1000;
        private boolean stream = false;

        // 默认构造函数，用于Jackson序列化
        public ChatRequest() {}

        public ChatRequest(List<Message> messages) {
            this.messages = messages;
        }

        // Getters and setters
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public boolean isStream() { return stream; }
        public void setStream(boolean stream) { this.stream = stream; }
    }

    /**
     * 发送聊天请求（非流式）
     * @param request 聊天请求
     * @return 聊天响应
     * @throws IOException 网络异常
     */
    public ChatResponse chat(ChatRequest request) throws IOException {
        String url = API_BASE_URL + CHAT_COMPLETIONS_ENDPOINT;
        HttpPost httpPost = new HttpPost(url);

        // 设置请求头
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Authorization", "Bearer " + apiKey);

        // 设置请求体
        String requestBody = objectMapper.writeValueAsString(request);
        httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

        logger.info("发送聊天请求到: {}", url);
        logger.debug("请求体: {}", requestBody);

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);

            logger.debug("响应状态码: {}", response.getStatusLine().getStatusCode());
            logger.debug("响应体: {}", responseBody);

            if (response.getStatusLine().getStatusCode() == 200) {
                return objectMapper.readValue(responseBody, ChatResponse.class);
            } else {
                throw new IOException("API请求失败: " + response.getStatusLine().getStatusCode() + " - " + responseBody);
            }
        }
    }

    /**
     * 发送聊天请求（流式）
     * @param request 聊天请求
     * @param onChunk 流式数据回调
     * @throws IOException 网络异常
     */
    public void chatStream(ChatRequest request, StreamCallback onChunk) throws IOException {
        request.setStream(true);
        String url = API_BASE_URL + CHAT_COMPLETIONS_ENDPOINT;
        HttpPost httpPost = new HttpPost(url);

        // 设置请求头
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Authorization", "Bearer " + apiKey);

        // 设置请求体
        String requestBody = objectMapper.writeValueAsString(request);
        httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

        logger.info("发送流式聊天请求到: {}", url);

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                String errorBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                throw new IOException("API请求失败: " + response.getStatusLine().getStatusCode() + " - " + errorBody);
            }

            // 处理流式响应
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) {
                            onChunk.onComplete();
                            break;
                        }

                        try {
                            JsonNode jsonNode = objectMapper.readTree(data);
                            onChunk.onChunk(jsonNode);
                        } catch (Exception e) {
                            logger.warn("解析流式数据失败: {}", data, e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 流式回调接口
     */
    public interface StreamCallback {
        void onChunk(JsonNode chunk);
        void onComplete();
    }

    /**
     * 简单的聊天方法
     * @param message 用户消息
     * @return AI回复
     * @throws IOException 网络异常
     */
    public String simpleChat(String message) throws IOException {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", message));

        ChatRequest request = new ChatRequest(messages);
        ChatResponse response = chat(request);

        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }

        return "抱歉，无法获取回复";
    }

    /**
     * 异步聊天方法
     * @param message 用户消息
     * @return 异步结果
     */
    public CompletableFuture<String> asyncChat(String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return simpleChat(message);
            } catch (IOException e) {
                logger.error("异步聊天失败", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 关闭HTTP客户端
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            logger.error("关闭HTTP客户端失败", e);
        }
    }
}
