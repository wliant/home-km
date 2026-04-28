package com.homekm.file;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "file_transforms")
public class FileTransform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(nullable = false, length = 16)
    private String variant;

    @Column(name = "minio_key", nullable = false, length = 1000)
    private String minioKey;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "mime_type", nullable = false, length = 80)
    private String mimeType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void init() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long v) { this.fileId = v; }
    public String getVariant() { return variant; }
    public void setVariant(String v) { this.variant = v; }
    public String getMinioKey() { return minioKey; }
    public void setMinioKey(String v) { this.minioKey = v; }
    public int getWidth() { return width; }
    public void setWidth(int v) { this.width = v; }
    public int getHeight() { return height; }
    public void setHeight(int v) { this.height = v; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long v) { this.sizeBytes = v; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String v) { this.mimeType = v; }
    public Instant getCreatedAt() { return createdAt; }
}
