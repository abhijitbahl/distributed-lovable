package com.projects.distributed_lovable.workspace_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "minio")
@Getter
@Setter
public class StorageConfig {

    private String url;
    private String accessKey;
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("minio.url must be configured");
        }
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("minio.access-key and minio.secret-key must be configured");
        }
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }
}
