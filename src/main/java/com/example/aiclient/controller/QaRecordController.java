package com.example.aiclient.controller;

import com.example.aiclient.security.LoginUser;
import com.example.aiclient.service.QaRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 问答记录：USER 仅本人，ADMIN 可看全部；删除为软删除。
 */
@RestController
@RequestMapping("/api/qa-records")
@RequiredArgsConstructor
public class QaRecordController {

    private final QaRecordService qaRecordService;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String docId,
            @AuthenticationPrincipal LoginUser user) {
        return ok(qaRecordService.list(page, pageSize, keyword, status, docId, user));
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id,
                                      @AuthenticationPrincipal LoginUser user) {
        return ok(qaRecordService.detail(id, user));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id,
                                      @AuthenticationPrincipal LoginUser user) {
        qaRecordService.delete(id, user);
        return ok(Map.of("deleted", true));
    }

    private static Map<String, Object> ok(Object data) {
        return Map.of("code", 200, "message", "success", "data", data);
    }
}