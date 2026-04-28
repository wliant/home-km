package com.homekm.file.dto;

import com.homekm.file.StoredFile;

import java.time.Instant;

public record FileResponse(
        long id,
        Long folderId,
        long ownerId,
        String filename,
        String mimeType,
        long sizeBytes,
        String description,
        boolean isChildSafe,
        boolean hasThumbnail,
        String downloadUrl,
        String thumbnailUrl,
        String visibility,
        String scanStatus,
        Instant uploadedAt,
        Instant updatedAt
) {
    public static FileResponse from(StoredFile f, String downloadUrl, String thumbnailUrl) {
        return new FileResponse(
                f.getId(),
                f.getFolder() != null ? f.getFolder().getId() : null,
                f.getOwner().getId(),
                f.getFilename(),
                f.getMimeType(),
                f.getSizeBytes(),
                f.getDescription(),
                f.isChildSafe(),
                f.getThumbnailKey() != null,
                downloadUrl,
                thumbnailUrl,
                f.getVisibility(),
                f.getScanStatus(),
                f.getUploadedAt(),
                f.getUpdatedAt()
        );
    }
}
