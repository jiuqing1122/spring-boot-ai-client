package com.example.aiclient.controller;

import com.example.aiclient.dto.RagChatRequest;
import com.example.aiclient.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {
    private final RagService ragService;

    /**
     * 上传文档
     */
    @PostMapping("/documents/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        return ragService.upload(file);
    }

    /** 文档列表 */
    @GetMapping("/documents")
    public Map<String, Object> listDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ragService.listDocuments(page, pageSize, keyword);
    }

    /** 文档详情 */
    @GetMapping("/documents/{docId}")
    public Map<String, Object> getDocument(@PathVariable String docId) {
        return ragService.getDocument(docId);
    }

    /** 删除文档 */
    @DeleteMapping("/documents/{docId}")
    public Map<String, Object> deleteDocument(@PathVariable String docId) {
        return ragService.deleteDocument(docId);
    }

    /** 非流式问答（旧接口无登录用户，不落库；正式前端用 /api/rag/chat） */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody RagChatRequest req) {
        return ragService.ragChat(req.getQuestion(), null);
    }

    /** 流式问答（SSE，旧接口不落库） */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody RagChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L);
        ragService.ragChatStream(req.getQuestion(), null, emitter);
        return emitter;
    }
}

