package com.homekm.common;

public enum Visibility {
    PRIVATE,
    HOUSEHOLD,
    CUSTOM;

    public static Visibility fromDb(String s) {
        if (s == null) return HOUSEHOLD;
        return switch (s.toLowerCase()) {
            case "private" -> PRIVATE;
            case "custom" -> CUSTOM;
            default -> HOUSEHOLD;
        };
    }

    public String dbValue() {
        return name().toLowerCase();
    }
}
