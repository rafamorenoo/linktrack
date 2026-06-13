package com.linktrack.controller;

import com.linktrack.dto.request.CreateUrlRequest;
import com.linktrack.dto.response.AnalyticsResponse;
import com.linktrack.dto.response.PageResponse;
import com.linktrack.dto.response.UrlResponse;
import com.linktrack.model.User;
import com.linktrack.service.ClickEventService;
import com.linktrack.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/urls")
public class UrlController {

    private final UrlService urlService;
    private final ClickEventService clickEventService;

    public UrlController(UrlService urlService, ClickEventService clickEventService) {
        this.urlService = urlService;
        this.clickEventService = clickEventService;
    }

    @PostMapping
    public ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest req,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(urlService.createUrl(req, user));
    }

    @GetMapping
    public ResponseEntity<PageResponse<UrlResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(urlService.getUserUrls(user, page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id,
                                            @AuthenticationPrincipal User user) {
        urlService.deactivateUrl(id, null, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<AnalyticsResponse> analytics(@PathVariable UUID id,
                                                        @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(clickEventService.getAnalytics(id, user));
    }
}
