package com.example.aiclient.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aiclient.dto.PageResult;
import com.example.aiclient.dto.QaRecordVO;
import com.example.aiclient.dto.QaSourceVO;
import com.example.aiclient.dto.SourceItem;
import com.example.aiclient.entity.QaRecordEntity;
import com.example.aiclient.entity.QaSourceEntity;
import com.example.aiclient.exception.BizException;
import com.example.aiclient.mapper.QaRecordMapper;
import com.example.aiclient.mapper.QaSourceMapper;
import com.example.aiclient.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 问答记录持久化、查询与软删除。
 * 落库放在独立的 @Transactional 方法里，调用方（RagService）在 LLM/SSE 期间不持有事务。
 */
@Service
@RequiredArgsConstructor
public class QaRecordService {

    private static final int MAX_PAGE_SIZE = 100;

    private final QaRecordMapper qaRecordMapper;
    private final QaSourceMapper qaSourceMapper;

    @Transactional
    public void save(Long userId, String question, String answer, String status,
                     Integer latencyMs, List<SourceItem> sources) {
        QaRecordEntity record = new QaRecordEntity();
        record.setUserId(userId);
        record.setQuestion(cut(question, 1000));
        record.setAnswer(answer);
        record.setStatus(status);
        record.setLatencyMs(latencyMs);
        qaRecordMapper.insert(record);

        if (sources == null || sources.isEmpty()) {
            return;
        }
        int rank = 1;
        for (SourceItem source : sources) {
            qaSourceMapper.insert(toSourceEntity(record.getId(), source, rank++));
        }
    }

    public PageResult<QaRecordVO> list(int page, int pageSize, String keyword,
                                       String status, String docId, LoginUser user) {
        long current = Math.max(page, 1);
        long size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        LambdaQueryWrapper<QaRecordEntity> wrapper = new LambdaQueryWrapper<>();
        // USER 只看自己的；deleted=0 由 @TableLogic 自动追加
        if (!isAdmin(user)) {
            wrapper.eq(QaRecordEntity::getUserId, user.id());
        }
        wrapper.eq(hasText(status), QaRecordEntity::getStatus, status);
        wrapper.like(hasText(keyword), QaRecordEntity::getQuestion, keyword);
        if (hasText(docId)) {
            wrapper.exists(true,
                    "SELECT 1 FROM qa_source WHERE qa_record_id = qa_record.id AND doc_id = {0}",
                    docId);
        }
        wrapper.orderByDesc(QaRecordEntity::getCreatedAt);

        Page<QaRecordEntity> result = qaRecordMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(toVOs(result.getRecords()), result.getTotal(), current, size);
    }

    public QaRecordVO detail(Long id, LoginUser user) {
        QaRecordEntity record = requireVisible(id, user);
        List<QaSourceEntity> sources = qaSourceMapper.selectList(
                new LambdaQueryWrapper<QaSourceEntity>()
                        .eq(QaSourceEntity::getQaRecordId, id)
                        .orderByAsc(QaSourceEntity::getRankNo));
        return toVO(record, sources);
    }

    @Transactional
    public void delete(Long id, LoginUser user) {
        requireVisible(id, user);
        // @TableLogic：生成 UPDATE qa_record SET deleted=1，不物理删除
        qaRecordMapper.deleteById(id);
    }

    private QaRecordEntity requireVisible(Long id, LoginUser user) {
        QaRecordEntity record = qaRecordMapper.selectById(id);
        if (record == null) {
            throw new BizException(40400, "记录不存在");
        }
        if (!isAdmin(user) && !Objects.equals(record.getUserId(), user.id())) {
            throw new BizException(40300, "无权限");
        }
        return record;
    }

    private List<QaRecordVO> toVOs(List<QaRecordEntity> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> ids = records.stream().map(QaRecordEntity::getId).toList();
        Map<Long, List<QaSourceEntity>> byRecord = qaSourceMapper.selectList(
                        new LambdaQueryWrapper<QaSourceEntity>()
                                .in(QaSourceEntity::getQaRecordId, ids)
                                .orderByAsc(QaSourceEntity::getRankNo))
                .stream()
                .collect(Collectors.groupingBy(QaSourceEntity::getQaRecordId));

        return records.stream()
                .map(r -> toVO(r, byRecord.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    private QaRecordVO toVO(QaRecordEntity record, List<QaSourceEntity> sources) {
        List<QaSourceVO> sourceVOs = sources.stream()
                .map(s -> new QaSourceVO(s.getDocId(), s.getSourceFile(), s.getPage(),
                        s.getChunkText(), s.getSimilarity(), s.getRankNo()))
                .toList();
        return new QaRecordVO(
                String.valueOf(record.getId()),
                record.getQuestion(),
                record.getAnswer(),
                record.getStatus(),
                record.getLatencyMs(),
                record.getCreatedAt(),
                sourceVOs);
    }

    private QaSourceEntity toSourceEntity(Long recordId, SourceItem source, int rank) {
        Map<String, Object> metadata = source.getMetadata();
        QaSourceEntity entity = new QaSourceEntity();
        entity.setQaRecordId(recordId);
        entity.setDocId(cut(source.getDocId(), 64));
        entity.setSourceFile(cut(metadata == null ? null : Objects.toString(metadata.get("source"), ""), 255));
        entity.setPage(metadata == null ? 0 : toInt(metadata.get("page")));
        entity.setChunkText(cut(source.getChunk(), 100));
        entity.setSimilarity(source.getSimilarity());
        entity.setRankNo(rank);
        return entity;
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 数据库列有长度上限，超长截断，避免写入失败；null 统一成空串（列均为 NOT NULL） */
    private static String cut(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isAdmin(LoginUser user) {
        return "ADMIN".equals(user.role());
    }
}