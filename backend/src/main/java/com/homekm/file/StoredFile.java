package com.homekm.file;

import com.homekm.auth.User;
import com.homekm.folder.Folder;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "files")
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 500)
    private String filename;

    @Column(name = "mime_type", nullable = false, length = 127)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "minio_key", nullable = false, unique = true, length = 1000)
    private String minioKey;

    @Column(name = "thumbnail_key", length = 1000)
    private String thumbnailKey;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_child_safe", nullable = false)
    private boolean childSafe = false;

    @Column(name = "client_upload_id", length = 36)
    private String clientUploadId;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Folder getFolder() { return folder; }
    public void setFolder(Folder folder) { this.folder = folder; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getMinioKey() { return minioKey; }
    public void setMinioKey(String minioKey) { this.minioKey = minioKey; }
    public String getThumbnailKey() { return thumbnailKey; }
    public void setThumbnailKey(String thumbnailKey) { this.thumbnailKey = thumbnailKey; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isChildSafe() { return childSafe; }
    public void setChildSafe(boolean childSafe) { this.childSafe = childSafe; }
    public String getClientUploadId() { return clientUploadId; }
    public void setClientUploadId(String clientUploadId) { this.clientUploadId = clientUploadId; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
