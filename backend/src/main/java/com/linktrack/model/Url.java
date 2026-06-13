package com.linktrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "urls")
public class Url {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "short_code", nullable = false, unique = true)
    private String shortCode;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "url", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClickEvent> clickEvents = new ArrayList<>();

    public Url() {}

    @PreUpdate void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public UUID getId() { return id; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String v) { this.shortCode = v; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String v) { this.originalUrl = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime v) { this.expiresAt = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<ClickEvent> getClickEvents() { return clickEvents; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Url u = new Url();
        public Builder shortCode(String v) { u.shortCode = v; return this; }
        public Builder originalUrl(String v) { u.originalUrl = v; return this; }
        public Builder title(String v) { u.title = v; return this; }
        public Builder user(User v) { u.user = v; return this; }
        public Builder expiresAt(LocalDateTime v) { u.expiresAt = v; return this; }
        public Builder active(boolean v) { u.active = v; return this; }
        public Url build() { return u; }
    }
}
