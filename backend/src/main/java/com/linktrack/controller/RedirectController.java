package com.linktrack.controller;

import com.linktrack.dto.ClickEventMessage;
import com.linktrack.service.SqsPublisherService;
import com.linktrack.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/s")
public class RedirectController {

    private final UrlService urlService;
    private final SqsPublisherService sqsPublisherService;

    public RedirectController(UrlService urlService, SqsPublisherService sqsPublisherService) {
        this.urlService = urlService;
        this.sqsPublisherService = sqsPublisherService;
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        var url = urlService.resolveShortCode(code);
        String originalUrl = urlService.resolveToOriginalUrl(code);

        // Publicar evento en SQS de forma asíncrona — no bloquea el redirect
        sqsPublisherService.publishClickEvent(new ClickEventMessage(
            url.getId(),
            request.getRemoteAddr(),
            request.getHeader("User-Agent"),
            request.getHeader("Referer"),
            detectDevice(request.getHeader("User-Agent")),
            LocalDateTime.now()
        ));

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(originalUrl))
            .build();
    }

    private String detectDevice(String ua) {
        if (ua == null) return "unknown";
        String lower = ua.toLowerCase();
        if (lower.contains("mobile") || lower.contains("android") || lower.contains("iphone")) return "mobile";
        if (lower.contains("tablet") || lower.contains("ipad")) return "tablet";
        return "desktop";
    }
}
