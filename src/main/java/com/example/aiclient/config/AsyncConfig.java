package com.example.aiclient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置。
 *
 * 为什么需要这个：SSE 流式转发是阻塞 IO，如果直接用
 * CompletableFuture.runAsync（默认走 ForkJoinPool.commonPool），
 * 几个并发请求就会把 commonPool 占满，还会拖累 JVM 里其他
 * 用到 commonPool 的代码。必须用专用线程池隔离。
 */
@Configuration
public class AsyncConfig {
    @Bean("sseExecutor")
    public ThreadPoolTaskExecutor sseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(64);
        executor.setThreadNamePrefix("sse-");
        // 队列满时由调用线程执行，避免直接丢弃导致客户端一直挂起
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
