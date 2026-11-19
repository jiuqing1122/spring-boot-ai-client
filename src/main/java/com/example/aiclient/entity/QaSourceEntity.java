package com.example.aiclient.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 问答来源快照 qa_source。
 */
@Data
@TableName("qa_source")
public class QaSourceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long qaRecordId;

    private String docId;

    private String sourceFile;

    private Integer page;

    /** 截断后的来源文本，映射 chunk_text */
    private String chunkText;

    /** BM25 独有命中时可能为空 */
    private Double similarity;

    /** 来源顺序，1 起 */
    private Integer rankNo;
}