# Spring Boot AI Client - 项目规划

## 一、项目概述

基于 Spring Boot 构建的 AI 客户端，通过 RestTemplate 调用 Python FastAPI 服务，提供 AI 对话接口。

## 二、目录结构

```
spring-boot-ai-client/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/example/aiclient/
        │       ├── AiClientApplication.java          # 启动类
        │       ├── config/
        │       │   └── RestTemplateConfig.java       # RestTemplate Bean 配置
        │       ├── dto/
        │       │   ├── ChatRequest.java              # 请求体
        │       │   └── ChatResponse.java             # 响应体（兼容正常+异常）
        │       ├── service/
        │       │   └── AiService.java                # 调用 FastAPI 的服务类
        │       ├── controller/
        │       │   └── AiController.java             # /test-ai 接口
        │       └── exception/
        │           ├── BizException.java             # 自定义业务异常（对齐 FastAPI）
        │           └── GlobalExceptionHandler.java   # @RestControllerAdvice
        └── resources/
            └── application.yml
```

## 三、模块说明

| 模块 | 文件 | 职责 |
|------|------|------|
| 启动类 | `AiClientApplication.java` | Spring Boot 应用入口 |
| 配置层 | `config/RestTemplateConfig.java` | 注册 RestTemplate Bean，配置 HTTP 调用参数 |
| 数据传输层 | `dto/ChatRequest.java` | 封装发送给 FastAPI 的请求体 |
| | `dto/ChatResponse.java` | 封装 FastAPI 返回的响应体，兼容正常与异常数据 |
| 服务层 | `service/AiService.java` | 核心业务逻辑，通过 RestTemplate 调用 FastAPI |
| 控制层 | `controller/AiController.java` | 暴露 `/test-ai` 接口，接收前端请求 |
| 异常处理 | `exception/BizException.java` | 自定义业务异常，与 FastAPI 错误码对齐 |
| | `exception/GlobalExceptionHandler.java` | 全局异常拦截器，统一返回格式 |
| 配置 | `resources/application.yml` | 项目配置文件（端口、FastAPI 地址等） |

## 四、技术栈

- **框架**：Spring Boot
- **HTTP 客户端**：RestTemplate
- **后端服务**：Python FastAPI
- **配置格式**：YAML