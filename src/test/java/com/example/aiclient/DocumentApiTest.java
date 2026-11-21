package com.example.aiclient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiclient.entity.UserEntity;
import com.example.aiclient.mapper.UserMapper;
import com.example.aiclient.security.JwtService;
import com.example.aiclient.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M3.5 验收：文档响应 camelCase 归一化；USER 写操作 403，ADMIN 成功。
 * FastAPI 转发用 Mock 替代，避免测试依赖外部服务。
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserMapper userMapper;

    @MockitoBean
    private RagService ragService;

    @Test
    void listNormalizesCamelCaseWithChineseKeyword() throws Exception {
        when(ragService.listDocuments(1, 20, "年假")).thenReturn(fastapiListResponse());

        mockMvc.perform(get("/api/documents")
                        .param("keyword", "年假")
                        .header("Authorization", "Bearer " + token("demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.items[0].docId").value("d1"))
                .andExpect(jsonPath("$.data.items[0].filename").value("制度.pdf"))
                .andExpect(jsonPath("$.data.items[0].originalFilename").value("制度.pdf"))
                .andExpect(jsonPath("$.data.items[0].contentType").value("application/pdf"))
                .andExpect(jsonPath("$.data.items[0].uploadTime").value("2026-09-21T19:12:58"))
                .andExpect(jsonPath("$.data.items[0].chunkCount").value(2))
                .andExpect(jsonPath("$.data.items[0].pageCount").value(2))
                .andExpect(jsonPath("$.data.items[0].size").value(96737))
                .andExpect(jsonPath("$.data.items[0].ext").value("pdf"))
                .andExpect(jsonPath("$.data.items[0].status").value("ready"));
    }

    @Test
    void detailNormalizesCamelCase() throws Exception {
        when(ragService.getDocument("d1")).thenReturn(fastapiDocResponse());

        mockMvc.perform(get("/api/documents/d1")
                        .header("Authorization", "Bearer " + token("demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.docId").value("d1"))
                .andExpect(jsonPath("$.data.originalFilename").value("制度.pdf"))
                .andExpect(jsonPath("$.data.chunkCount").value(2));
    }

    @Test
    void detailNotFoundMapsTo40400() throws Exception {
        when(ragService.getDocument("missing")).thenReturn(Map.of(
                "code", 40401,
                "message", "文档不存在"));

        mockMvc.perform(get("/api/documents/missing")
                        .header("Authorization", "Bearer " + token("demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("文档不存在"));
    }

    @Test
    void userCannotUploadOrDelete() throws Exception {
        String userToken = token("demo");

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1}))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(delete("/api/documents/d1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        verifyNoInteractions(ragService);
    }

    @Test
    void adminCanUploadAndDelete() throws Exception {
        when(ragService.upload(any())).thenReturn(fastapiDocResponse());
        when(ragService.deleteDocument("d1")).thenReturn(Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of("doc_id", "d1", "existed", true)));

        String adminToken = token("admin");

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1}))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.docId").value("d1"))
                .andExpect(jsonPath("$.data.originalFilename").value("制度.pdf"));

        mockMvc.perform(delete("/api/documents/d1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.docId").value("d1"))
                .andExpect(jsonPath("$.data.existed").value(true));
    }

    @Test
    void noTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    private String token(String username) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username));
        return jwtService.generate(user);
    }

    private static Map<String, Object> documentMeta() {
        return Map.of(
                "doc_id", "d1",
                "filename", "制度.pdf",
                "original_filename", "制度.pdf",
                "size", 96737,
                "content_type", "application/pdf",
                "upload_time", "2026-09-21T19:12:58",
                "chunk_count", 2,
                "page_count", 2,
                "ext", "pdf",
                "status", "ready");
    }

    private static Map<String, Object> fastapiDocResponse() {
        return Map.of("code", 200, "message", "success", "data", documentMeta());
    }

    private static Map<String, Object> fastapiListResponse() {
        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "items", List.of(documentMeta()),
                        "total", 1,
                        "page", 1,
                        "page_size", 20));
    }
}