package com.linktrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.linktrack.config.AppProperties;
import com.linktrack.dto.ClickEventMessage;
import com.linktrack.model.ClickEvent;
import com.linktrack.repository.ClickEventRepository;
import com.linktrack.repository.UrlRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SqsConsumerService {

    private static final Logger log = LoggerFactory.getLogger(SqsConsumerService.class);

    private final SqsClient sqsClient;
    private final AppProperties appProperties;
    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private ScheduledExecutorService scheduler;

    public SqsConsumerService(SqsClient sqsClient,
                               AppProperties appProperties,
                               ClickEventRepository clickEventRepository,
                               UrlRepository urlRepository) {
        this.sqsClient = sqsClient;
        this.appProperties = appProperties;
        this.clickEventRepository = clickEventRepository;
        this.urlRepository = urlRepository;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void startPolling() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sqs-consumer");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::poll, 5, 5, TimeUnit.SECONDS);
        log.info("SQS consumer iniciado - cola: {}", appProperties.aws().sqs().clickEventsQueue());
    }

    @PreDestroy
    public void stopPolling() {
        running.set(false);
        if (scheduler != null) scheduler.shutdown();
    }

    private void poll() {
        if (!running.get()) return;
        try {
            var request = ReceiveMessageRequest.builder()
                .queueUrl(appProperties.aws().sqs().clickEventsQueue())
                .maxNumberOfMessages(10)
                .waitTimeSeconds(5)
                .build();
            List<Message> messages = sqsClient.receiveMessage(request).messages();
            for (Message msg : messages) {
                processMessage(msg);
            }
        } catch (Exception e) {
            log.error("Error al leer de SQS", e);
        }
    }

    @Transactional
    protected void processMessage(Message msg) {
        try {
            ClickEventMessage event = objectMapper.readValue(msg.body(), ClickEventMessage.class);
            urlRepository.findById(event.urlId()).ifPresent(url -> {
                var click = new ClickEvent();
                click.setUrl(url);
                click.setIpAddress(event.ipAddress());
                click.setUserAgent(event.userAgent());
                click.setReferer(event.referer());
                click.setDeviceType(event.deviceType());
                clickEventRepository.save(click);
                log.debug("Click persistido para URL {}", event.urlId());
            });
            // Borrar el mensaje de la cola tras procesarlo
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(appProperties.aws().sqs().clickEventsQueue())
                .receiptHandle(msg.receiptHandle())
                .build());
        } catch (Exception e) {
            log.error("Error procesando mensaje SQS: {}", msg.body(), e);
        }
    }
}
