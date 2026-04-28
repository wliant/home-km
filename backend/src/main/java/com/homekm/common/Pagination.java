package com.homekm.common;

/**
 * Centralised pagination bounds for list endpoints. Controllers clamp the
 * client-supplied {@code size} to {@link #MAX_SIZE} so a malicious or buggy
 * client cannot trigger an N=ALL scan.
 *
 * <p>Page index ({@code page}) is similarly normalised: negative values
 * collapse to {@code 0}.
 */
public final class Pagination {

    /** Hard ceiling on a single page request. */
    public static final int MAX_SIZE = 100;

    /** Floor: a non-positive size collapses to this. */
    public static final int DEFAULT_SIZE = 20;

    private Pagination() {}

    public static int clampSize(int requested) {
        if (requested <= 0) return DEFAULT_SIZE;
        return Math.min(requested, MAX_SIZE);
    }

    public static int clampPage(int requested) {
        return Math.max(requested, 0);
    }
}
