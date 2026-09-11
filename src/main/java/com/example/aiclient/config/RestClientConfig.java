package com.example.aiclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;


@Configuration
public class RestClientConfig {
    @Value("${fastapi.connect-timeout:5000}")
    private long connectTimeout;

    @Value("${fastapi.read-timeout:60000}")
    private long readTimeout;

    /**
     * 创建 RestTemplate Bean，用于向 FastAPI 后端发送 HTTP 请求
     *
     * @return 配置了连接超时和读取超时的 RestTemplate 实例
     */
    @Bean
    public RestClient fastApiRestClient(RestClient.Builder builder) {
        // 使用 JDK 内置的请求工厂，无需引入第三方 HTTP 库
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int)connectTimeout);
        factory.setReadTimeout((int)readTimeout);

        return builder
                .requestFactory(factory)
                .build();
    }
}