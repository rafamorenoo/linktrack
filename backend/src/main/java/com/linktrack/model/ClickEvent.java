package com.linktrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "click_events")
public class ClickEvent {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    @Column(name = "ip_address") private String ipAddress;
    @Column(name = "user_agent", columnDefinition = "TEXT") private String userAgent;
    private String referer;
    private String country;
    private String city;
    @Column(name = "device_type") private String deviceType;

    @Column(name = "clicked_at", nullable = false, updatable = false)
    private LocalDateTime clickedAt = LocalDateTime.now();

    public ClickEvent() {}

    public UUID getId() { return id; }
    public Url getUrl() { return url; }
    public void setUrl(Url v) { this.url = v; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String v) { this.ipAddress = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { this.userAgent = v; }
    public String getReferer() { return referer; }
    public void setReferer(String v) { this.referer = v; }
    public String getCountry() { return country; }
    public void setCountry(String v) { this.country = v; }
    public String getCity() { return city; }
    public void setCity(String v) { this.city = v; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String v) { this.deviceType = v; }
    public LocalDateTime getClickedAt() { return clickedAt; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final ClickEvent e = new ClickEvent();
        public Builder url(Url v) { e.url = v; return this; }
        public Builder ipAddress(String v) { e.ipAddress = v; return this; }
        public Builder userAgent(String v) { e.userAgent = v; return this; }
        public Builder referer(String v) { e.referer = v; return this; }
        public Builder deviceType(String v) { e.deviceType = v; return this; }
        public ClickEvent build() { return e; }
    }
}
