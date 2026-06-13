package com.linktrack.dto.response;

import com.linktrack.model.Url;

import java.time.LocalDateTime;
import java.util.UUID;

public record UrlResponse(
    UUID id,
    String shortCode,
    String originalUrl,
    String title,
    String shortUrl,
    boolean active,
    LocalDateTime expiresAt,
    LocalDateTime createdAt,
    long totalClicks
) {
    public static UrlResponse from(Url url, String baseUrl, long clicks) {
        return new UrlResponse(
            url.getId(), url.getShortCode(), url.getOriginalUrl(),
            url.getTitle(), baseUrl + "/" + url.getShortCode(),
            url.isActive(), url.getExpiresAt(), url.getCreatedAt(), clicks
        );
    }
}
