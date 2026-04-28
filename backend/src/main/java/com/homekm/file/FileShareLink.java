package com.homekm.file;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "file_share_links")
public class FileShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "password_hash", length = 120)
    private String passwordHash;

    @Column(name = "max_downloads")
    private Integer maxDownloads;

    @Column(name = "download_count", nullable = false)
    private int downloadCount;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    void init() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String v) { this.tokenHash = v; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long v) { this.fileId = v; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant v) { this.expiresAt = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public Integer getMaxDownloads() { return maxDownloads; }
    public void setMaxDownloads(Integer v) { this.maxDownloads = v; }
    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int v) { this.downloadCount = v; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long v) { this.createdBy = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant v) { this.revokedAt = v; }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
    public boolean isRevoked() {
        return revokedAt != null;
    }
    public boolean isExhausted() {
        return maxDownloads != null && downloadCount >= maxDownloads;
    }
}
