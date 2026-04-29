package com.homekm.file;

import com.homekm.auth.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "file_versions")
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private StoredFile file;

    @Column(name = "minio_key", nullable = false, length = 1000)
    private String minioKey;

    @Column(nullable = false, length = 500)
    private String filename;

    @Column(name = "mime_type", nullable = false, length = 127)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    /** Exactly one row per file_id is is_current=true (enforced by partial UQ). */
    @Column(name = "is_current", nullable = false)
    private boolean current = false;

    public Long getId() { return id; }
    public StoredFile getFile() { return file; }
    public void setFile(StoredFile v) { this.file = v; }
    public String getMinioKey() { return minioKey; }
    public void setMinioKey(String v) { this.minioKey = v; }
    public String getFilename() { return filename; }
    public void setFilename(String v) { this.filename = v; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String v) { this.mimeType = v; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long v) { this.sizeBytes = v; }
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User v) { this.uploadedBy = v; }
    public Instant getUploadedAt() { return uploadedAt; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean v) { this.current = v; }
}
