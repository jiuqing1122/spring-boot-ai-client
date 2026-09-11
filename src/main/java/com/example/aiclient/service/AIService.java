package com.example.aiclient.service;

import com.example.aiclient.constant.FastApiConstant;
import com.example.aiclient.dto.ChatRequest;
import com.example.aiclient.dto.ChatResponse;
import com.example.aiclient.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIService {
    private final RestClient restClient;

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
     */
    public void chatStream(String prompt, SseEmitter emitter) {
        String apiUrl = fastapiBaseUrl + FastApiConstant.CHAT_STREAM_API_URL;

        log.info("流式调用 FastAPI | url={} | prompt={}", apiUrl, prompt);

        // 独立线程执行（RestClient 是阻塞式，不能占用 Tomcat 请求线程）
        CompletableFuture.runAsync(() -> {
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
        });
    }
}
