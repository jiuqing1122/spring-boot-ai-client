package com.example.aiclient.dto;

/**
 * 问答记录来源（历史记录接口，camelCase）。
 */
public record QaSourceVO(
        String docId,
        String sourceFile,
        Integer page,
        String chunk,
        Double similarity,
        Integer rank) {
}