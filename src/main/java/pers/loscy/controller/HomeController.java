package pers.loscy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器
 * @author 徐天
 * @create 2025/7/10 14:42
 */
@Controller
public class HomeController {

    /**
     * 首页，显示主页面
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }
} 