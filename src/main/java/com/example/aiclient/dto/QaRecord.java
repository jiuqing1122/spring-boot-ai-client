package com.example.aiclient.dto;

import lombok.Data;

import java.util.List;

/**
 * 问答记录。sources 只存摘要（chunk 截断 100 字）。
 */
@Data
public class QaRecord {
    private String id;
    private String question;
    private String answer;
    private List<SourceItem> sources;
    private String createTime;
    /** SUCCESS / ERROR / DISCONNECTED */
    private String status;
}
