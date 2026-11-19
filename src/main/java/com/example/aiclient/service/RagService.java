package com.example.aiclient.service;

import com.example.aiclient.constant.FastApiConstant;
import com.example.aiclient.dto.RagChatRequest;
import com.example.aiclient.dto.SourceItem;
import com.example.aiclient.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class RagService {
    private final RestClient restClient;
    private final ThreadPoolTaskExecutor sseExecutor;
    private final ObjectMapper objectMapper;
    private final QaRecordService qaRecordService;

    @Value("${fastapi.base-url}")
    private String fastapiBaseUrl;

    public RagService(RestClient restClient,
                      @Qualifier("sseExecutor") ThreadPoolTaskExecutor sseExecutor,
                      ObjectMapper objectMapper,
                      QaRecordService qaRecordService) {
        this.restClient = restClient;
        this.sseExecutor = sseExecutor;
        this.objectMapper = objectMapper;
        this.qaRecordService = qaRecordService;
    }

    /**
     * 文档管理（直接透传 FastAPI 响应）
     * @param file
     * @return 上传响应
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> upload(MultipartFile file) {
        String url = fastapiBaseUrl + FastApiConstant.RAG_UPLOAD_API_URL;
        log.info("转发上传 | url={} | filename={} | size={}",
                url, file.getOriginalFilename(), file.getSize());

        try {
            byte[] bytes = file.getBytes();
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            //构建上传资源，包含文件名和文件内容
            builder.part("file", new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            }).contentType(MediaType.APPLICATION_OCTET_STREAM);//设置文件内容类型为二进制流

            Map<String, Object> resp = restClient.post()
                    .uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(Map.class);

            return resp == null ? errorMap(50002, "AI 服务返回空响应") : resp;
        } catch (RestClientException | IOException e) {
            log.error("上传转发失败", e);
            throw new BizException(50001, "上传失败：" + e.getMessage());
        }
    }

    /**
     * 文档列表
     * @param page
     * @param pageSize
     * @param keyword
     * @return 文档列表响应
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listDocuments(int page, int pageSize, String keyword) {
        // 用 UriComponentsBuilder 编码 query，避免中文 keyword 拼 URL 出错
        String requestUrl = fastapiBaseUrl + FastApiConstant.RAG_DOCUMENTS_API_URL;
        String url = UriComponentsBuilder
                .fromUriString(requestUrl)
                .queryParam("page", page)
                .queryParam("page_size", pageSize)
                .queryParamIfPresent("keyword",
                        Optional.ofNullable(keyword).filter(s -> !s.isBlank()))
                .toUriString();

        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            log.error("文档列表转发失败", e);
            throw new BizException(50001, "文档列表失败：" + e.getMessage());
        }
    }

    /**
     * 文档详情
     * @param docId
     * @return 文档详情响应
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDocument(String docId) {
        String url = fastapiBaseUrl + FastApiConstant.RAG_DOCUMENTS_API_URL + "/" + docId;
        try {
            return restClient.get().uri(url).retrieve().body(Map.class);
        } catch (RestClientException e) {
            log.error("文档详情转发失败", e);
            throw new BizException(50001, "文档详情失败：" + e.getMessage());
        }
    }

    /**
     * 文档删除
     * @param docId
     * @return 删除响应
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> deleteDocument(String docId) {
        String url = fastapiBaseUrl + FastApiConstant.RAG_DOCUMENTS_API_URL + "/" + docId;
        try {
            return restClient.delete().uri(url).retrieve().body(Map.class);
        } catch (RestClientException e) {
            log.error("文档删除转发失败", e);
            throw new BizException(50001, "文档删除失败：" + e.getMessage());
        }
    }

    /**
     * 非流式问答，保存问答记录
     * @param question
     * @return Map<String, Object>
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> ragChat(String question, Long userId) {
        long start = System.currentTimeMillis();
        String url = fastapiBaseUrl + FastApiConstant.RAG_CHAT_API_URL;
        log.info("转发 RAG 问答 | url={} | question={}", url, question);

        Map<String, Object> resp;
        try {
            resp = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RagChatRequest(question))
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            log.error("调用 FastAPI 失败", e);
            throw new BizException(50001, "调用 AI 服务失败：" + e.getMessage());
        }

        if (resp == null) {
            throw new BizException(50002, "AI 服务返回空响应");
        }

        Object code = resp.get("code");
        if (userId != null && code != null && "200".equals(code.toString())) {
            saveFromNonStreamResponse(userId, question, resp,
                    (int) (System.currentTimeMillis() - start));
        }

        return resp;
    }

    @SuppressWarnings("unchecked")
    private void saveFromNonStreamResponse(Long userId, String question,
                                           Map<String, Object> resp, int latencyMs) {
        try {
            Object dataObject = resp.get("data");
            if (!(dataObject instanceof Map)) {
                return;
            }
            Map<String, Object> data = (Map<String, Object>) dataObject;
            String answer = String.valueOf(data.getOrDefault("answer", ""));

            List<SourceItem> sourceItems = new ArrayList<>();
            Object sourceObject = data.get("sources");
            if (sourceObject instanceof List) {
                for (Object source : (List<?>) sourceObject) {
                    sourceItems.add(objectMapper.convertValue(source, SourceItem.class));
                }
            }
            qaRecordService.save(userId, question, answer, "SUCCESS", latencyMs, sourceItems);
        } catch (Exception e) {
            log.warn("保存非流式问答记录失败 | err={}", e.getMessage());
        }
    }

    /**
     * 流式问答
     * @param question
     * @param emitter
     */
    public void ragChatStream(String question, Long userId, SseEmitter emitter) {
        long start = System.currentTimeMillis();
        String url = fastapiBaseUrl + FastApiConstant.RAG_CHAT_STREAM_API_URL;
        log.info("转发 RAG 流式问答 | url={} | question={}", url, question);

        // 独立线程执行（RestClient 阻塞式，不能占用 Tomcat 请求线程）
        sseExecutor.execute(() -> {
            // 累积容器：答完要落成 QaRecord
            StringBuilder answerBuf = new StringBuilder();
            List<SourceItem> sourcesBuf = new ArrayList<>();
            // 用长度为 1 的数组，便于在 lambda 内修改
            String[] statusHolder = {"SUCCESS"};
            BufferedReader reader = null;

            try {
                InputStream inputStream = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new RagChatRequest(question))
                        .retrieve()
                        .body(InputStream.class);

                if (inputStream == null) {
                    statusHolder[0] = "ERROR";
                    sendSseError(emitter, 50002, "AI 服务返回空响应");
                    emitter.complete();
                    return;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                //因为reader被赋值了两次（null 一次， new 一次），
                //不是 effectively final，所以 lambda 捕获它之前必须复制给 finalReader。
                //effectively final：一个局部变量只要从声明到使用只被赋值一次、之后再没改过，它就是 effectively final
                BufferedReader finalReader = reader;

                // 客户端断开 / 超时 / 异常时关闭底层流，避免 Python 侧生成器一直跑
                emitter.onCompletion(() -> closeQuietly(finalReader));
                emitter.onTimeout(() -> closeQuietly(finalReader));
                emitter.onError((ex) -> closeQuietly(finalReader));

                String line;
                String eventName = null;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("event:")) {
                        eventName = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();

                        // ---- 先累积（必须在清空 eventName 之前） ----
                        if ("sources".equals(eventName)) {
                            sourcesBuf.addAll(parseSources(data));
                        } else if ("message".equals(eventName)) {
                            String content = parseMessageContent(data);
                            if (content != null) {
                                answerBuf.append(content);
                            }
                        } else if ("error".equals(eventName)) {
                            statusHolder[0] = "ERROR";
                        }

                        // ---- 再转发 ----
                        if (eventName != null) {
                            emitter.send(SseEmitter.event().name(eventName).data(data));
                            eventName = null; //配对消费
                        } else {
                            emitter.send(data); //兼容无名事件
                        }
                    }
                }
                emitter.complete(); // 标记完成，避免重复发送
            } catch (IOException e) {
                // 客户端关标签页属正常现象，记 warn
                log.warn("SSE 客户端断开或 IO 异常 | question={} | msg={}", question, e.getMessage());
                statusHolder[0] = "DISCONNECTED";
                closeQuietly(reader);
                emitter.complete();
            } catch (Exception e) {
                log.error("流式调用 FastAPI 失败 | question={}", question, e);
                statusHolder[0] = "ERROR";
                closeQuietly(reader);
                try {
                    sendSseError(emitter, 500, "AI 服务内部错误");
                } catch (Exception ignored) { }
                emitter.complete();
            } finally {
                // 旧 /rag 接口没有登录用户，不落库；新 /api 接口由 Controller 线程传入 userId
                if (userId != null
                        && !(answerBuf.isEmpty() && sourcesBuf.isEmpty()
                        && "SUCCESS".equals(statusHolder[0]))) {
                    try {
                        qaRecordService.save(userId, question, answerBuf.toString(),
                                statusHolder[0],
                                (int) (System.currentTimeMillis() - start),
                                sourcesBuf);
                    } catch (Exception e) {
                        // 落库失败不能影响已经完成的 SSE 流
                        log.warn("保存流式问答记录失败 | err={}", e.getMessage());
                    }
                }
            }
        });
    }

    //=======内部工具=======

    /** 解析 SSE sources 事件的 data JSON → List<SourceItem> */
    private List<SourceItem> parseSources(String dataJson) {
        try {
            JsonNode node = objectMapper.readTree(dataJson);
            JsonNode arr = node.get("sources");
            if (arr == null || !arr.isArray()) {
                return List.of();
            }
            List<SourceItem> sourcesList = new ArrayList<>();
            for (JsonNode n : arr) {
                sourcesList.add(objectMapper.convertValue(n, SourceItem.class));
            }
            return sourcesList;
        } catch (Exception e) {
            log.warn("解析 sources 失败 | json={} | err={}", dataJson, e.getMessage());
            return List.of();
        }
    }

    /** 解析 SSE message 事件的 data JSON → 内容片段 */
    private String parseMessageContent(String dataJson) {
        try {
            JsonNode node = objectMapper.readTree(dataJson);
            JsonNode content = node.get("content");
            return content == null ? null : content.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private void sendSseError(SseEmitter emitter, int code, String message) {
        try {
            // 手工 String.format 拼 JSON 只转义了引号, 换行/反斜杠/制表符等会产出非法 JSON,
            // 客户端解析失败就拿不到错误信息。改用 ObjectMapper, 转义全部交给 Jackson
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "error",
                    "code", code,
                    "message", message));
            emitter.send(SseEmitter.event().name("error").data(json));
        } catch (Exception ignored) { }
    }

    private void closeQuietly(BufferedReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) { }
        }
    }

    private Map<String, Object> errorMap(int code, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", code);
        m.put("message", message);
        m.put("data", null);
        return m;
    }
}
