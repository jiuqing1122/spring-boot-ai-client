package com.example.aiclient.service;

import com.example.aiclient.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 二期文档接口：复用 RagService 的 FastAPI 转发，把响应统一成 camelCase（契约 v1.0）。
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final RagService ragService;

    public Map<String, Object> list(int page, int pageSize, String keyword) {
        Map<String, Object> resp = require(ragService.listDocuments(page, pageSize, keyword));
        if (!isSuccess(resp)) {
            return resp;
        }
        Map<String, Object> data = asMap(resp.get("data"));

        List<Map<String, Object>> items = new ArrayList<>();
        if (data.get("items") instanceof List<?> rawItems) {
            for (Object item : rawItems) {
                if (item instanceof Map) {
                    items.add(toDocumentVO(asMap(item)));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", data.get("total"));
        result.put("page", data.get("page"));
        result.put("pageSize", data.get("page_size"));
        return ok(result);
    }

    public Map<String, Object> detail(String docId) {
        Map<String, Object> resp = require(ragService.getDocument(docId));
        if (isSuccess(resp)) {
            return ok(toDocumentVO(asMap(resp.get("data"))));
        }
        // 契约 v1.0 统一用 40400；FastAPI 文档不存在返回 40401
        if ("40401".equals(String.valueOf(resp.get("code")))) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", 40400);
            body.put("message", resp.get("message") == null ? "文档不存在" : resp.get("message"));
            body.put("data", null);
            return body;
        }
        return resp;
    }

    public Map<String, Object> upload(MultipartFile file) {
        Map<String, Object> resp = require(ragService.upload(file));
        return isSuccess(resp) ? ok(toDocumentVO(asMap(resp.get("data")))) : resp;
    }

    public Map<String, Object> delete(String docId) {
        Map<String, Object> resp = require(ragService.deleteDocument(docId));
        if (!isSuccess(resp)) {
            return resp;
        }
        Map<String, Object> data = asMap(resp.get("data"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("docId", data.get("doc_id"));
        result.put("existed", data.get("existed"));
        return ok(result);
    }

    private static Map<String, Object> require(Map<String, Object> resp) {
        if (resp == null) {
            throw new BizException(50002, "AI 服务返回空响应");
        }
        return resp;
    }

    private static boolean isSuccess(Map<String, Object> resp) {
        Object code = resp.get("code");
        return code != null && "200".equals(code.toString());
    }

    private static Map<String, Object> toDocumentVO(Map<String, Object> meta) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("docId", meta.get("doc_id"));
        vo.put("filename", meta.get("filename"));
        vo.put("originalFilename", meta.get("original_filename"));
        vo.put("size", meta.get("size"));
        vo.put("contentType", meta.get("content_type"));
        vo.put("uploadTime", meta.get("upload_time"));
        vo.put("chunkCount", meta.get("chunk_count"));
        vo.put("pageCount", meta.get("page_count"));
        vo.put("ext", meta.get("ext"));
        vo.put("status", meta.get("status"));
        return vo;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    private static Map<String, Object> ok(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 200);
        body.put("message", "success");
        body.put("data", data);
        return body;
    }
}