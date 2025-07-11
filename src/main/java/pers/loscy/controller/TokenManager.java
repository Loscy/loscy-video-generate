package pers.loscy.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Token管理器
 * 负责token的生成、存储、验证和清理
 * @author 徐天
 * @create 2025/1/9
 */
public class TokenManager {
    
    // 存储有效的token，key为token，value为创建时间
    private static final Map<String, Long> validTokens = new ConcurrentHashMap<>();
    
    // 定时清理过期token的线程池
    private static final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // token有效期（毫秒）- 24小时
    private static final long TOKEN_EXPIRY_TIME = 24 * 60 * 60 * 1000;
    
    static {
        // 启动定时清理任务，每小时清理一次过期token
        cleanupExecutor.scheduleAtFixedRate(TokenManager::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * 添加token
     * @param token token字符串
     */
    public static void addToken(String token) {
        validTokens.put(token, System.currentTimeMillis());
    }
    
    /**
     * 验证token是否有效
     * @param token token字符串
     * @return 是否有效
     */
    public static boolean isValidToken(String token) {
        Long createTime = validTokens.get(token);
        if (createTime == null) {
            return false;
        }
        
        // 检查是否过期
        long currentTime = System.currentTimeMillis();
        if (currentTime - createTime > TOKEN_EXPIRY_TIME) {
            // token已过期，移除
            validTokens.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * 移除token
     * @param token token字符串
     */
    public static void removeToken(String token) {
        validTokens.remove(token);
    }
    
    /**
     * 清理过期的token
     */
    private static void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        validTokens.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > TOKEN_EXPIRY_TIME
        );
    }
    
    /**
     * 获取当前有效token数量
     * @return token数量
     */
    public static int getValidTokenCount() {
        return validTokens.size();
    }
    
    /**
     * 关闭清理线程池
     */
    public static void shutdown() {
        cleanupExecutor.shutdown();
    }
} 