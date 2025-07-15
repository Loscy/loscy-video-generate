package pers.loscy.model;

import java.util.List;

/**
 * 小红书笔记生成响应
 * @author 徐天
 * @create 2025/7/10 14:42
 */
public class XhsNoteResponse {
    
    private boolean success;
    private String message;
    private String taskId;
    private String title;
    private String summary;
    private List<String> keyPoints;
    private String coverImageHtml;
    private List<String> contentImageHtmls;
    private String markdownContent;
    private String imageDescription;
    
    public XhsNoteResponse() {}

    public String getImageDescription() {
        return imageDescription;
    }

    public void setImageDescription(String imageDescription) {
        this.imageDescription = imageDescription;
    }


    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public List<String> getKeyPoints() {
        return keyPoints;
    }
    
    public void setKeyPoints(List<String> keyPoints) {
        this.keyPoints = keyPoints;
    }
    
    public String getCoverImageHtml() {
        return coverImageHtml;
    }
    
    public void setCoverImageHtml(String coverImageHtml) {
        this.coverImageHtml = coverImageHtml;
    }
    
    public List<String> getContentImageHtmls() {
        return contentImageHtmls;
    }
    
    public void setContentImageHtmls(List<String> contentImageHtmls) {
        this.contentImageHtmls = contentImageHtmls;
    }
    
    public String getMarkdownContent() {
        return markdownContent;
    }
    
    public void setMarkdownContent(String markdownContent) {
        this.markdownContent = markdownContent;
    }
} 