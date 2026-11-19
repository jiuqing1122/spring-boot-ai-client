package com.example.aiclient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiclient.dto.PageResult;
import com.example.aiclient.dto.QaRecordVO;
import com.example.aiclient.dto.SourceItem;
import com.example.aiclient.entity.UserEntity;
import com.example.aiclient.exception.BizException;
import com.example.aiclient.mapper.UserMapper;
import com.example.aiclient.security.LoginUser;
import com.example.aiclient.service.QaRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * M3 验收：落库、来源映射、USER/ADMIN 可见性、软删除。
 */
@SpringBootTest
@Transactional
class QaRecordServiceTest {

    @Autowired
    private QaRecordService qaRecordService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void saveQueryAndMapping() {
        UserEntity demo = user("demo");
        String keyword = "m3test-" + System.nanoTime();

        qaRecordService.save(demo.getId(), keyword, "答案", "SUCCESS", 1200, List.of(
                source("doc-1", "制度.pdf", 2, "x".repeat(150), 0.71),
                source("doc-2", "手册.pdf", 0, "短片段", null)));

        PageResult<QaRecordVO> result = qaRecordService.list(1, 10, keyword, null, null, login(demo));

        assertThat(result.total()).isEqualTo(1);
        QaRecordVO vo = result.items().get(0);
        assertThat(vo.id()).isNotBlank();
        assertThat(vo.status()).isEqualTo("SUCCESS");
        assertThat(vo.latencyMs()).isEqualTo(1200);
        assertThat(vo.sources()).hasSize(2);

        assertThat(vo.sources().get(0).docId()).isEqualTo("doc-1");
        assertThat(vo.sources().get(0).sourceFile()).isEqualTo("制度.pdf");
        assertThat(vo.sources().get(0).page()).isEqualTo(2);
        assertThat(vo.sources().get(0).chunk()).hasSize(100);
        assertThat(vo.sources().get(0).similarity()).isEqualTo(0.71);
        assertThat(vo.sources().get(0).rank()).isEqualTo(1);

        assertThat(vo.sources().get(1).docId()).isEqualTo("doc-2");
        assertThat(vo.sources().get(1).similarity()).isNull();
        assertThat(vo.sources().get(1).rank()).isEqualTo(2);
    }

    @Test
    void userIsolationAndAdminVisibility() {
        UserEntity demo = user("demo");
        UserEntity admin = user("admin");
        String keyword = "m3iso-" + System.nanoTime();

        qaRecordService.save(demo.getId(), keyword, "a", "SUCCESS", 1, List.of());

        assertThat(qaRecordService.list(1, 10, keyword, null, null, login(demo)).total()).isEqualTo(1);
        assertThat(qaRecordService.list(1, 10, keyword, null, null, login(admin)).total()).isEqualTo(1);

        UserEntity other = newUser("m3other" + System.nanoTime());
        assertThat(qaRecordService.list(1, 10, keyword, null, null, login(other)).total()).isZero();
    }

    @Test
    void otherUserCannotReadOrDelete() {
        UserEntity demo = user("demo");
        String keyword = "m3perm-" + System.nanoTime();
        qaRecordService.save(demo.getId(), keyword, "a", "SUCCESS", 1, List.of());
        String id = qaRecordService.list(1, 10, keyword, null, null, login(demo)).items().get(0).id();
        UserEntity other = newUser("m3perm" + System.nanoTime());

        assertThatThrownBy(() -> qaRecordService.detail(Long.valueOf(id), login(other)))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(40300));
        assertThatThrownBy(() -> qaRecordService.delete(Long.valueOf(id), login(other)))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(40300));
    }

    @Test
    void softDeleteKeepsRow() {
        UserEntity demo = user("demo");
        String keyword = "m3del-" + System.nanoTime();
        qaRecordService.save(demo.getId(), keyword, "a", "SUCCESS", 1, List.of());
        String id = qaRecordService.list(1, 10, keyword, null, null, login(demo)).items().get(0).id();

        qaRecordService.delete(Long.valueOf(id), login(demo));

        assertThat(qaRecordService.list(1, 10, keyword, null, null, login(demo)).total()).isZero();
        assertThatThrownBy(() -> qaRecordService.detail(Long.valueOf(id), login(demo)))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(40400));
        Integer deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM qa_record WHERE id = ?", Integer.class, Long.valueOf(id));
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    void filtersByDocIdAndStatus() {
        UserEntity demo = user("demo");
        String keyword = "m3filter-" + System.nanoTime();
        qaRecordService.save(demo.getId(), keyword + "-a", "a", "SUCCESS", 1,
                List.of(source("doc-abc", "f.pdf", 1, "c", 0.5)));
        qaRecordService.save(demo.getId(), keyword + "-b", "b", "ERROR", 1,
                List.of(source("doc-xyz", "f.pdf", 1, "c", 0.5)));

        assertThat(qaRecordService.list(1, 10, keyword, null, "doc-abc", login(demo)).total())
                .isEqualTo(1);
        assertThat(qaRecordService.list(1, 10, keyword, null, "doc-none", login(demo)).total())
                .isZero();
        assertThat(qaRecordService.list(1, 10, keyword, "ERROR", null, login(demo)).total())
                .isEqualTo(1);
    }

    private UserEntity user(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username));
    }

    private UserEntity newUser(String username) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        user.setNickname("");
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    private static LoginUser login(UserEntity user) {
        return new LoginUser(user.getId(), user.getUsername(), user.getRole());
    }

    private static SourceItem source(String docId, String file, int page,
                                     String chunk, Double similarity) {
        SourceItem source = new SourceItem();
        source.setDocId(docId);
        source.setChunk(chunk);
        source.setSimilarity(similarity);
        source.setMetadata(Map.of("source", file, "page", page, "chunk_index", 0));
        return source;
    }
}