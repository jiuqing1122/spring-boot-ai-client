package com.example.aiclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * 问答命中的来源片段
 * JSON里的字段是下划线风格(doc_id)，Java用驼峰命名，靠@JsonProperty注解桥接
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceItem {
    @JsonProperty("doc_id")
    private String docId;

    private String chunk;
    private Double distance;
    private Double similarity;
    private Map<String, Object> metadata;
}
