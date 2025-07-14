package pers.loscy.model;

/**
 * 小红书笔记生成请求
 * @author 徐天
 * @create 2025/7/10 14:42
 */
public class XhsNoteRequest {

    private String content; // 用户输入的内容

    public XhsNoteRequest() {}

    public XhsNoteRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "XhsNoteRequest{" +
                "content='" + content + '\'' +
                '}';
    }
}