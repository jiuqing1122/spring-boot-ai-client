package com.example.aiclient.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问答记录（历史记录接口，camelCase；id 转字符串）。
 */
public record QaRecordVO(
        String id,
        String question,
        String answer,
        String status,
        Integer latencyMs,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime,
        List<QaSourceVO> sources) {
}