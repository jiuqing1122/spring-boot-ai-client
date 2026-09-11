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
}
