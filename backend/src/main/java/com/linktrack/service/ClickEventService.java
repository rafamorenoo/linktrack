package com.linktrack.service;

import com.linktrack.dto.response.AnalyticsResponse;
import com.linktrack.exception.BadRequestException;
import com.linktrack.exception.ResourceNotFoundException;
import com.linktrack.model.ClickEvent;
import com.linktrack.model.Url;
import com.linktrack.model.User;
import com.linktrack.repository.ClickEventRepository;
import com.linktrack.repository.UrlRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClickEventService {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    public ClickEventService(ClickEventRepository clickEventRepository,
                              UrlRepository urlRepository) {
        this.clickEventRepository = clickEventRepository;
        this.urlRepository = urlRepository;
    }

    @Async
    @Transactional
    public void recordClick(Url url, String ip, String userAgent, String referer) {
        var event = ClickEvent.builder()
            .url(url)
            .ipAddress(ip)
            .userAgent(userAgent)
            .referer(referer)
            .deviceType(detectDevice(userAgent))
            .build();
        clickEventRepository.save(event);
    }

    public AnalyticsResponse getAnalytics(UUID urlId, User user) {
        var url = urlRepository.findById(urlId)
            .orElseThrow(() -> new ResourceNotFoundException("URL no encontrada"));
        if (!url.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("No tienes permisos sobre esta URL");
        }
        long total = clickEventRepository.countByUrl(url);
        var perDay = toMapList(clickEventRepository.countClicksPerDaySince(url, LocalDateTime.now().minusDays(30)), "date", "clicks");
        var byCountry = toMapList(clickEventRepository.countClicksByCountry(url), "country", "clicks");
        var byDevice = toMapList(clickEventRepository.countClicksByDevice(url), "device", "clicks");
        return new AnalyticsResponse(total, perDay, byCountry, byDevice);
    }

    private List<Map<String, Object>> toMapList(List<Object[]> rows, String k1, String k2) {
        return rows.stream()
            .map(r -> Map.<String, Object>of(k1, r[0] != null ? r[0].toString() : "unknown", k2, r[1]))
            .collect(Collectors.toList());
    }

    private String detectDevice(String ua) {
        if (ua == null) return "unknown";
        String lower = ua.toLowerCase();
        if (lower.contains("mobile") || lower.contains("android") || lower.contains("iphone")) return "mobile";
        if (lower.contains("tablet") || lower.contains("ipad")) return "tablet";
        return "desktop";
    }
}
