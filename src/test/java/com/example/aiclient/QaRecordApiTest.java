package com.example.aiclient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiclient.dto.SourceItem;
import com.example.aiclient.entity.UserEntity;
import com.example.aiclient.mapper.UserMapper;
import com.example.aiclient.security.JwtService;
import com.example.aiclient.security.LoginUser;
import com.example.aiclient.service.QaRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M3 接口契约：camelCase 字段、id 字符串、时间格式、软删除结果。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QaRecordApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QaRecordService qaRecordService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    void listDetailDeleteContract() throws Exception {
        UserEntity demo = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, "demo"));
        String token = jwtService.generate(demo);
        String keyword = "m3api" + System.nanoTime();

        SourceItem source = new SourceItem();
        source.setDocId("doc-9");
        source.setChunk("片段");
        source.setSimilarity(0.71);
        source.setMetadata(Map.of("source", "制度.pdf", "page", 3, "chunk_index", 0));
        qaRecordService.save(demo.getId(), keyword, "答案", "SUCCESS", 321, List.of(source));

        String id = qaRecordService.list(1, 10, keyword, null, null,
                        new LoginUser(demo.getId(), demo.getUsername(), demo.getRole()))
                .items().get(0).id();

        mockMvc.perform(get("/api/qa-records").param("keyword", keyword)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(id))
                .andExpect(jsonPath("$.data.items[0].id").isString())
                .andExpect(jsonPath("$.data.items[0].latencyMs").value(321))
                .andExpect(jsonPath("$.data.items[0].createTime")
                        .value(matchesPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
                .andExpect(jsonPath("$.data.items[0].sources[0].docId").value("doc-9"))
                .andExpect(jsonPath("$.data.items[0].sources[0].sourceFile").value("制度.pdf"))
                .andExpect(jsonPath("$.data.items[0].sources[0].page").value(3))
                .andExpect(jsonPath("$.data.items[0].sources[0].rank").value(1));

        mockMvc.perform(get("/api/qa-records/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.sources[0].similarity").value(0.71));

        mockMvc.perform(delete("/api/qa-records/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deleted").value(true));

        mockMvc.perform(get("/api/qa-records/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void noTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/qa-records"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }
}