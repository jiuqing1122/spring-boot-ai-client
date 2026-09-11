package com.example.aiclient.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /** 业务异常：返回 200 + 业务码（与 FastAPI 风格一致） */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, Object>> handleBizException(BizException ex) {
        log.warn("业务异常 | code={} | message={}", ex.getCode(), ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        body.put("data", null);
        return ResponseEntity.ok(body);
    }

    /** 404：请求路径不存在（如浏览器自动请求 /favicon.ico） */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(NoHandlerFoundException ex) {
        log.debug("路径不存在 | method={} | path={}", ex.getHttpMethod(), ex.getRequestURL());
        Map<String, Object> body = new HashMap<>();
        body.put("code", 404);
        body.put("message", "Not Found");
        body.put("data", null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /** 系统异常：返回 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("系统异常", e);
        Map<String, Object> body = new HashMap<>();
        body.put("code", 500);
        body.put("message", "Internal Server Error");
        body.put("data", null);
        // 构造 HTTP 500 响应，将 Map 作为 JSON 返回给调用方
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}