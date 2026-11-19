package com.example.aiclient.dto;

import java.util.List;

/**
 * 统一分页响应体。
 */
public record PageResult<T>(List<T> items, long total, long page, long pageSize) {
}