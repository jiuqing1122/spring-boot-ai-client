package com.example.aiclient.controller;

import com.example.aiclient.dto.RagChatRequest;
import com.example.aiclient.security.LoginUser;
import com.example.aiclient.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 二期正式问答接口。userId 在 Controller 线程取出后显式传给 Service，
 * SSE 异步线程不读取 SecurityContext。
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class ApiRagController {

    private final RagService ragService;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody RagChatRequest req,
                                    @AuthenticationPrincipal LoginUser user) {
        return ragService.ragChat(req.getQuestion(), user.id());
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody RagChatRequest req,
                                 @AuthenticationPrincipal LoginUser user) {
        SseEmitter emitter = new SseEmitter(0L);
        ragService.ragChatStream(req.getQuestion(), user.id(), emitter);
        return emitter;
    }
}