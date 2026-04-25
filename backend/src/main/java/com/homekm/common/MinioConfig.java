package com.homekm.common;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    private final AppProperties appProperties;

    public MinioConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public MinioClient minioClient() {
        AppProperties.Minio minio = appProperties.getMinio();
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }
}
