package com.example.urlshortener.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "access_logs", uniqueConstraints = @UniqueConstraint(name = "uq_access_logs_url_date", columnNames = {"url_id", "access_date"}))
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_id", nullable = false)
    private Long urlId;

    @Column(name = "access_date", nullable = false)
    private LocalDate accessDate;

    @Column(name = "access_count", nullable = false)
    private long accessCount;

    public AccessLog() {
    }

    public AccessLog(Long urlId, LocalDate accessDate) {
        this.urlId = urlId;
        this.accessDate = accessDate;
        this.accessCount = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUrlId() { return urlId; }
    public void setUrlId(Long urlId) { this.urlId = urlId; }

    public LocalDate getAccessDate() { return accessDate; }
    public void setAccessDate(LocalDate accessDate) { this.accessDate = accessDate; }

    public long getAccessCount() { return accessCount; }
    public void setAccessCount(long accessCount) { this.accessCount = accessCount; }

    @Override
    public String toString() {
        return "AccessLog{" +
                "id=" + id +
                ", urlId=" + urlId +
                ", accessDate=" + accessDate +
                ", accessCount=" + accessCount +
                '}';
    }
}