package pers.loscy.deepseek;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * DeepSeek配置类
 * @author 徐天
 * @create 2025/1/9
 */
public class DeepSeekConfig {
    
    private static final String CONFIG_FILE = "deepseek-config.properties";
    private static String apiKey;
    
    static {
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        try (InputStream input = DeepSeekConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("找不到配置文件: " + CONFIG_FILE);
            }
            
            Properties properties = new Properties();
            properties.load(input);
            
            apiKey = properties.getProperty("deepseek.api.key");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new RuntimeException("配置文件中缺少 deepseek.api.key");
            }
            
        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败", e);
        }
    }
    
    /**
     * 获取API密钥
     */
    public static String getApiKey() {
        return apiKey;
    }
} 