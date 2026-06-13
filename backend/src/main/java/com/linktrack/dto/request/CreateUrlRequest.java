package com.linktrack.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

public record CreateUrlRequest(
    @NotBlank @URL String originalUrl,
    @Size(max = 100) String title,
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,10}$", message = "El código debe tener 3-10 caracteres alfanuméricos") String customCode,
    LocalDateTime expiresAt
) {}
