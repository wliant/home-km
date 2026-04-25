package com.homekm.tag.dto;

import com.homekm.tag.Tag;

import java.time.Instant;

public record TagResponse(long id, String name, String color, Instant createdAt) {
    public static TagResponse from(Tag t) {
        return new TagResponse(t.getId(), t.getName(), t.getColor(), t.getCreatedAt());
    }
}
