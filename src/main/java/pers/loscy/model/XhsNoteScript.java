package pers.loscy.model;

import java.util.List;

/**
 * 小红书笔记脚本（第一步生成的内容）
 * @author 徐天
 * @create 2025/7/14
 */
public class XhsNoteScript {
    
    private String title;                    // 标题
    private List<String> keyPoints;          // 核心要点
    private List<String> descriptions;       // 每个要点的详细描述
    private String imageDescription;         // 图片描述
    private String originalContent;          // 原始输入内容
    
    public XhsNoteScript() {}
    
    public XhsNoteScript(String title, List<String> keyPoints, List<String> descriptions, String imageDescription) {
        this.title = title;
        this.keyPoints = keyPoints;
        this.descriptions = descriptions;
        this.imageDescription = imageDescription;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public List<String> getKeyPoints() {
        return keyPoints;
    }
    
    public void setKeyPoints(List<String> keyPoints) {
        this.keyPoints = keyPoints;
    }
    
    public List<String> getDescriptions() {
        return descriptions;
    }
    
    public void setDescriptions(List<String> descriptions) {
        this.descriptions = descriptions;
    }
    
    public String getImageDescription() {
        return imageDescription;
    }
    
    public void setImageDescription(String imageDescription) {
        this.imageDescription = imageDescription;
    }
    
    public String getOriginalContent() {
        return originalContent;
    }
    
    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }
    
    @Override
    public String toString() {
        return "XhsNoteScript{" +
                "title='" + title + '\'' +
                ", keyPoints=" + keyPoints +
                ", descriptions=" + descriptions +
                ", imageDescription='" + imageDescription + '\'' +
                ", originalContent='" + originalContent + '\'' +
                '}';
    }
}
