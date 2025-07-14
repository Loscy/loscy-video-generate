package pers.loscy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pers.loscy.service.XhsNoteService;
import pers.loscy.model.XhsNoteRequest;
import pers.loscy.model.XhsNoteResponse;
import pers.loscy.model.XhsNoteScript;

import java.util.*;
import java.util.ArrayList;

/**
 * 小红书笔记生成控制器
 * @author 徐天
 * @create 2025/7/10 14:42
 */
@Controller
@RequestMapping("/xhs")
public class XhsController {

    @Autowired
    private XhsNoteService xhsNoteService;

    /**
     * 显示小红书笔记生成页面
     */
    @GetMapping("/generate")
    public String showGeneratePage() {
        return "xhsGenerate";
    }

    /**
     * 生成小红书笔记
     */
    @PostMapping("/generate")
    @ResponseBody
    public Map<String, Object> generateNote(@RequestBody XhsNoteRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            XhsNoteResponse response = xhsNoteService.generateNote(request);
            
            if (response.isSuccess()) {
                result.put("success", true);
                result.put("data", response);
                result.put("taskId", response.getTaskId());
            } else {
                result.put("success", false);
                result.put("message", response.getMessage());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "生成失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 步骤1: 生成小红书笔记脚本
     */
    @PostMapping("/generate-script")
    @ResponseBody
    public Map<String, Object> generateScript(@RequestBody XhsNoteRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            XhsNoteScript script = xhsNoteService.generateScript(request);

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
     * 步骤2: 根据脚本生成图片
     */
    @PostMapping("/generate-images")
    @ResponseBody
    public Map<String, Object> generateImages(@RequestBody XhsNoteScript script) {
        Map<String, Object> result = new HashMap<>();

        try {
            XhsNoteResponse response = xhsNoteService.generateImages(script);

            if (response.isSuccess()) {
                result.put("success", true);
                result.put("data", response);
                result.put("taskId", response.getTaskId());
                result.put("message", "图片生成成功");
            } else {
                result.put("success", false);
                result.put("message", response.getMessage());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "图片生成失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 显示分步生成页面
     */
    @GetMapping("/generate-steps")
    public String showGenerateStepsPage() {
        return "xhsGenerateSteps";
    }

    /**
     * 显示生成结果页面
     */
    @GetMapping("/result/{taskId}")
    public String showResultPage(@PathVariable String taskId, Model model) {
        try {
            XhsNoteResponse noteData = xhsNoteService.getNoteData(taskId);
            model.addAttribute("noteData", noteData);
            return "xhsResult";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /**
     * 下载笔记包（ZIP文件）
     */
    @GetMapping("/download/{taskId}")
    public ResponseEntity<ByteArrayResource> downloadNotePackage(@PathVariable String taskId) {
        try {
            // 生成ZIP文件
            byte[] zipData = xhsNoteService.generateNotePackage(taskId);

            // 创建资源
            ByteArrayResource resource = new ByteArrayResource(zipData);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=xhs-note-" + taskId + ".zip");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(zipData.length)
                    .body(resource);

        } catch (Exception e) {
            // 返回错误响应
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 测试结果页面布局
     */
    @GetMapping("/test-result")
    public String testResultPage(Model model) {
        // 创建测试数据
        XhsNoteResponse testData = new XhsNoteResponse();
        testData.setTaskId("test-task-id");
        testData.setTitle("🔥AI生成小红书笔记测试！超好用的工具分享～");
        testData.setSuccess(true);

        // 设置核心要点
        List<String> keyPoints = Arrays.asList(
            "AI智能分析内容",
            "一键生成标题和要点",
            "自动配图功能强大",
            "支持多种风格选择"
        );
        testData.setKeyPoints(keyPoints);

        // 设置封面图HTML - 使用新的3:4尺寸模板
        String coverHtml = generateTestCoverHtml();
        testData.setCoverImageHtml(coverHtml);

        // 设置内容图HTML - 使用新的动态模板
        List<String> contentImageHtmls = generateTestContentHtmls();
        testData.setContentImageHtmls(contentImageHtmls);

        // 设置Markdown内容
        String markdownContent = generateTestMarkdownContent();
        testData.setMarkdownContent(markdownContent);

        model.addAttribute("noteData", testData);
        return "xhsResult";
    }

    /**
     * 生成测试封面HTML
     */
    private String generateTestCoverHtml() {
        return "<!DOCTYPE html>\n" +
               "<html lang=\"zh-CN\">\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "    <title>🔥AI生成小红书笔记测试！超好用的工具分享～</title>\n" +
               "    <style>\n" +
               "        html, body {\n" +
               "            margin: 0;\n" +
               "            padding: 0;\n" +
               "            box-sizing: border-box;\n" +
               "            font-family: 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;\n" +
               "        }\n" +
               "        body {\n" +
               "            display: flex;\n" +
               "            justify-content: center;\n" +
               "            align-items: center;\n" +
               "            background: #f5f5f5;\n" +
               "            min-height: 100vh;\n" +
               "        }\n" +
               "        .container {\n" +
               "            width: 600px;\n" +
               "            height: 800px;\n" +
               "            display: flex;\n" +
               "            flex-direction: column;\n" +
               "            background: #fff;\n" +
               "            border-radius: 20px;\n" +
               "            overflow: hidden;\n" +
               "            box-shadow: 0 8px 32px rgba(0,0,0,0.12);\n" +
               "        }\n" +
               "        .title-section {\n" +
               "            height: 280px;\n" +
               "            display: flex;\n" +
               "            flex-direction: column;\n" +
               "            justify-content: center;\n" +
               "            align-items: center;\n" +
               "            padding: 30px 25px;\n" +
               "            background: linear-gradient(135deg, #ff6b9d 0%, #c44569 100%);\n" +
               "            color: white;\n" +
               "            position: relative;\n" +
               "            overflow: hidden;\n" +
               "        }\n" +
               "        .main-title {\n" +
               "            font-size: 2.2rem;\n" +
               "            font-weight: bold;\n" +
               "            margin-bottom: 15px;\n" +
               "            text-align: center;\n" +
               "            text-shadow: 2px 2px 4px rgba(0,0,0,0.3);\n" +
               "            z-index: 2;\n" +
               "            position: relative;\n" +
               "            line-height: 1.2;\n" +
               "        }\n" +
               "        .subtitle {\n" +
               "            font-size: 1.1rem;\n" +
               "            font-weight: 500;\n" +
               "            margin-bottom: 12px;\n" +
               "            text-align: center;\n" +
               "            opacity: 0.95;\n" +
               "            z-index: 2;\n" +
               "            position: relative;\n" +
               "            background: rgba(255,255,255,0.2);\n" +
               "            padding: 8px 16px;\n" +
               "            border-radius: 20px;\n" +
               "            backdrop-filter: blur(10px);\n" +
               "        }\n" +
               "        .desc {\n" +
               "            font-size: 0.95rem;\n" +
               "            color: rgba(255,255,255,0.9);\n" +
               "            text-align: center;\n" +
               "            max-width: 90%;\n" +
               "            line-height: 1.6;\n" +
               "            z-index: 2;\n" +
               "            position: relative;\n" +
               "        }\n" +
               "        .image-section {\n" +
               "            height: 520px;\n" +
               "            display: flex;\n" +
               "            justify-content: center;\n" +
               "            align-items: center;\n" +
               "            background: linear-gradient(45deg, #ffd6ea, #ffe6f2);\n" +
               "            position: relative;\n" +
               "            padding: 20px;\n" +
               "        }\n" +
               "        .cover-img {\n" +
               "            width: 480px;\n" +
               "            height: 360px;\n" +
               "            border-radius: 15px;\n" +
               "            box-shadow: 0 8px 32px rgba(0,0,0,0.15);\n" +
               "            object-fit: cover;\n" +
               "            transition: transform 0.3s ease;\n" +
               "        }\n" +
               "        .cover-img:hover {\n" +
               "            transform: scale(1.02);\n" +
               "        }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "<div class=\"container\">\n" +
               "    <div class=\"title-section\">\n" +
               "        <div class=\"main-title\">🔥AI生成小红书笔记测试！</div>\n" +
               "        <div class=\"subtitle\">AI智能分析内容</div>\n" +
               "        <div class=\"desc\">使用DeepSeek AI分析内容，生成吸引人的标题和核心要点，让你的笔记更有吸引力！</div>\n" +
               "    </div>\n" +
               "    <div class=\"image-section\">\n" +
               "        <img class=\"cover-img\" src=\"/images/cover_7beb2cee-3809-48ed-9281-f262920bcd99_1752202657739.jpg\" alt=\"AI生成封面\" />\n" +
               "    </div>\n" +
               "</div>\n" +
               "</body>\n" +
               "</html>";
    }

    /**
     * 生成测试内容HTML列表
     */
    private List<String> generateTestContentHtmls() {
        List<String> htmls = new ArrayList<>();

        String[] titles = {"🔥AI生成小红书笔记测试！", "💡智能分析内容", "🎨自动配图功能"};
        String[] descriptions = {
            "AI智能分析内容 - 让我们一起来看看吧！",
            "一键生成标题和要点 - 让我们一起来看看吧！",
            "自动配图功能强大 - 让我们一起来看看吧！"
        };

        for (int i = 0; i < 3; i++) {
            String html = generateSingleContentHtml(titles[i], descriptions[i], i + 1);
            htmls.add(html);
        }

        return htmls;
    }

    /**
     * 生成单个内容HTML
     */
    private String generateSingleContentHtml(String title, String description, int index) {
        // 动态生成标题字符 - 修复字符编码问题，正确处理emoji
        StringBuilder titleChars = new StringBuilder();
        int length = title.length();
        for (int i = 0; i < length; ) {
            int codePoint = title.codePointAt(i);
            String character = new String(Character.toChars(codePoint));

            if (!Character.isWhitespace(codePoint)) {
                titleChars.append("<div class=\"title-char\">").append(character).append("</div>");
            }

            i += Character.charCount(codePoint);
        }

        return "<!DOCTYPE html>\n" +
               "<html lang=\"zh-CN\">\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <title>" + title + "</title>\n" +
               "    <style>\n" +
               "        body {\n" +
               "            font-family: 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;\n" +
               "            background: #fff;\n" +
               "            color: #222;\n" +
               "            margin: 0;\n" +
               "            padding: 0;\n" +
               "        }\n" +
               "        .container {\n" +
               "            width: 600px;\n" +
               "            height: 800px;\n" +
               "            margin: 0 auto;\n" +
               "            background: #fff;\n" +
               "            padding: 40px 32px;\n" +
               "            border-radius: 20px;\n" +
               "            box-shadow: 0 4px 20px rgba(0,0,0,0.08);\n" +
               "            display: flex;\n" +
               "            flex-direction: column;\n" +
               "            justify-content: space-between;\n" +
               "        }\n" +
               "        .title {\n" +
               "            display: flex;\n" +
               "            justify-content: center;\n" +
               "            flex-wrap: wrap;\n" +
               "            margin-bottom: 30px;\n" +
               "            gap: 4px;\n" +
               "        }\n" +
               "        .title-char {\n" +
               "            border: 2px dashed #ff6b9d;\n" +
               "            font-size: 2rem;\n" +
               "            font-weight: bold;\n" +
               "            width: 50px;\n" +
               "            height: 50px;\n" +
               "            display: flex;\n" +
               "            align-items: center;\n" +
               "            justify-content: center;\n" +
               "            background: linear-gradient(135deg, #ffe6f2, #ffd6ea);\n" +
               "            border-radius: 8px;\n" +
               "            color: #c44569;\n" +
               "            letter-spacing: 0;\n" +
               "        }\n" +
               "        .desc {\n" +
               "            font-size: 1.1rem;\n" +
               "            font-weight: 600;\n" +
               "            margin-bottom: 25px;\n" +
               "            display: flex;\n" +
               "            align-items: flex-start;\n" +
               "            background: linear-gradient(135deg, #f8f9ff, #fff0f5);\n" +
               "            padding: 15px;\n" +
               "            border-radius: 12px;\n" +
               "            border-left: 4px solid #ff6b9d;\n" +
               "        }\n" +
               "        .bullet {\n" +
               "            color: #000;\n" +
               "            font-size: 1.2rem;\n" +
               "            margin-right: 8px;\n" +
               "        }\n" +
               "        .sections {\n" +
               "            flex: 1;\n" +
               "            margin-bottom: 20px;\n" +
               "        }\n" +
               "        .section {\n" +
               "            display: flex;\n" +
               "            align-items: flex-start;\n" +
               "            margin-bottom: 20px;\n" +
               "            background: rgba(255, 255, 255, 0.8);\n" +
               "            padding: 15px;\n" +
               "            border-radius: 12px;\n" +
               "            border: 1px solid rgba(255, 107, 157, 0.1);\n" +
               "        }\n" +
               "        .num {\n" +
               "            width: 36px;\n" +
               "            height: 36px;\n" +
               "            background: linear-gradient(135deg, #ff6b9d, #c44569);\n" +
               "            color: white;\n" +
               "            border-radius: 50%;\n" +
               "            display: flex;\n" +
               "            align-items: center;\n" +
               "            justify-content: center;\n" +
               "            font-weight: bold;\n" +
               "            font-size: 1.1rem;\n" +
               "            margin-right: 15px;\n" +
               "            flex-shrink: 0;\n" +
               "            box-shadow: 0 2px 8px rgba(255, 107, 157, 0.3);\n" +
               "        }\n" +
               "        .section-content {\n" +
               "            flex: 1;\n" +
               "            line-height: 1.6;\n" +
               "            font-size: 1rem;\n" +
               "        }\n" +
               "        .section-title {\n" +
               "            background: linear-gradient(135deg, #ff6b9d, #c44569);\n" +
               "            color: white;\n" +
               "            font-weight: bold;\n" +
               "            padding: 4px 10px;\n" +
               "            border-radius: 6px;\n" +
               "            margin-right: 8px;\n" +
               "            font-size: 0.9rem;\n" +
               "            box-shadow: 0 2px 4px rgba(255, 107, 157, 0.3);\n" +
               "        }\n" +
               "        .ending {\n" +
               "            font-size: 1.1rem;\n" +
               "            font-weight: 600;\n" +
               "            display: flex;\n" +
               "            align-items: center;\n" +
               "            background: linear-gradient(135deg, #f8f9ff, #fff0f5);\n" +
               "            padding: 15px;\n" +
               "            border-radius: 12px;\n" +
               "            border-left: 4px solid #ff6b9d;\n" +
               "        }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "<div class=\"container\">\n" +
               "    <div class=\"title\">\n" +
               "        " + titleChars.toString() + "\n" +
               "    </div>\n" +
               "    <div class=\"desc\"><span class=\"bullet\">●</span>" + description + "</div>\n" +
               "    <div class=\"sections\">\n" +
               "        <div class=\"section\">\n" +
               "            <div class=\"num\">1</div>\n" +
               "            <div class=\"section-content\">\n" +
               "                <span class=\"section-title\">AI智能分析</span>使用DeepSeek AI分析内容，生成吸引人的标题和核心要点\n" +
               "            </div>\n" +
               "        </div>\n" +
               "        <div class=\"section\">\n" +
               "            <div class=\"num\">2</div>\n" +
               "            <div class=\"section-content\">\n" +
               "                <span class=\"section-title\">一键生成</span>输入内容后一键生成标题、要点和精美图片\n" +
               "            </div>\n" +
               "        </div>\n" +
               "        <div class=\"section\">\n" +
               "            <div class=\"num\">3</div>\n" +
               "            <div class=\"section-content\">\n" +
               "                <span class=\"section-title\">自动配图</span>自动生成小红书风格的封面图和内容图片\n" +
               "            </div>\n" +
               "        </div>\n" +
               "        <div class=\"section\">\n" +
               "            <div class=\"num\">4</div>\n" +
               "            <div class=\"section-content\">\n" +
               "                <span class=\"section-title\">下载分享</span>生成的HTML可直接下载为图片，方便分享\n" +
               "            </div>\n" +
               "        </div>\n" +
               "    </div>\n" +
               "    <div class=\"ending\"><span class=\"bullet\">●</span>记得点赞收藏哦！</div>\n" +
               "</div>\n" +
               "</body>\n" +
               "</html>";
    }

    /**
     * 生成测试Markdown内容
     */
    private String generateTestMarkdownContent() {
        return "# 🔥AI生成小红书笔记测试！超好用的工具分享～\n\n" +
               "## 📝 核心要点\n\n" +
               "- **AI智能分析内容** - 使用DeepSeek AI分析内容，生成吸引人的标题和核心要点\n" +
               "- **一键生成标题和要点** - 输入内容后一键生成标题、要点和精美图片\n" +
               "- **自动配图功能强大** - 自动生成小红书风格的封面图和内容图片\n" +
               "- **支持多种风格选择** - 生成的HTML可直接下载为图片，方便分享\n\n" +
               "## 🎯 详细介绍\n\n" +
               "### 1. AI智能分析\n" +
               "使用DeepSeek AI分析内容，生成吸引人的标题和核心要点，让你的笔记更有吸引力！\n\n" +
               "### 2. 一键生成\n" +
               "输入内容后一键生成标题、要点和精美图片，操作简单快捷。\n\n" +
               "### 3. 自动配图\n" +
               "自动生成小红书风格的封面图和内容图片，视觉效果出众。\n\n" +
               "### 4. 下载分享\n" +
               "生成的HTML可直接下载为图片，方便在各个平台分享。\n\n" +
               "## 💡 使用技巧\n\n" +
               "1. **内容要有吸引力** - 输入的内容要有趣、有价值\n" +
               "2. **标题要抓眼球** - 使用emoji和关键词\n" +
               "3. **要点要清晰** - 每个要点都要简洁明了\n" +
               "4. **图片要精美** - 选择合适的风格和配色\n\n" +
               "---\n\n" +
               "*记得点赞收藏哦！* ❤️\n\n" +
               "> 本内容由AI生成，仅供参考学习使用。";
    }
}
