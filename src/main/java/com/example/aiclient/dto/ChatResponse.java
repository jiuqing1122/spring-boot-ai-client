package com.example.aiclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 兼容 FastAPI 两种响应格式：
 * 1. 正常：{"reply": "..."}
 * 2. 业务异常：{"code": 40001, "message": "...", "data": null}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatResponse {
    private String reply;
    private Integer code;
    private String message;
    private Object data;

    public boolean isBizError() {
        return code != null;
    }
}
