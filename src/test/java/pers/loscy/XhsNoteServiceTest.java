package pers.loscy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pers.loscy.model.XhsNoteRequest;
import pers.loscy.model.XhsNoteResponse;
import pers.loscy.service.XhsNoteService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 小红书笔记服务测试
 * @author 徐天
 * @create 2025/7/10 14:42
 */
@SpringBootTest
public class XhsNoteServiceTest {

    @Test
    public void testGenerateNote() throws Exception {
        // 创建服务实例
        XhsNoteService service = new XhsNoteService();
        
        // 创建测试请求
        XhsNoteRequest request = new XhsNoteRequest();
        request.setContent("如何提高工作效率？这是一个非常重要的话题，我们需要从时间管理、任务优先级、专注力等多个方面来探讨。");
        
        // 生成笔记
        XhsNoteResponse response = service.generateNote(request);
        
        // 验证结果
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getTaskId());
        assertNotNull(response.getTitle());
        assertNotNull(response.getSummary());
        assertNotNull(response.getKeyPoints());
        assertNotNull(response.getCoverImageHtml());
        assertNotNull(response.getContentImageHtmls());
        assertNotNull(response.getMarkdownContent());
        
        // 验证HTML内容
        assertTrue(response.getCoverImageHtml().contains("${mainTitle}") == false);
        assertTrue(response.getCoverImageHtml().contains("<!DOCTYPE html>"));
        
        // 验证内容HTML
        assertFalse(response.getContentImageHtmls().isEmpty());
        for (String html : response.getContentImageHtmls()) {
            assertTrue(html.contains("<!DOCTYPE html>"));
            assertTrue(html.contains("${infoTitle}") == false);
        }
        
        System.out.println("测试通过！");
        System.out.println("任务ID: " + response.getTaskId());
        System.out.println("标题: " + response.getTitle());
        System.out.println("核心要点数量: " + response.getKeyPoints().size());
        System.out.println("内容HTML数量: " + response.getContentImageHtmls().size());
    }
} 