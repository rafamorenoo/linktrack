package com.linktrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RefreshToken() {}

    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public String getToken() { return token; }
    public void setToken(String v) { this.token = v; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime v) { this.expiresAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final RefreshToken t = new RefreshToken();
        public Builder user(User v) { t.user = v; return this; }
        public Builder token(String v) { t.token = v; return this; }
        public Builder expiresAt(LocalDateTime v) { t.expiresAt = v; return this; }
        public RefreshToken build() { return t; }
    }
}
