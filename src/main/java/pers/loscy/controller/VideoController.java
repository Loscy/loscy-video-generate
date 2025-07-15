package pers.loscy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pers.loscy.core.CoreService;
import pers.loscy.core.CoreService.VideoGenerationResult;
import pers.loscy.core.CoreService.VideoScript;
import pers.loscy.AliUpload;

import java.util.HashMap;
import java.util.Map;

/**
 * 视频生成控制器
 * @author 徐天
 * @create 2025/1/9
 */
@Controller
@RequestMapping("/video")
public class VideoController {

    @Autowired
    private CoreService coreService;

    @Autowired
    private AliUpload aliUpload;

    /**
     * 首页
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 验证访问密钥API
     */
    @PostMapping("/verify-access")
    @ResponseBody
    public Map<String, Object> verifyAccess(@RequestParam String accessKey) {
        Map<String, Object> result = new HashMap<>();
        
        // 写死的验证密钥
        final String VALID_ACCESS_KEY = "loscy";
        
        if (VALID_ACCESS_KEY.equals(accessKey)) {
            // 生成token
            String token = generateToken();
            result.put("success", true);
            result.put("message", "验证成功");
            result.put("token", token);
        } else {
            result.put("success", false);
            result.put("message", "访问密钥错误");
        }
        
        return result;
    }
    
    /**
     * 生成访问token
     */
    private String generateToken() {
        // 生成一个基于时间戳和随机数的token
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf(new java.util.Random().nextInt(1000000));
        String token = timestamp + "_" + random + "_" + "loscy";
        
        // 将token存储到内存中（实际项目中应该使用Redis等缓存）
        TokenManager.addToken(token);
        
        return token;
    }
    
    /**
     * 验证token
     */
    private boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        return TokenManager.isValidToken(token);
    }
    
    /**
     * 从Authorization头中提取token
     */
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return null;
        }
        
        // 支持Bearer token格式
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 也支持直接传递token
        return authHeader;
    }

    /**
     * 生成视频页面
     */
    @GetMapping("/generate")
    public String generatePage() {
        return "generate";
    }

    /**
     * 步骤1: 生成脚本API
     */
    @PostMapping("/generate-script")
    @ResponseBody
    public Map<String, Object> generateScript(@RequestParam String inputText, 
                                             @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> result = new HashMap<>();
        
        // 验证token
        String token = extractTokenFromHeader(authHeader);
        if (!validateToken(token)) {
            result.put("success", false);
            result.put("message", "未授权访问，请重新验证");
            return result;
        }
        
        try {
            VideoScript script = coreService.generateVideoScript(inputText);
            
            result.put("success", true);
            result.put("script", script);
            result.put("message", "脚本生成成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "脚本生成失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 步骤2: 生成图片API
     */
    @PostMapping("/generate-images")
    @ResponseBody
    public Map<String, Object> generateImages(@RequestBody VideoScript script,
                                             @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> result = new HashMap<>();
        
        // 验证token
        String token = extractTokenFromHeader(authHeader);
        if (!validateToken(token)) {
            result.put("success", false);
            result.put("message", "未授权访问，请重新验证");
            return result;
        }
        
        try {
            java.util.List<String> imageUrls = coreService.generateImages(script);
            
            result.put("success", true);
            result.put("imageUrls", imageUrls);
            result.put("message", "图片生成成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "图片生成失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 步骤3: 生成视频API
     */
    @PostMapping("/generate-video")
    @ResponseBody
    public Map<String, Object> generateVideo(@RequestBody VideoScript script,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> result = new HashMap<>();
        
        // 验证token
        String token = extractTokenFromHeader(authHeader);
        if (!validateToken(token)) {
            result.put("success", false);
            result.put("message", "未授权访问，请重新验证");
            return result;
        }
        
        try {
            String jobId = coreService.generateVideo(script);
            
            result.put("success", true);
            result.put("jobId", jobId);
            result.put("message", "视频生成任务已提交");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "视频生成失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 查询视频状态API
     */
    @GetMapping("/status/{jobId}")
    @ResponseBody
    public Map<String, Object> getVideoStatus(@PathVariable String jobId,
                                             @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> result = new HashMap<>();
        
        // 验证token
        String token = extractTokenFromHeader(authHeader);
        if (!validateToken(token)) {
            result.put("success", false);
            result.put("message", "未授权访问，请重新验证");
            return result;
        }
        
        try {
            VideoGenerationResult videoResult = coreService.queryVideoStatus(jobId);
            
            result.put("success", true);
            result.put("jobId", videoResult.getJobId());
            result.put("status", videoResult.getStatus());
            result.put("videoUrl", videoResult.getVideoUrl());
            
            if (videoResult.getErrorMessage() != null) {
                result.put("errorMessage", videoResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 等待视频完成API
     */
    @PostMapping("/wait/{jobId}")
    @ResponseBody
    public Map<String, Object> waitForVideoCompletion(@PathVariable String jobId, 
                                                     @RequestParam(defaultValue = "300") int timeout) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            VideoGenerationResult videoResult = coreService.waitForVideoCompletion(jobId, timeout);
            
            result.put("success", true);
            result.put("jobId", videoResult.getJobId());
            result.put("status", videoResult.getStatus());
            result.put("videoUrl", videoResult.getVideoUrl());
            
            if (videoResult.getErrorMessage() != null) {
                result.put("errorMessage", videoResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "等待失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 兼容旧版本的生成视频API（保留）
     */
    @PostMapping("/generate")
    @ResponseBody
    public Map<String, Object> generateVideoLegacy(@RequestParam String inputText) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            VideoGenerationResult videoResult = coreService.generateVideoFromText(inputText);
            
            result.put("success", true);
            result.put("jobId", videoResult.getJobId());
            result.put("status", videoResult.getStatus());
            result.put("message", "视频生成任务已提交");
            
            if (videoResult.getScript() != null) {
                result.put("title", videoResult.getScript().getTitle());
                result.put("scenes", videoResult.getScript().getScenes());
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "生成失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 图片上传接口
     */
    @PostMapping("/upload-image")
    @ResponseBody
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "sceneIndex", required = false) Integer sceneIndex,
                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> result = new HashMap<>();

        // 验证token
        String token = extractTokenFromHeader(authHeader);
        if (!validateToken(token)) {
            result.put("success", false);
            result.put("message", "未授权访问，请重新验证");
            return result;
        }

        try {
            // 验证文件
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "上传文件为空");
                return result;
            }

            // 验证文件大小 (限制为10MB)
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (file.getSize() > maxSize) {
                result.put("success", false);
                result.put("message", "文件大小超过限制，最大支持10MB");
                return result;
            }

            // 构建对象名称
            String objectName = null;
            if (sceneIndex != null) {
                objectName = "scene_" + sceneIndex + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            }

            // 临时保存文件到本地
            String tempDir = System.getProperty("java.io.tmpdir");
            String tempFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String tempFilePath = tempDir + "/" + tempFileName;
            file.transferTo(new java.io.File(tempFilePath));

            // 上传文件
            String imageUrl = aliUpload.uploadLocalFile(tempFilePath, objectName);

            // 删除临时文件
            new java.io.File(tempFilePath).delete();

            result.put("success", true);
            result.put("imageUrl", imageUrl);
            result.put("message", "图片上传成功");
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "图片上传失败: " + e.getMessage());
        }

        return result;
    }
}