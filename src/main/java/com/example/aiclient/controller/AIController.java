package com.example.aiclient.controller;

import com.example.aiclient.dto.ChatRequest;
import com.example.aiclient.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AIController {
    private final AIService aiService;

    /**
     * 测试接口：调用 FastAPI 的 AI 服务
     * 示例：GET /test-ai?prompt=你好
     */
    @GetMapping("/test-ai")
    public Map<String,Object> testAI(@RequestParam String prompt) {
        String reply = aiService.chat(prompt);
        Map<String, Object> result = new HashMap<>();
        result.put("code",0);
        result.put("message","success");
        result.put("data",reply);

        return result;
    }

    /**
     * 流式接口：SSE 转发 FastAPI 的流式响应
     * 示例：POST /test-ai/stream
     * 请求体：{"prompt":"你好"}
     */
    @PostMapping(value = "/test-ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter testAiStream(@RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L);
        aiService.chatStream(req.getPrompt(), emitter);
        return emitter;
    }
}
