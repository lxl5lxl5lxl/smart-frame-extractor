# 智能帧提取器

一个基于Spring Boot的应用程序，用于从RTSP视频流中智能提取关键帧，并通过运动检测将其推送到指定的HTTP服务器。

## 🚀 功能特性

- **RTSP通道管理**：添加、更新、启动、停止和列出RTSP通道
- **智能帧提取**：使用OpenCV仅提取有运动的关键帧，减少网络传输和存储开销
- **HTTP帧推送**：将提取的帧发送到配置的算法服务器或存储服务
- **可配置参数**：灵活调整检测设置、推送间隔、编码质量等
- **REST API**：通过RESTful接口轻松管理通道
- **健壮的错误处理**：自动重连机制和完善的异常处理
- **实时日志**：关键操作和事件的详细日志记录
- **本地帧接收**：内置帧接收端点，方便测试和调试

## 🏗️ 架构设计

### 核心组件

| 组件 | 职责 |
|------|------|
| **ChannelManager** | 管理RTSP通道生命周期，维护通道列表和运行时状态，使用线程池管理工作线程 |
| **RtspWorker** | 从RTSP流拉取视频帧，实现帧过滤逻辑，处理连接异常和重连 |
| **MotionFilter** | 实现运动检测算法，比较帧差异以检测运动，支持可配置的运动阈值 |
| **FramePusher** | 将视频帧编码为JPEG格式，使用OkHttp将帧推送到配置的HTTP端点 |
| **ChannelController** | 提供通道管理的REST API端点，验证请求并处理错误 |
| **FrameReceiverController** | 提供本地帧接收端点，用于测试和调试 |

### 工作流程

1. **通道管理**：通过REST API创建和管理RTSP通道
2. **流抓取**：RtspWorker从RTSP流中持续抓取视频帧
3. **帧过滤**：根据配置的帧率间隔过滤帧，减少处理压力
4. **运动检测**：MotionFilter检测帧中是否存在运动
5. **帧推送**：将检测到运动的帧编码为JPEG并推送到指定服务器
6. **结果接收**：本地或远程服务器接收并处理推送的帧

## 📦 快速开始

### 前置条件

- Java 17+
- Maven 3.6+
- 用于测试的RTSP视频流

### 安装步骤

1. **克隆仓库**
   ```bash
   git clone https://github.com/your-username/smart-frame-extractor.git
   cd smart-frame-extractor
   ```

2. **构建项目**
   ```bash
   mvn clean package
   ```

3. **运行应用程序**
   ```bash
   java -jar target/smart-frame-extractor.jar
   ```

4. **访问API**
   - 应用程序将在 `http://localhost:8080` 启动
   - 使用API工具（如Postman、curl）测试接口

## ⚙️ 配置说明

应用程序使用YAML格式的配置文件，默认配置文件为 `application.yml`。以下是主要配置参数：

```yaml
# 应用服务器配置
server:
  port: 8080

# 日志配置
logging:
  level:
    root: info
    com.lxl.smartframeextractor: info
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# 智能帧提取器配置
extractor:
  # 算法服务器URL，用于推送检测到的帧
  algoUrl: "http://localhost:8080/api/frames"
  
  # 帧检测设置
  maxCheckFps: 5               # 最大检测帧率
  minPushIntervalMs: 300       # 最小推送间隔（毫秒）
  detectWidth: 320             # 检测宽度（降低分辨率以提高性能）
  detectHeight: 180            # 检测高度
  
  # 运动检测阈值
  madThreshold: 2.5            # 平均绝对差异阈值
  motionAreaRatioThreshold: 0.003  # 运动面积比例阈值
  
  # 心跳推送设置
  keepAlivePushIntervalSec: 20  # 心跳推送间隔（秒）
  
  # JPEG编码设置
  jpegQuality: 80              # JPEG编码质量（0-100）
  
  # RTSP连接设置
  rtspOpenTimeoutMs: 5000      # RTSP连接超时（毫秒）
  rtspReadTimeoutMs: 5000      # RTSP读取超时（毫秒）
  reconnectBackoffMs: 2000     # 重连退避时间（毫秒）
  
  # 线程池设置
  workerThreads: 64            # 工作线程池大小
```

## 📋 API文档

### 通道管理

#### 添加/更新通道
```http
POST /channels
Content-Type: application/json

{
  "cameraId": "cam001",
  "rtspUrl": "rtsp://example.com/stream"
}
```

#### 启动通道
```http
POST /channels/{cameraId}/start
```

#### 停止通道
```http
POST /channels/{cameraId}/stop
```

#### 列出所有通道
```http
GET /channels
```

#### 获取通道详情
```http
GET /channels/{cameraId}
```

### 帧接收接口

#### 接收推送的帧
```http
POST /api/frames
Content-Type: multipart/form-data

# 参数
- cameraId: 相机ID（字符串）
- timestamp: 时间戳（毫秒）
- image: 图片文件（multipart/form-data）

# 返回
- 成功："Frame received and saved successfully"
- 失败："Failed to save frame"
```

**说明**：
- 接收到的图片将保存到应用程序根目录下的 `received_frames` 目录
- 文件名格式：`时间戳.jpg`（例如：`1767603034414.jpg`）
- 支持任意图片格式，保存时统一使用 `.jpg` 扩展名

## 🛠️ 构建和运行

### 构建项目
```bash
mvn clean package
```

### 运行应用
```bash
# 使用默认配置运行
java -jar target/smart-frame-extractor.jar

# 使用自定义配置文件运行
java -jar target/smart-frame-extractor.jar --spring.config.location=file:/path/to/application.yml
```

### 开发模式
```bash
# 使用Spring Boot Maven插件运行
mvn spring-boot:run
```

## 📁 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── lxl/
│   │           └── smartframeextractor/
│   │               ├── api/            # REST API控制器
│   │               │   ├── ChannelController.java
│   │               │   └── FrameReceiverController.java
│   │               ├── config/         # 配置类
│   │               │   └── ExtractorProperties.java
│   │               ├── core/           # 核心业务逻辑
│   │               │   ├── FramePusher.java
│   │               │   ├── MotionFilter.java
│   │               │   └── RtspWorker.java
│   │               ├── service/        # 服务层
│   │               │   └── ChannelManager.java
│   │               └── SmartFrameExtractorApplication.java  # 主应用程序类
│   └── resources/
│       └── application.yml            # 应用程序配置
└── test/                               # 测试文件（待完善）
```

## 🎯 运动检测算法

应用程序使用一种高效的运动检测算法，主要步骤如下：

1. **帧预处理**：将彩色帧转换为灰度图，并调整大小以提高处理速度
2. **高斯模糊**：对灰度图应用高斯模糊，减少噪声影响
3. **帧差计算**：计算当前帧与前一帧之间的绝对差异
4. **阈值处理**：对差异图像进行阈值处理，生成二进制运动掩码
5. **特征计算**：
   - **平均绝对差异(MAD)**：计算差异图像的平均像素值
   - **运动面积比例**：计算运动区域占总区域的比例
6. **运动判断**：当MAD或运动面积比例超过配置的阈值时，判定为检测到运动
7. **帧推送**：将检测到运动的帧推送到指定服务器

## 📊 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.5.15 | 应用程序框架 |
| JavaCV | 1.5.10 | 视频处理（FFmpeg + OpenCV） |
| OkHttp | 4.12.0 | HTTP客户端 |
| Spring Validation | 内置 | 请求参数验证 |
| Maven | 3.6+ | 项目构建和依赖管理 |
| Java | 17+ | 开发语言 |
| SLF4J | 内置 | 日志框架 |

## 📝 使用示例

### 1. 创建RTSP通道
```bash
curl -X POST http://localhost:8080/channels \
  -H "Content-Type: application/json" \
  -d '{"cameraId":"cam001","rtspUrl":"rtsp://example.com/stream"}'
```

### 2. 启动通道
```bash
curl -X POST http://localhost:8080/channels/cam001/start
```

### 3. 查看通道列表
```bash
curl -X GET http://localhost:8080/channels
```

### 4. 停止通道
```bash
curl -X POST http://localhost:8080/channels/cam001/stop
```

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

1. Fork本仓库
2. 创建特性分支：`git checkout -b feature/AmazingFeature`
3. 提交更改：`git commit -m 'Add some AmazingFeature'`
4. 推送到分支：`git push origin feature/AmazingFeature`
5. 打开Pull Request

## 📄 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📧 联系方式

如有问题或建议，欢迎通过以下方式联系：

- 项目地址：https://github.com/lxl5lxl5lxl/smart-frame-extractor
- Issues：https://github.com/lxl5lxl5lxl/smart-frame-extractor/issues

## 🙏 致谢

- [JavaCV](https://github.com/bytedeco/javacv) - 提供强大的视频处理能力
- [OpenCV](https://opencv.org/) - 计算机视觉库
- [Spring Boot](https://spring.io/projects/spring-boot) - 简化应用程序开发

---

**享受使用智能帧提取器！** 🎉
