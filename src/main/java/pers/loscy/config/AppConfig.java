package pers.loscy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pers.loscy.core.CoreService;
import pers.loscy.AliUpload;

/**
 * 应用配置类
 * @author 徐天
 * @create 2025/1/9
 */
@Configuration
public class AppConfig {

    /**
     * 配置CoreService Bean
     */
    @Bean
    public CoreService coreService() {
        return new CoreService();
    }

    /**
     * 配置AliUpload Bean
     */
    @Bean
    public AliUpload aliUpload() {
        return new AliUpload();
    }
}