package com.example.aiclient.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
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
    /**
     * 用 LocalDateTime 而不是 String：
     * 之前存 Date.toString()（形如 "Wed Sep 30 20:15:17 CST 2026"），
     * 星期几在最前面，按字符串排序会变成"按星期几排"，时间序全乱。
     * 用时间类型排序才是正确的。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /** SUCCESS / ERROR / DISCONNECTED */
    private String status;
}
