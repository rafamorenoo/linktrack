package com.linktrack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl, Aws aws) {
    public record Aws(String region, Sqs sqs) {
        public record Sqs(String clickEventsQueue) {}
    }
}
