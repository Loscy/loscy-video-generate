# 小红书笔记生成器 - 快速开始

## 🚀 5分钟快速上手

### 1. 环境准备
确保你的系统已安装：
- Java 8 或更高版本
- Maven 3.6 或更高版本

### 2. 配置API密钥

#### 步骤1：配置DeepSeek API
```bash
# 复制配置文件模板
cp src/main/resources/deepseek-config.properties.template src/main/resources/deepseek-config.properties

# 编辑配置文件，填入你的DeepSeek API密钥
# 获取地址: https://platform.deepseek.com/
```

#### 步骤2：配置阿里云百炼
```bash
# 复制配置文件模板
cp src/main/resources/oss-config.properties.template src/main/resources/oss-config.properties

# 编辑配置文件，填入你的百炼配置
# 获取地址: https://bailian.console.aliyun.com/
```

### 3. 启动应用
```bash
# 方式1：使用启动脚本（推荐）
./start.sh

# 方式2：手动启动
mvn clean compile
mvn spring-boot:run
```

### 4. 开始使用
1. 打开浏览器访问：`http://localhost:8787`
2. 在文本框中输入你想要生成笔记的内容
3. 选择生成风格和图片数量
4. 点击"开始生成"
5. 等待生成完成后查看结果
6. 点击"下载笔记包"获取ZIP文件

## 📝 使用示例

### 输入内容示例：
```
我想分享一个关于健康生活的小红书笔记，内容包括：
1. 早起的好处 - 提高工作效率，改善精神状态
2. 健康饮食建议 - 均衡营养，少油少盐
3. 运动健身方法 - 每天30分钟有氧运动
4. 心理健康维护 - 保持积极心态，学会放松
```

### 生成结果：
- ✅ 吸引人的标题
- ✅ 3-5个核心要点
- ✅ 1张封面图片
- ✅ 3张内容图片
- ✅ 完整的Markdown文案
- ✅ 打包下载的ZIP文件

## 🔧 常见问题

### Q: 启动时提示"未找到Java环境"
A: 请安装Java 8或更高版本，并确保JAVA_HOME环境变量正确设置

### Q: 启动时提示"未找到Maven环境"
A: 请安装Maven 3.6或更高版本

### Q: 生成失败，提示API错误
A: 请检查配置文件中的API密钥是否正确，确认API服务是否正常

### Q: 图片生成失败
A: 请检查网络连接，确认阿里云百炼服务配置是否正确

## 📞 技术支持

如果遇到问题，请：
1. 查看应用日志：`logs/` 目录
2. 检查配置文件是否正确
3. 确认API服务状态

## 🎯 下一步

- 阅读完整文档：`README_XHS.md`
- 自定义模板样式
- 调整生成参数
- 集成到其他系统 