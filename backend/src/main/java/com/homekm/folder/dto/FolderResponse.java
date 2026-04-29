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
        Instant archivedAt,
        String color,
        String icon,
        List<FolderResponse> children,
        /** Ancestor chain from root → this folder. Set only by getById; nullish on tree. */
        List<Crumb> ancestors
) {
    public record Crumb(long id, String name) {}

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
                f.getArchivedAt(),
                f.getColor(),
                f.getIcon(),
                List.of(),
                null
        );
    }

    public FolderResponse withChildren(List<FolderResponse> children) {
        return new FolderResponse(id, parentId, name, description, ownerId,
                isChildSafe, createdAt, updatedAt, archivedAt, color, icon, children, ancestors);
    }

    public FolderResponse withAncestors(List<Crumb> chain) {
        return new FolderResponse(id, parentId, name, description, ownerId,
                isChildSafe, createdAt, updatedAt, archivedAt, color, icon, children, chain);
    }
}
