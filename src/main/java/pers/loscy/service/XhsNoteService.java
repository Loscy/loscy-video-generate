package pers.loscy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.Context;
import pers.loscy.deepseek.DeepSeekAi;
import pers.loscy.model.XhsNoteRequest;
import pers.loscy.model.XhsNoteResponse;
import pers.loscy.deepseek.DeepSeekConfig;
import pers.loscy.image.ImageGenerate;
import pers.loscy.AliUpload;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 小红书笔记生成服务
 * @author 徐天
 * @create 2025/7/10 14:42
 */
@Service
public class XhsNoteService {

    private static final Logger log = LoggerFactory.getLogger(XhsNoteService.class);

    @Autowired
    private TemplateEngine templateEngine;

    private final DeepSeekAi deepSeekAi;
    private final ObjectMapper objectMapper;
    private final ImageGenerate imageGenerate;
    private final AliUpload aliUpload;
    
    // 存储生成的任务数据
    private final Map<String, XhsNoteResponse> taskDataMap = new ConcurrentHashMap<>();

    public XhsNoteService() throws Exception {
        // 初始化DeepSeek配置
        this.deepSeekAi = new DeepSeekAi(DeepSeekConfig.getApiKey());
        this.objectMapper = new ObjectMapper();
        // 初始化文生图服务
        this.imageGenerate = new ImageGenerate();
        // 初始化OSS上传服务
        this.aliUpload = new AliUpload();
    }

    /**
     * 生成小红书笔记
     */
    public XhsNoteResponse generateNote(XhsNoteRequest request) throws Exception {
        String taskId = UUID.randomUUID().toString();
        XhsNoteResponse response = new XhsNoteResponse();
        response.setTaskId(taskId);
        response.setSuccess(true);

        try {
            // 1. 使用DeepSeek总结用户输入内容
            String summary = generateSummary(request.getContent());
            response.setSummary(summary);

            // 2. 解析总结内容，提取标题和核心点
            NoteContent noteContent = parseSummary(summary);
            response.setTitle(noteContent.getTitle());
            response.setKeyPoints(noteContent.getKeyPoints());

            // 3. 生成封面图片HTML
            // 3.1 生成图片
            String mainImageUrl = generateMainImage(noteContent.getImageDescription());
            String coverImageHtml = generateCoverImageHtml(noteContent, mainImageUrl);
            response.setCoverImageHtml(coverImageHtml);

            // 4. 生成内容图片HTML（固定4张，每张对应一个核心点）
            List<String> contentImageHtmls = generateContentImageHtmls(noteContent, 4);
            response.setContentImageHtmls(contentImageHtmls);

            // 5. 生成Markdown内容
            String markdownContent = generateMarkdownContent(noteContent);
            response.setMarkdownContent(markdownContent);

            // 6. 保存任务数据
            taskDataMap.put(taskId, response);

            log.info("小红书笔记生成完成，任务ID: {}, 标题: {}", taskId, noteContent.getTitle());

            return response;

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("生成失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 使用文生图生成主图片
     * @param imageDescription 图片描述
     * @return 生成的图片URL
     */
    private String generateMainImage(String imageDescription) {
        try {
            // 调用文生图API生成图片 - 3:4尺寸
            ImageGenerate.ImageGenerateResult imageResult = imageGenerate.generateImage(
                imageDescription,
                "wanx2.1-t2i-turbo",  // 模型名称
                800,   // 宽度
                580,  // 高度
                20,    // 步数
                null   // 随机种子
            );
            // 下载图片并上传到OSS
            return downloadAndUploadToOSS(imageResult.getImageUrl(), imageResult.getTaskId());

        } catch (Exception e) {
            // 如果生成失败，使用默认图片
            try {
                return aliUpload.uploadResourceFile("img/error-base.png");
            } catch (Exception ex) {
                throw new RuntimeException("图片生成失败且无法使用默认图片", e);
            }
        }
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
        String objectName = "xhs-generated/" + timestamp + "_" + taskId + ".jpg";
        
        // 下载图片并上传到OSS
        try (InputStream inputStream = new java.net.URL(imageUrl).openStream()) {
            // 创建临时文件
            java.io.File tempFile = java.io.File.createTempFile("xhs_image_", ".jpg");
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
     * 使用DeepSeek生成内容总结
     */
    private String generateSummary(String content) throws Exception {
        String prompt = String.format(
            "请对以下内容进行总结，生成小红书风格的笔记内容。要求：\n" +
            "1. 生成一个吸引人的标题\n" +
            "2. 提取固定4个核心要点\n" +
            "3. 每个要点要有详细的描述，描述字数必须在300-800字之间\n" +
            "4. 生成一个适合文生图的图片描述\n" +
            "5. 内容标题限制8个字以内\n" +
            "6. 语言要生动有趣，符合小红书风格\n" +
            "7. 描述内容要详实具体，包含实用信息和具体建议\n\n" +
            "内容：%s\n\n" +
            "请严格按照以下JSON格式输出，不要包含任何其他文字：\n" +
            "{\n" +
            "  \"title\": \"标题内容\",\n" +
            "  \"keyPoints\": [\"核心点1\", \"核心点2\", \"核心点3\", \"核心点4\"],\n" +
            "  \"descriptions\": [\"描述1(300-800字)\", \"描述2(300-800字)\", \"描述3(300-800字)\", \"描述4(300-800字)\"],\n" +
            "  \"imageDescription\": \"图片描述内容\"\n" +
            "}",
            content
        );

        return deepSeekAi.simpleChat(prompt);
    }

    /**
     * 解析总结内容（JSON格式）
     */
    private NoteContent parseSummary(String summary) throws Exception {
        try {
            // 尝试直接解析JSON
            JsonNode jsonNode = objectMapper.readTree(summary);
            
            NoteContent noteContent = new NoteContent();
            noteContent.setTitle(jsonNode.get("title").asText());
            noteContent.setImageDescription(jsonNode.get("imageDescription").asText());
            
            // 解析核心点数组
            List<String> keyPoints = new ArrayList<>();
            JsonNode keyPointsNode = jsonNode.get("keyPoints");
            if (keyPointsNode != null && keyPointsNode.isArray()) {
                for (JsonNode point : keyPointsNode) {
                    keyPoints.add(point.asText());
                }
            }
            noteContent.setKeyPoints(keyPoints);
            
            // 解析描述数组
            List<String> descriptions = new ArrayList<>();
            JsonNode descriptionsNode = jsonNode.get("descriptions");
            if (descriptionsNode != null && descriptionsNode.isArray()) {
                for (JsonNode desc : descriptionsNode) {
                    descriptions.add(desc.asText());
                }
            }
            noteContent.setDescriptions(descriptions);
            
            return noteContent;
            
        } catch (Exception e) {
            // 如果JSON解析失败，尝试从文本中提取JSON部分
            String jsonContent = extractJsonFromText(summary);
            if (jsonContent != null) {
                return parseSummary(jsonContent);
            }
            
            // 如果仍然失败，回退到原来的文本解析方式
            return parseSummaryFromText(summary);
        }
    }

    /**
     * 从文本中提取JSON内容
     */
    private String extractJsonFromText(String text) {
        int startIndex = text.indexOf('{');
        int endIndex = text.lastIndexOf('}');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return text.substring(startIndex, endIndex + 1);
        }
        
        return null;
    }

    /**
     * 从文本格式解析总结内容（备用方案）
     */
    private NoteContent parseSummaryFromText(String summary) {
        NoteContent noteContent = new NoteContent();
        List<String> keyPoints = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();

        String[] lines = summary.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("标题：")) {
                noteContent.setTitle(line.substring(3).trim());
            } else if (line.startsWith("核心点")) {
                keyPoints.add(line.substring(line.indexOf("：") + 1).trim());
            } else if (line.startsWith("描述：")) {
                descriptions.add(line.substring(3).trim());
            } else if (line.startsWith("图片描述：")) {
                noteContent.setImageDescription(line.substring(5).trim());
            }
        }

        noteContent.setKeyPoints(keyPoints);
        noteContent.setDescriptions(descriptions);
        return noteContent;
    }

    /**
     * 生成封面图片HTML
     */
    private String generateCoverImageHtml(NoteContent noteContent, String imageUrl) throws Exception {
        // 读取HTML模板文件
        String templateContent = readTemplateFile("templates/xhsCoverTemplate.html");
        
        // 准备替换变量
        String mainTitle = noteContent.getTitle();
        String subTitle = noteContent.getKeyPoints().isEmpty() ? "" : noteContent.getKeyPoints().get(0);
        String desc = noteContent.getDescriptions().isEmpty() ? "" : noteContent.getDescriptions().get(0);
        String points = String.join("、", noteContent.getKeyPoints());
        
        // 替换模板变量
        String htmlContent = templateContent
            .replace("${mainTitle}", mainTitle)
            .replace("${subTitle}", subTitle)
            .replace("${points}", points)
            .replace("${url}", imageUrl)
            .replace("${desc}", desc);
        
        return htmlContent;
    }

    /**
     * 生成内容图片HTML
     */
    private List<String> generateContentImageHtmls(NoteContent noteContent, Integer imageCount) throws Exception {
        List<String> htmlContents = new ArrayList<>();
        int count = imageCount != null ? imageCount : 4;

        // 读取HTML模板文件
        String templateContent = readTemplateFile("templates/xhsInfoTemplate.html");

        // 确保生成4个HTML文件，每个对应一个核心点
        for (int i = 0; i < 4; i++) {
            String keyPoint = i < noteContent.getKeyPoints().size() ?
                noteContent.getKeyPoints().get(i) : "核心要点" + (i + 1);
            String description = i < noteContent.getDescriptions().size() ?
                noteContent.getDescriptions().get(i) : "详细描述内容";

            // 准备替换变量
            String infoTitle = noteContent.getTitle();

            // 动态生成标题字符
            String titleCharsHtml = generateTitleCharsHtml(infoTitle);

            // 生成当前核心点的描述
            String keyPointDesc = generateSingleKeyPointDesc(keyPoint, i + 1);

            // 生成单个核心点的详细内容
            String singlePointContent = generateSinglePointContent(keyPoint, description, i + 1);

            // 生成结尾文本
            String endingText = generateEndingText(keyPoint);

            // 替换模板变量
            String htmlContent = templateContent
                .replace("${infoTitle}", infoTitle)
                .replace("${titleChars}", titleCharsHtml)
                .replace("${keyPointDesc}", keyPointDesc)
                .replace("${dynamicSections}", singlePointContent)
                .replace("${endingText}", endingText);

            htmlContents.add(htmlContent);
        }

        return htmlContents;
    }

    /**
     * 动态生成标题字符HTML
     */
    private String generateTitleCharsHtml(String title) {
        if (title == null || title.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();

        // 使用codePoint来正确处理emoji和Unicode字符
        int length = title.length();
        for (int i = 0; i < length; ) {
            int codePoint = title.codePointAt(i);
            String character = new String(Character.toChars(codePoint));

            // 跳过空格，但保留所有其他字符包括emoji
            if (!Character.isWhitespace(codePoint)) {
                html.append("<div class=\"title-char\">").append(character).append("</div>");
            }

            i += Character.charCount(codePoint);
        }

        return html.toString();
    }

    /**
     * 生成要点描述
     */
    private String generateKeyPointDesc(List<String> keyPoints, int currentIndex) {
        if (keyPoints == null || keyPoints.isEmpty()) {
            return "精彩内容分享";
        }

        if (currentIndex < keyPoints.size()) {
            return keyPoints.get(currentIndex) + " - 让我们一起来看看吧！";
        }

        return "更多精彩内容等你发现！";
    }

    /**
     * 生成单个核心点的描述
     */
    private String generateSingleKeyPointDesc(String keyPoint, int pointNumber) {
        return "核心要点" + pointNumber + "：" + keyPoint + " - 深度解析与实用指南";
    }

    /**
     * 生成单个核心点的详细内容
     */
    private String generateSinglePointContent(String keyPoint, String description, int pointNumber) {
        StringBuilder html = new StringBuilder();

        html.append("<div class=\"single-point-container\">")
            .append("<div class=\"point-header\">")
            .append("<div class=\"point-number\">").append(pointNumber).append("</div>")
            .append("<div class=\"point-title\">").append(keyPoint).append("</div>")
            .append("</div>")
            .append("<div class=\"point-description\">")
            .append(formatDescription(description))
            .append("</div>")
            .append("</div>");

        return html.toString();
    }

    /**
     * 格式化描述内容，添加段落分隔
     */
    private String formatDescription(String description) {
        if (description == null || description.isEmpty()) {
            return "暂无详细描述";
        }

        // 将长文本按句号分段，提高可读性
        String[] sentences = description.split("。");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (!sentence.isEmpty()) {
                if (i > 0 && i % 3 == 0) {
                    formatted.append("<br><br>");
                }
                formatted.append(sentence);
                if (i < sentences.length - 1) {
                    formatted.append("。");
                }
            }
        }

        return formatted.toString();
    }

    /**
     * 动态生成sections HTML
     */
    private String generateDynamicSections(List<String> keyPoints, List<String> descriptions) {
        if (keyPoints == null || keyPoints.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();

        for (int i = 0; i < keyPoints.size(); i++) {
            String keyPoint = keyPoints.get(i);
            String description = i < descriptions.size() ? descriptions.get(i) : keyPoint;

            html.append("<div class=\"section\">")
                .append("<div class=\"num\">").append(i + 1).append("</div>")
                .append("<div class=\"section-content\">")
                .append("<span class=\"section-title\">").append(keyPoint).append("</span>")
                .append(description)
                .append("</div>")
                .append("</div>");
        }

        return html.toString();
    }

    /**
     * 生成结尾文本
     */
    private String generateEndingText(String keyPoint) {
        String[] endings = {
            "记得点赞收藏哦！",
            "希望对你有帮助～",
            "一起变得更好吧！",
            "分享给更多朋友～",
            "期待你的尝试！"
        };

        // 根据keyPoint的hash选择结尾，保证同样的keyPoint总是得到同样的结尾
        int index = Math.abs(keyPoint.hashCode()) % endings.length;
        return endings[index];
    }



    /**
     * 生成Markdown内容
     */
    private String generateMarkdownContent(NoteContent noteContent) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(noteContent.getTitle()).append("\n\n");

        for (int i = 0; i < noteContent.getKeyPoints().size(); i++) {
            markdown.append("## ").append(i + 1).append(". ").append(noteContent.getKeyPoints().get(i)).append("\n\n");
            
            if (i < noteContent.getDescriptions().size()) {
                markdown.append(noteContent.getDescriptions().get(i)).append("\n\n");
            }
        }

        return markdown.toString();
    }

    /**
     * 获取任务数据
     */
    public XhsNoteResponse getNoteData(String taskId) {
        XhsNoteResponse data = taskDataMap.get(taskId);
        if (data == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        return data;
    }

    /**
     * 生成笔记包（ZIP文件）
     */
    public byte[] generateNotePackage(String taskId) throws Exception {
        XhsNoteResponse noteData = getNoteData(taskId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            // 1. 添加Markdown文件
            if (noteData.getMarkdownContent() != null) {
                ZipEntry markdownEntry = new ZipEntry("笔记内容.md");
                zos.putNextEntry(markdownEntry);
                zos.write(noteData.getMarkdownContent().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            // 2. 添加封面HTML文件
            if (noteData.getCoverImageHtml() != null) {
                ZipEntry coverEntry = new ZipEntry("封面图.html");
                zos.putNextEntry(coverEntry);
                zos.write(noteData.getCoverImageHtml().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            // 3. 添加内容HTML文件
            if (noteData.getContentImageHtmls() != null) {
                for (int i = 0; i < noteData.getContentImageHtmls().size(); i++) {
                    String html = noteData.getContentImageHtmls().get(i);
                    ZipEntry contentEntry = new ZipEntry("核心要点" + (i + 1) + ".html");
                    zos.putNextEntry(contentEntry);
                    zos.write(html.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }

            // 4. 添加说明文件
            String readme = generateReadmeContent(noteData);
            ZipEntry readmeEntry = new ZipEntry("使用说明.txt");
            zos.putNextEntry(readmeEntry);
            zos.write(readme.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

        }

        return baos.toByteArray();
    }

    /**
     * 生成说明文件内容
     */
    private String generateReadmeContent(XhsNoteResponse noteData) {
        StringBuilder readme = new StringBuilder();
        readme.append("小红书笔记生成包\n");
        readme.append("==================\n\n");
        readme.append("标题: ").append(noteData.getTitle()).append("\n");
        readme.append("任务ID: ").append(noteData.getTaskId()).append("\n");
        readme.append("生成时间: ").append(new Date().toString()).append("\n\n");

        readme.append("文件说明:\n");
        readme.append("- 笔记内容.md: Markdown格式的笔记内容\n");
        readme.append("- 封面图.html: 封面图的HTML文件，可在浏览器中打开并截图\n");

        if (noteData.getContentImageHtmls() != null) {
            for (int i = 0; i < noteData.getContentImageHtmls().size(); i++) {
                readme.append("- 核心要点").append(i + 1).append(".html: 第").append(i + 1).append("个核心要点的详细展示\n");
            }
        }

        readme.append("\n使用方法:\n");
        readme.append("1. 在浏览器中打开HTML文件\n");
        readme.append("2. 使用浏览器的截图功能或开发者工具截取图片\n");
        readme.append("3. 将图片保存并上传到小红书\n");
        readme.append("4. 复制笔记内容.md中的文字作为文案\n\n");

        readme.append("注意事项:\n");
        readme.append("- HTML文件需要在浏览器中打开才能正常显示\n");
        readme.append("- 建议使用Chrome浏览器获得最佳显示效果\n");
        readme.append("- 可以调整浏览器窗口大小来获得合适的图片尺寸\n");

        return readme.toString();
    }

    /**
     * 读取模板文件内容
     */
    private String readTemplateFile(String templatePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(templatePath);
        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    /**
     * 笔记内容内部类
     */
    private static class NoteContent {
        private String title;
        private List<String> keyPoints;
        private List<String> descriptions;
        private String imageDescription;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<String> getKeyPoints() { return keyPoints; }
        public void setKeyPoints(List<String> keyPoints) { this.keyPoints = keyPoints; }
        public List<String> getDescriptions() { return descriptions; }
        public void setDescriptions(List<String> descriptions) { this.descriptions = descriptions; }
        public String getImageDescription() { return imageDescription; }
        public void setImageDescription(String imageDescription) { this.imageDescription = imageDescription; }
    }
} 