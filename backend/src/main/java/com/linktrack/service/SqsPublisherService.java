package com.linktrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.linktrack.config.AppProperties;
import com.linktrack.dto.ClickEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class SqsPublisherService {

    private static final Logger log = LoggerFactory.getLogger(SqsPublisherService.class);

    private final SqsClient sqsClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public SqsPublisherService(SqsClient sqsClient, AppProperties appProperties) {
        this.sqsClient = sqsClient;
        this.appProperties = appProperties;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Async
    public void publishClickEvent(ClickEventMessage message) {
        try {
            String body = objectMapper.writeValueAsString(message);
            var request = SendMessageRequest.builder()
                .queueUrl(appProperties.aws().sqs().clickEventsQueue())
                .messageBody(body)
                .build();
            sqsClient.sendMessage(request);
            log.debug("Click event publicado en SQS para URL {}", message.urlId());
        } catch (Exception e) {
            log.error("Error publicando click event en SQS", e);
        }
    }
}
