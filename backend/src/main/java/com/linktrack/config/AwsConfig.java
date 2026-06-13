package com.linktrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    private final AppProperties appProperties;

    public AwsConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public SqsClient sqsClient() {
        var builder = SqsClient.builder()
            .region(Region.of(appProperties.aws().region()))
            .credentialsProvider(DefaultCredentialsProvider.create());
        String queueUrl = appProperties.aws().sqs().clickEventsQueue();
        if (queueUrl.contains("localhost") || queueUrl.contains("localstack")) {
            builder.endpointOverride(URI.create("http://localhost:4566"));
        }
        return builder.build();
    }
}
