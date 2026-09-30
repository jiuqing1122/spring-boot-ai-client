package com.example.aiclient.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    /**
     * 请求体无法解析（JSON 语法错、字段类型不匹配、编码不是 UTF-8 等）。
     * 这是调用方的问题，属于 400 而不是 500 —— 若交给下面的 Exception 兜底，
     * 调用方只会看到一个没有信息量的 500，排查方向会被带偏。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        // getMostSpecificCause() 才是 Jackson 的原始报错；
        // 直接用 ex.getMessage() 会带上一长串 Spring 的包装描述。
        String detail = ex.getMostSpecificCause().getMessage();
        log.warn("请求体解析失败 | {}", detail);

        Map<String, Object> body = new HashMap<>();
        body.put("code", 40000);
        body.put("message", "请求体 JSON 格式错误：" + detail);
        body.put("data", null);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 请求参数类型不匹配（如 ?page=abc 传了非数字）。
     * 同样是调用方的问题，返回 400。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("请求参数类型错误 | name={} | value={}", ex.getName(), ex.getValue());

        Map<String, Object> body = new HashMap<>();
        body.put("code", 40001);
        body.put("message", "请求参数类型错误：" + ex.getName() + " 无法转换为 "
                + (ex.getRequiredType() == null ? "目标类型" : ex.getRequiredType().getSimpleName()));
        body.put("data", null);
        return ResponseEntity.badRequest().body(body);
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