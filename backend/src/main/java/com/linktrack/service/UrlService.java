package com.linktrack.service;

import com.linktrack.config.AppProperties;
import com.linktrack.dto.request.CreateUrlRequest;
import com.linktrack.dto.response.PageResponse;
import com.linktrack.dto.response.UrlResponse;
import com.linktrack.exception.BadRequestException;
import com.linktrack.exception.ResourceNotFoundException;
import com.linktrack.model.Url;
import com.linktrack.model.User;
import com.linktrack.repository.ClickEventRepository;
import com.linktrack.repository.UrlRepository;
import com.linktrack.util.ShortCodeGenerator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final ClickEventRepository clickEventRepository;
    private final AppProperties appProperties;

    public UrlService(UrlRepository urlRepository,
                      ClickEventRepository clickEventRepository,
                      AppProperties appProperties) {
        this.urlRepository = urlRepository;
        this.clickEventRepository = clickEventRepository;
        this.appProperties = appProperties;
    }

    @Transactional
    public UrlResponse createUrl(CreateUrlRequest req, User user) {
        String code = req.customCode() != null ? req.customCode() : ShortCodeGenerator.generate();
        if (urlRepository.existsByShortCode(code)) {
            throw new BadRequestException("El codigo ya esta en uso");
        }
        var url = Url.builder()
            .shortCode(code)
            .originalUrl(req.originalUrl())
            .title(req.title())
            .user(user)
            .expiresAt(req.expiresAt())
            .build();
        urlRepository.save(url);
        return UrlResponse.from(url, appProperties.baseUrl(), 0);
    }

    // Cachea solo el string de la URL original, no la entidad JPA
    @Cacheable(value = "urls", key = "#shortCode")
    public String resolveToOriginalUrl(String shortCode) {
        var url = urlRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new ResourceNotFoundException("URL no encontrada: " + shortCode));
        if (!url.isActive() || url.isExpired()) {
            throw new BadRequestException("Esta URL esta inactiva o expirada");
        }
        return url.getOriginalUrl();
    }

    // Sin cache, devuelve la entidad para grabar el click
    public Url resolveShortCode(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new ResourceNotFoundException("URL no encontrada: " + shortCode));
    }

    public PageResponse<UrlResponse> getUserUrls(User user, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var urls = urlRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return PageResponse.from(urls.map(u ->
            UrlResponse.from(u, appProperties.baseUrl(), clickEventRepository.countByUrl(u))));
    }

    @CacheEvict(value = "urls", key = "#shortCode")
    @Transactional
    public void deactivateUrl(UUID id, String shortCode, User user) {
        var url = urlRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("URL no encontrada"));
        if (!url.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("No tienes permisos sobre esta URL");
        }
        url.setActive(false);
        urlRepository.save(url);
    }
}
