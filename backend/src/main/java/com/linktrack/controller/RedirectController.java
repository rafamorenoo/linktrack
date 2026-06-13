package com.linktrack.controller;

import com.linktrack.service.ClickEventService;
import com.linktrack.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/s")
public class RedirectController {

    private final UrlService urlService;
    private final ClickEventService clickEventService;

    public RedirectController(UrlService urlService, ClickEventService clickEventService) {
        this.urlService = urlService;
        this.clickEventService = clickEventService;
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        String originalUrl = urlService.resolveToOriginalUrl(code);
        var url = urlService.resolveShortCode(code);
        clickEventService.recordClick(url,
            request.getRemoteAddr(),
            request.getHeader("User-Agent"),
            request.getHeader("Referer"));
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(originalUrl))
            .build();
    }
}
