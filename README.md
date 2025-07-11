# AI短视频生成器

基于Spring Boot + Thymeleaf的AI短视频生成Web应用，集成DeepSeek AI、阿里云文生图和阿里云ICE视频生成技术。

## 功能特性

- 🤖 **AI智能脚本生成**: 基于DeepSeek AI技术，自动分析文本内容生成专业短视频脚本
- 🎨 **智能配图**: 根据脚本内容自动生成匹配的高质量图片
- 🎬 **一键生成**: 集成阿里云ICE视频生成技术，一键生成专业级短视频
- 🌐 **Web界面**: 现代化的响应式Web界面，支持实时进度显示
- 📱 **移动端适配**: 完美支持手机、平板等移动设备

## 技术栈

- **后端**: Spring Boot 3.2.0, Java 17
- **前端**: Thymeleaf, Bootstrap 5, JavaScript
- **AI服务**: DeepSeek AI, 阿里云文生图
- **视频生成**: 阿里云ICE
- **存储**: 阿里云OSS
- **构建工具**: Maven

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+

### 安装步骤

1. **克隆项目**
   ```bash
   cd loscy-video-generate
   ```

2. **配置API密钥**
   
   编辑 `src/main/resources/deepseek-config.properties`:
   ```properties
   deepseek.api.key=your_deepseek_api_key
   ```

3. **编译运行**
   ```bash
   mvn clean package
   java -jar target/loscy-video-generate-1.0.0.jar
   ```

4. **访问应用**
   
   打开浏览器访问: http://localhost:8080

### 使用说明

1. 在首页点击"立即开始"按钮
2. 输入您想要制作成视频的文本内容
3. 点击"开始生成"按钮
4. 等待AI自动生成脚本、图片和视频
5. 下载生成的视频文件

## 项目结构

```
loscy-video-generate/
├── src/
│   ├── main/
│   │   ├── java/pers/loscy/
│   │   │   ├── VideoGenerateApplication.java    # Spring Boot主类
│   │   │   ├── controller/
│   │   │   │   └── VideoController.java         # 控制器
│   │   │   ├── config/
│   │   │   │   └── AppConfig.java               # 配置类
│   │   │   ├── core/
│   │   │   │   └── CoreService.java             # 核心服务
│   │   │   ├── deepseek/
│   │   │   │   ├── DeepSeekAi.java              # DeepSeek AI服务
│   │   │   │   └── DeepSeekConfig.java          # DeepSeek配置
│   │   │   ├── image/
│   │   │   │   └── ImageGenerate.java           # 图片生成服务
│   │   │   └── AliUpload.java                   # OSS上传服务
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── index.html                   # 首页模板
│   │       │   └── generate.html                # 生成页面模板
│   │       ├── application.properties           # 应用配置
│   │       └── deepseek-config.properties       # DeepSeek配置
│   └── test/
│       └── java/pers/loscy/
│           └── VideoGenerateApplicationTests.java
├── pom.xml                                      # Maven配置
└── README.md                                    # 项目说明
```

## API接口

### 生成视频
- **POST** `/video/generate`
- **参数**: `inputText` (文本内容)
- **返回**: JSON格式的生成结果

### 查询状态
- **GET** `/video/status/{jobId}`
- **参数**: `jobId` (任务ID)
- **返回**: JSON格式的状态信息

### 等待完成
- **POST** `/video/wait/{jobId}`
- **参数**: `jobId` (任务ID), `timeout` (超时时间)
- **返回**: JSON格式的完成结果

## 配置说明

### 应用配置 (application.properties)
- `server.port`: 服务器端口
- `spring.thymeleaf.cache`: Thymeleaf缓存开关
- `logging.level`: 日志级别配置

### DeepSeek配置 (deepseek-config.properties)
- `deepseek.api.key`: DeepSeek API密钥

## 开发说明

### 添加新功能
1. 在 `controller` 包中添加新的控制器
2. 在 `core` 包中添加业务逻辑
3. 在 `templates` 目录中添加对应的模板文件

### 自定义样式
- 修改 `templates` 目录下的HTML文件中的CSS样式
- 或创建独立的CSS文件并在模板中引用

## 部署说明

### 生产环境部署
1. 修改 `application.properties` 中的配置
2. 使用 `mvn clean package` 打包
3. 使用 `java -jar` 启动应用

### Docker部署
```dockerfile
FROM openjdk:17-jre-slim
COPY target/loscy-video-generate-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 常见问题

### Q: 如何修改API密钥？
A: 编辑 `src/main/resources/deepseek-config.properties` 文件中的 `deepseek.api.key` 值。

### Q: 如何修改服务器端口？
A: 在 `application.properties` 中修改 `server.port` 配置。

### Q: 生成的视频保存在哪里？
A: 视频文件保存在阿里云OSS中，通过返回的URL可以访问和下载。

## 许可证

本项目采用 MIT 许可证。

## 作者

徐天 - 2025年1月

## 更新日志

### v1.0.0 (2025-01-09)
- 初始版本发布
- 支持AI脚本生成
- 支持图片自动生成
- 支持视频一键生成
- 提供Web界面 