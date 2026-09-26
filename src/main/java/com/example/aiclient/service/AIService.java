package com.example.aiclient.service;

import com.example.aiclient.constant.FastApiConstant;
import com.example.aiclient.dto.ChatRequest;
import com.example.aiclient.dto.ChatResponse;
import com.example.aiclient.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
//@RequiredArgsConstructor
public class AIService {
    private final RestClient restClient;
    private final ThreadPoolTaskExecutor sseExecutor;

    /**
     * 显式构造函数 + @Qualifier，确保注入的是 sseExecutor 而不是 Spring Boot
     * 自动配置的 applicationTaskExecutor（两者都是 ThreadPoolTaskExecutor 类型）
     */
    public AIService(RestClient restClient,
                     @Qualifier("sseExecutor") ThreadPoolTaskExecutor sseExecutor) {
        this.restClient = restClient;
        this.sseExecutor = sseExecutor;
    }

    @Value("${fastapi.base-url}")
    private String fastapiBaseUrl;

    /**
     * 调用 FastAPI 的 /ai/chat 接口
     */
    public String chat(String prompt) {
        String apiUrl = fastapiBaseUrl + FastApiConstant.CHAT_API_URL;

        log.info("调用 FastAPI | url={} | prompt={}", apiUrl, prompt);

        ChatResponse response;
        try {
            response = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ChatRequest(prompt)) // 发送 JSON 请求体
                    .retrieve() // 执行请求
                    .body(ChatResponse.class); // 解析响应体为 ChatResponse 类型
        } catch (RestClientException e) {
            log.error("调用 FastAPI 失败", e);
            throw new BizException(50001, "调用 AI 服务失败：" + e.getMessage());
        }

        if (response == null) {
            throw new BizException(50002, "AI 服务返回空响应");
        }

        if (response.isBizError()) {
            log.warn("FastAPI 返回业务异常 | code={} | message={}",
                    response.getCode(), response.getMessage());
            throw new BizException(response.getCode(), response.getMessage());
        }

        return response.getReply();

    }

    /**
     * 流式调用 FastAPI，将 SSE 事件转发到 SseEmitter
     * P1 修复：完整转发 event: 和 data: 两行，前端才能区分 sources/message/done/error
     * P2 修复：用专用 sseExecutor，不再占用 commonPool
     * P3 修复：注册生命周期回调，客户端断开时关闭底层 InputStream，避免白烧 token
     */
    public void chatStream(String prompt, SseEmitter emitter) {
        String apiUrl = fastapiBaseUrl + FastApiConstant.CHAT_STREAM_API_URL;

        log.info("流式调用 FastAPI | url={} | prompt={}", apiUrl, prompt);

        // 独立线程执行（RestClient 是阻塞式，不能占用 Tomcat 请求线程）
        /*CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    restClient.post()
                            .uri(apiUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(new ChatRequest(prompt)) // 发送 JSON 请求体
                            .retrieve() // 执行请求
                            .body(InputStream.class),// 解析响应体为 InputStream 类型
                    StandardCharsets.UTF_8
            ))) {
                String line;
                // 读取响应体，直到结束
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        emitter.send(line.substring(5).trim());
                    }
                }
                emitter.complete(); // 标记流结束
            } catch (Exception e) {
                log.error("流式调用 FastAPI 失败 | prompt={}", prompt, e);
                emitter.completeWithError(e); // 标记流失败
            }
        });*/

        //修复后的代码
        sseExecutor.execute(() -> {
            BufferedReader reader = null;
            try {
                InputStream inputStream = restClient.post()
                        .uri(apiUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ChatRequest(prompt)) // 发送 JSON 请求体
                        .retrieve() // 执行请求
                        .body(InputStream.class); // 解析响应体为 InputStream 类型

                if(inputStream == null) {
                    sendError(emitter,50002, "AI 服务返回空响应");
                    emitter.complete();
                    return;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                // 注册生命周期回调：客户端断开 / 超时 / 异常时关闭底层流
                // 这样 Python 侧的生成器才会被取消，不会一直跑到结束
                BufferedReader finalReader = reader;
                emitter.onCompletion(() -> closeQuietly(finalReader));
                emitter.onTimeout(() -> closeQuietly(finalReader));
                emitter.onError((ex) -> closeQuietly(finalReader));

                String line;
                String eventName = null;
                while((line = reader.readLine()) != null) {
                    if(line.startsWith("event:")) {
                        eventName = line.substring(6).trim();
                    } else if(line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if(eventName != null) {
                            //完整转发：event名 + data体
                            emitter.send(SseEmitter.event().name(eventName).data(data));
                            eventName = null;// 重置事件名，准备下一次，配对消费
                        }else {
                            // 兼容旧接口（Python /ai/chat/stream 只发无名 data:）
                            emitter.send(data);
                        }
                    }
                }
                emitter.complete(); // 标记流结束
            } catch (IOException e) {
                // 客户端主动断开（关标签页）属于正常现象，记 warn 不要记 error
                log.warn("SSE 客户端断开或 IO 异常 | prompt={} | msg={}", prompt, e.getMessage());
                closeQuietly(reader);
                try {
                    sendError(emitter, 499, "客户端断开");
                } catch (Exception ignored) { }
                emitter.complete();
            } catch (Exception e) {
                log.error("流式调用 FastAPI 失败 | prompt={}", prompt, e);
                closeQuietly(reader);
                try {
                    sendError(emitter, 500, "AI 服务内部错误");
                } catch (Exception ignored) { }
                emitter.complete();
            }
        });

    }
    /** 发送标准 error 事件（流已经开始后只能靠 event 传递错误，不能改状态码） */
    private void sendError(SseEmitter emitter, int code, String message) {
        try {
            String json = String.format("{\"type\":\"error\",\"code\":%d,\"message\":\"%s\"}",
                    code, message.replace("\"", "\\\""));
            emitter.send(SseEmitter.event().name("error").data(json));
        } catch (IOException ignored) {
            // 如果连 error 都发不出去（连接已断），忽略即可
        }
    }

    /** 安静关闭 BufferedReader，忽略 IOException */
    private void closeQuietly(BufferedReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) { }
        }
    }
}
