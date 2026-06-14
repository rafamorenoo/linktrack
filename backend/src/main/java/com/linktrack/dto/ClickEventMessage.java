package com.linktrack.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClickEventMessage(
    UUID urlId,
    String ipAddress,
    String userAgent,
    String referer,
    String deviceType,
    LocalDateTime clickedAt
) {}
