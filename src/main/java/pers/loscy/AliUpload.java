package pers.loscy;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 阿里云OSS上传工具类
 * @author 徐天
 * @create 2025/1/9
 */
public class AliUpload {
    
    private static final Logger logger = LoggerFactory.getLogger(AliUpload.class);
    
    private static final String CONFIG_FILE = "oss-config.properties";
    private static final String DEFAULT_ENDPOINT = "https://oss-cn-shanghai.aliyuncs.com";
    
    private OSS ossClient;
    private String bucketName;
    
    public AliUpload() {
        Properties config = loadConfig();
        String endpoint = getConfigValue(config, "oss.endpoint", "OSS_ENDPOINT", DEFAULT_ENDPOINT);
        String accessKeyId = getRequiredConfigValue(config, "oss.accessKeyId", "OSS_ACCESS_KEY_ID");
        String accessKeySecret = getRequiredConfigValue(config, "oss.accessKeySecret", "OSS_ACCESS_KEY_SECRET");
        bucketName = getRequiredConfigValue(config, "oss.bucketName", "OSS_BUCKET_NAME");
        ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    private Properties loadConfig() {
        Properties config = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                config.load(input);
            }
            return config;
        } catch (IOException e) {
            throw new RuntimeException("加载OSS配置文件失败", e);
        }
    }

    private String getRequiredConfigValue(Properties config, String propertyName, String envName) {
        String value = getConfigValue(config, propertyName, envName, null);
        if (value == null || value.trim().isEmpty() || value.startsWith("your_")) {
            throw new RuntimeException("缺少配置: " + propertyName + " 或环境变量 " + envName);
        }
        return value;
    }

    private String getConfigValue(Properties config, String propertyName, String envName, String defaultValue) {
        String value = System.getenv(envName);
        if (value == null || value.trim().isEmpty()) {
            value = config.getProperty(propertyName);
        }
        if (value == null || value.trim().isEmpty()) {
            value = defaultValue;
        }
        return value == null ? null : value.trim();
    }
    
    /**
     * 上传本地文件到OSS
     * @param localFilePath 本地文件路径
     * @param objectName OSS对象名
     * @return OSS URL
     */
    public String uploadLocalFile(String localFilePath, String objectName) {
        try {
            File file = new File(localFilePath);
            if (!file.exists()) {
                throw new RuntimeException("文件不存在: " + localFilePath);
            }
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, file);
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            
            String url = "https://" + bucketName + ".oss-cn-shanghai.aliyuncs.com/" + objectName;
            logger.info("文件上传成功: {}", url);
            return url;
            
        } catch (Exception e) {
            logger.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }
    
    /**
     * 上传资源文件到OSS
     * @param resourcePath 资源文件路径
     * @return OSS URL
     */
    public String uploadResourceFile(String resourcePath) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new RuntimeException("资源文件不存在: " + resourcePath);
            }
            
            String objectName = "resources/" + System.currentTimeMillis() + "_" + resourcePath;
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            
            String url = "https://" + bucketName + ".oss-cn-shanghai.aliyuncs.com/" + objectName;
            logger.info("资源文件上传成功: {}", url);
            return url;
            
        } catch (Exception e) {
            logger.error("资源文件上传失败", e);
            throw new RuntimeException("资源文件上传失败", e);
        }
    }
    
    /**
     * 关闭OSS客户端
     */
    public void close() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
