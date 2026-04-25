package com.homekm.folder.dto;

import com.homekm.folder.Folder;

import java.time.Instant;
import java.util.List;

public record FolderResponse(
        long id,
        Long parentId,
        String name,
        String description,
        long ownerId,
        boolean isChildSafe,
        Instant createdAt,
        Instant updatedAt,
        List<FolderResponse> children
) {
    public static FolderResponse from(Folder f) {
        return new FolderResponse(
                f.getId(),
                f.getParent() != null ? f.getParent().getId() : null,
                f.getName(),
                f.getDescription(),
                f.getOwner().getId(),
                f.isChildSafe(),
                f.getCreatedAt(),
                f.getUpdatedAt(),
                List.of()
        );
    }

    public FolderResponse withChildren(List<FolderResponse> children) {
        return new FolderResponse(id, parentId, name, description, ownerId,
                isChildSafe, createdAt, updatedAt, children);
    }
}
