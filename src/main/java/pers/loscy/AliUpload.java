package pers.loscy;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;

/**
 * 阿里云OSS上传工具类
 * @author 徐天
 * @create 2025/1/9
 */
public class AliUpload {
    
    private static final Logger logger = LoggerFactory.getLogger(AliUpload.class);
    
    private static final String ENDPOINT = "https://oss-cn-shanghai.aliyuncs.com";
    private static final String ACCESS_KEY_ID = "your_oss_access_key_id_here";
    private static final String ACCESS_KEY_SECRET = "your_oss_access_key_secret_here";
    private static final String BUCKET_NAME = "loscyaivideo";
    
    private OSS ossClient;
    
    public AliUpload() {
        ossClient = new OSSClientBuilder().build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
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
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME, objectName, file);
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            
            String url = "https://" + BUCKET_NAME + ".oss-cn-shanghai.aliyuncs.com/" + objectName;
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
            PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME, objectName, inputStream);
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            
            String url = "https://" + BUCKET_NAME + ".oss-cn-shanghai.aliyuncs.com/" + objectName;
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