package com.example.aiclient.controller;

import com.example.aiclient.dto.ChatResponse;
import com.example.aiclient.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
