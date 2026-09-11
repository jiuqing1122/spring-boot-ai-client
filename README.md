# Spring Boot AI Client

一个基于 **Spring Boot 4.x / Spring Framework 7** 的最小示例项目，演示如何通过 `RestClient` 调用 Python FastAPI 提供的 AI 服务，完成跨语言微服务调用，并支持 **SSE 流式转发** 实现打字机效果。

## ✨ 特性

- 🔗 **跨语言调用**：Spring Boot (Java) → FastAPI (Python) → DeepSeek / Ollama
- 🌊 **流式转发**：使用 `SseEmitter` 将 FastAPI 的 SSE 流原样推送给前端
- 🛡️ **统一异常处理**：`@RestControllerAdvice` 对齐 FastAPI 的响应格式
- ⚙️ **RestClient**：Spring Framework 7 官方推荐的同步 HTTP 客户端（替代已废弃的 `RestTemplate`）
- 📝 **最小依赖**：仅 `spring-boot-starter-web`

## 🏗️ 架构

```
┌──────────────┐                          ┌────────────────┐
│  浏览器       │  POST /test-ai/stream    │  Spring Boot   │
│  (fetch)     │ ───────────────────────► │  (本服务:8080)  │
└──────────────┘                          └───────┬────────┘
                                                  │ RestClient
                                                  │ (阻塞读 InputStream)
                                                  ▼
                                          ┌────────────────┐
                                          │   FastAPI      │
                                          │   (:8000)      │
                                          └───────┬────────┘
                                                  │
                                                  ▼
                                          ┌────────────────┐
                                          │ DeepSeek/Ollama│
                                          └────────────────┘
```

## 📁 项目结构

```
spring-boot-ai-client/
├── pom.xml
└── src/main/java/com/example/aiclient/
    ├── AiClientApplication.java
    ├── config/
    │   ├── RestClientConfig.java   # RestClient Bean（含超时配置）
    │   └── WebConfig.java          # CORS 配置
    ├── constant/
    │   └── FastApiConstant.java    # FastAPI 路径常量
    ├── controller/
    │   └── AiController.java       # /test-ai 与 /test-ai/stream
    ├── dto/
    │   ├── ChatRequest.java
    │   └── ChatResponse.java       # 兼容正常响应与业务异常
    ├── exception/
    │   ├── BizException.java
    │   └── GlobalExceptionHandler.java
    └── service/
        └── AIService.java          # chat() + chatStream()
```

## 🚀 快速开始

### 1. 环境准备

- JDK 17+
- Maven 3.9+
- **FastAPI 服务已启动**（见 `fastapi-service` 项目）

### 2. 启动

```bash
mvn spring-boot:run
```

或直接在 IDEA 中运行 `AiClientApplication`。

服务监听：http://localhost:8080

## 🔌 API

### `GET /test-ai`（非流式）

| 参数 | 类型 | 说明 |
|------|------|------|
| prompt | string | 用户输入 |

**示例：**

```
GET http://localhost:8080/test-ai?prompt=你好
```

**响应：**

```json
{ "code": 0, "message": "success", "data": "你好！我是..." }
```

### `POST /test-ai/stream`（流式 SSE）

**请求：**

```json
{ "prompt": "你好" }
```

**响应（`text/event-stream`）：**

```
data:{"type":"message","content":"你"}
data:{"type":"message","content":"好"}
data:{"type":"done"}
```

## ⚙️ 配置

`src/main/resources/application.yml`：

```yaml
server:
  port: 8080

fastapi:
  base-url: http://localhost:8000
  connect-timeout: 5000    # 连接超时（毫秒）
  read-timeout: 60000      # 读超时（毫秒），AI 调用较慢，需较大

spring:
  mvc:
    async:
      request-timeout: -1  # SSE 不超时
```

## 🧪 测试

### 用 curl.exe（Windows PowerShell）

```powershell
# 准备 body.json
'{"prompt":"你好"}' | Out-File -Encoding utf8 body.json

# 非流式
curl.exe "http://localhost:8080/test-ai?prompt=hello"

# 流式
curl.exe -N -X POST "http://localhost:8080/test-ai/stream" `
  -H "Content-Type: application/json" `
  --data-binary "@body.json"
```

### 浏览器测试

直接用浏览器打开 `src/main/resources/static/sse_test.html`，把 API_URL 改为：

```javascript
const API_URL = 'http://localhost:8080/test-ai/stream';
```

即可看到打字机效果。

## 🕳️ 踩坑记录

| 现象 | 根因 | 解法 |
|------|------|------|
| `postForObject` 返回 null | Spring Framework 7 中 `RestTemplate` 的 JSON 转换器失效 | 改用 `RestClient` |
| JSON parse error | PowerShell 把 `-d $body` 的双引号吃掉 | 用 `--data-binary "@body.json"` |
| SSE 一直不推送 | Servlet 容器 AsyncContext 30s 超时 | `spring.mvc.async.request-timeout: -1` |
| CORS 跨域被拦 | 无 CORS 配置 | `WebConfig implements WebMvcConfigurer` |
| GET 带中文 400 | URL 未编码 | 改用 POST + JSON body |

## ⚠️ 特别说明

### 为什么用 `RestClient` 而不是 `RestTemplate`？

Spring Framework 7 中 `RestTemplate` 已被标记为 **legacy**，其内部的 JSON 消息转换器配置不完整，会导致 `postForObject(..., XxxClass)` 静默返回 `null`。而 `RestClient` 从 Spring 6.1 引入，是官方推荐的现代同步 HTTP 客户端。

### 为什么流式转发需要 `SseEmitter`？

`SseEmitter` 允许 controller 先返回一个"空响应"，之后由其他线程逐步写入数据。这是 Spring MVC 处理 SSE / 长连接的标准方式。同步返回 `ResponseEntity` 无法实现流式推送。

## 📄 License

MIT
