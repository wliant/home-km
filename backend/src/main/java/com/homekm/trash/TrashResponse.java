package com.homekm.trash;

import java.time.Instant;
import java.util.List;

public record TrashResponse(
    List<TrashItem> notes,
    List<TrashItem> files,
    List<TrashItem> folders
) {
    public record TrashItem(Long id, String type, String name, Instant deletedAt) {}
}
