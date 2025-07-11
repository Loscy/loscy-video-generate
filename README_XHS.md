# 小红书笔记生成器

一个基于AI的小红书笔记自动生成工具，能够根据用户输入的内容自动生成标题、核心要点、图片和文案。

## 功能特性

- 🤖 **AI内容生成**：使用DeepSeek AI自动总结用户输入内容，生成吸引人的标题和核心要点
- 🎨 **智能图片生成**：使用阿里云百炼文生图服务生成封面图和内容图片
- 📝 **文案自动生成**：根据内容自动生成符合小红书风格的文案
- 📦 **一键打包下载**：将所有生成的图片和文案打包成ZIP文件供下载
- 🌐 **Web界面**：提供美观的Web界面，支持实时生成和预览

## 技术架构

- **后端框架**：Spring Boot 2.7.14
- **模板引擎**：Thymeleaf
- **AI服务**：DeepSeek AI API
- **图片生成**：阿里云百炼文生图
- **文件存储**：阿里云OSS
- **前端**：HTML5 + CSS3 + JavaScript

## 快速开始

### 1. 环境要求

- Java 8+
- Maven 3.6+
- 有效的DeepSeek API密钥
- 阿里云OSS配置

### 2. 配置设置

#### DeepSeek配置
在 `src/main/resources/deepseek-config.properties` 中配置：
```properties
deepseek.api.key=your_deepseek_api_key_here
```

#### OSS配置
在 `src/main/resources/oss-config.properties` 中配置：
```properties
# 百炼配置
bailian.apiKey=your_bailian_api_key
bailian.endpoint=your_bailian_endpoint
bailian.region=your_bailian_region
```

### 3. 启动应用

```bash
# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

应用将在 `http://localhost:8787` 启动。

### 4. 使用流程

1. **访问首页**：打开浏览器访问 `http://localhost:8787`
2. **输入内容**：在文本框中详细描述你想要生成笔记的内容
3. **选择风格**：选择生成风格（默认、生活方式、美妆护肤等）
4. **设置图片数量**：选择要生成的内容图片数量（1-5张）
5. **开始生成**：点击"开始生成"按钮
6. **查看结果**：等待生成完成后查看生成的图片和文案
7. **下载文件**：点击"下载笔记包"获取ZIP文件

## 生成流程

### 1. 内容分析
- 用户输入原始内容
- 使用DeepSeek AI分析内容并提取关键信息
- 生成吸引人的标题和3-5个核心要点

### 2. 图片生成
- **封面图片**：根据标题生成小红书风格的封面图
- **内容图片**：为每个核心要点生成对应的内容图片
- 使用阿里云百炼文生图服务，支持自定义风格和尺寸

### 3. 文案生成
- 根据分析结果生成符合小红书风格的文案
- 包含标题、核心要点和详细描述
- 输出为Markdown格式，便于编辑和发布

### 4. 文件打包
- 将所有生成的图片和文案打包成ZIP文件
- 包含封面图片、内容图片和文案文件
- 支持一键下载

## API接口

### 生成笔记
```
POST /xhs/generate
Content-Type: application/json

{
    "content": "用户输入的内容",
    "style": "default",
    "imageCount": 3
}
```

### 查看结果
```
GET /xhs/result/{taskId}
```

### 下载文件
```
GET /xhs/download/{taskId}
```

## 项目结构

```
src/main/java/pers/loscy/
├── controller/
│   ├── HomeController.java          # 首页控制器
│   └── XhsController.java           # 小红书笔记控制器
├── model/
│   ├── XhsNoteRequest.java          # 请求数据模型
│   └── XhsNoteResponse.java         # 响应数据模型
├── service/
│   └── XhsNoteService.java          # 核心业务服务
├── deepseek/
│   ├── DeepSeekAi.java              # DeepSeek AI客户端
│   └── DeepSeekConfig.java          # DeepSeek配置
├── image/
│   └── ImageGenerate.java           # 图片生成服务
└── AliUpload.java                   # OSS上传工具

src/main/resources/
├── templates/
│   ├── xhsGenerate.html             # 生成页面
│   ├── xhsResult.html               # 结果页面
│   ├── error.html                   # 错误页面
│   ├── xhsCoverTemplate.html        # 封面模板
│   └── xhsInfoTemplate.html         # 内容模板
├── application.properties           # 应用配置
├── deepseek-config.properties       # DeepSeek配置
└── oss-config.properties           # OSS配置
```

## 自定义配置

### 修改端口
在 `application.properties` 中修改：
```properties
server.port=8080
```

### 修改图片尺寸
在 `XhsNoteService.java` 中修改图片生成参数：
```java
// 封面图片尺寸
imageGenerate.generateImage(prompt, null, 1080, 1920, null, null);

// 内容图片尺寸
imageGenerate.generateImage(prompt, null, 1080, 1920, null, null);
```

### 自定义模板
可以修改 `xhsCoverTemplate.html` 和 `xhsInfoTemplate.html` 来自定义图片样式。

## 注意事项

1. **API限制**：注意DeepSeek和阿里云百炼的API调用限制
2. **图片质量**：生成的图片质量取决于AI模型和提示词的质量
3. **内容审核**：生成的内容需要符合平台规范
4. **存储空间**：注意OSS存储空间的使用情况

## 故障排除

### 常见问题

1. **API密钥错误**
   - 检查配置文件中的API密钥是否正确
   - 确认API密钥是否有效且有足够配额

2. **图片生成失败**
   - 检查网络连接
   - 确认阿里云百炼服务是否正常
   - 检查提示词是否符合规范

3. **文件上传失败**
   - 检查OSS配置是否正确
   - 确认OSS权限设置
   - 检查网络连接

### 日志查看
应用日志位于 `logs/` 目录下，可以通过查看日志来诊断问题。

## 更新日志

### v1.0.0 (2025-07-10)
- 初始版本发布
- 支持基础的小红书笔记生成功能
- 集成DeepSeek AI和阿里云百炼服务
- 提供Web界面和文件下载功能

## 许可证

本项目仅供学习和研究使用，请遵守相关平台的使用条款。 