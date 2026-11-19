package com.example.aiclient.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答记录 qa_record。逻辑删除字段 deleted 由 MyBatis-Plus 自动追加条件。
 */
@Data
@TableName("qa_record")
public class QaRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String question;

    private String answer;

    /** SUCCESS / ERROR / DISCONNECTED */
    private String status;

    private Integer latencyMs;

    /** 逻辑删除：0 未删，1 已删 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
}