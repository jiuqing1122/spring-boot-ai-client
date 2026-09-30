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

    /** 非流式问答 */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody RagChatRequest req) {
        return ragService.ragChat(req.getQuestion());
    }

    /** 流式问答（SSE） */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody RagChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L);
        ragService.ragChatStream(req.getQuestion(), emitter);
        return emitter;
    }

    /** 问答记录查询 */
    @GetMapping("/qa-records")
    public Map<String, Object> listQaRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String docId,
            @RequestParam(required = false) String keyword) {
        return ragService.listQaRecords(page, pageSize, docId, keyword);
    }
}

