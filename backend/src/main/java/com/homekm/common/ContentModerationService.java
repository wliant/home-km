package com.homekm.common;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lightweight assistive moderation for the child-safe flag. Goal: relieve
 * parents from individually flagging every benign upload, while leaving
 * anything ambiguous in the parental review queue
 * ({@code gaps/child-safe/parental-review-queue.md}).
 *
 * Today this is a deliberate placeholder: a tiny profanity wordlist for
 * notes and an obvious-keyword check on filenames. NSFW image classification
 * is left as a follow-up that needs an ML sidecar (the gap doc references
 * {@code nsfwjs}). The service shape is final, so swapping in a smarter
 * backend is a single-class change.
 *
 * Returns a {@link Decision} with the recommendation plus the rationale —
 * audit-friendly and lets the UI explain why an item was auto-flagged.
 */
@Service
public class ContentModerationService {

    /**
     * Tiny built-in wordlist. Intentionally short and English-only —
     * operators tweak it via the {@code app.moderation.extra-wordlist}
     * env var (deferred), or accept that this layer is best-effort.
     * Storing it inline avoids shipping a 50KB file we never review.
     */
    private static final Set<String> WORDLIST = Set.of(
            // Mild placeholders only — real deployments should extend.
            "damn", "hell", "asshole", "bullshit", "fuck", "shit", "bitch", "porn"
    );

    private static final Pattern WORD_BOUNDARY = Pattern.compile("\\b(?:" + String.join("|", WORDLIST) + ")\\b",
            Pattern.CASE_INSENSITIVE);

    /** Keywords on filenames that defer to manual review even for image MIMEs. */
    private static final Pattern SUSPICIOUS_FILENAME = Pattern.compile(
            "\\b(?:nude|nsfw|adult|xxx|porn|explicit)\\b", Pattern.CASE_INSENSITIVE);

    public enum Verdict {
        /** Auto-mark child-safe; record the review timestamp. */
        AUTO_SAFE,
        /** Leave child-safe=false; surface in the parental review queue. */
        NEEDS_REVIEW,
        /** Auto-mark adult-only (record review timestamp so the queue is clear). */
        AUTO_ADULT
    }

    public record Decision(Verdict verdict, String reason) {}

    /**
     * Classify a note's title + body. Long-form text gets the wordlist
     * treatment; if anything matches, we flag adult-only outright. Short
     * titles with no hits are presumed safe so the parent isn't drowning
     * in queue items for "Groceries" or "School pickup".
     */
    public Decision classifyNote(String title, String body) {
        String haystack = (title == null ? "" : title) + " " + (body == null ? "" : body);
        if (haystack.length() > 100_000) {
            // Pathological input — flag for review and let a human triage.
            return new Decision(Verdict.NEEDS_REVIEW, "long-content");
        }
        if (WORD_BOUNDARY.matcher(haystack).find()) {
            return new Decision(Verdict.AUTO_ADULT, "matched profanity wordlist");
        }
        return new Decision(Verdict.AUTO_SAFE, "no flagged terms");
    }

    /**
     * Classify a file. v1 is filename + MIME based; the gap doc points at
     * an NSFW image classifier as the long-term plan, but that needs a
     * sidecar service so we defer rather than ship something fake.
     */
    public Decision classifyFile(String filename, String mimeType) {
        if (filename != null && SUSPICIOUS_FILENAME.matcher(filename).find()) {
            return new Decision(Verdict.AUTO_ADULT, "suspicious filename keyword");
        }
        if (mimeType == null) {
            return new Decision(Verdict.NEEDS_REVIEW, "missing MIME");
        }
        // Documents and AV without a content classifier — surface for
        // review. Images with clean filenames pass through pending the
        // future NSFW model; same for audio (rarely sensitive in a
        // household context).
        if (mimeType.startsWith("application/pdf")
                || mimeType.startsWith("application/msword")
                || mimeType.startsWith("application/vnd.")
                || mimeType.startsWith("video/")) {
            return new Decision(Verdict.NEEDS_REVIEW, "document/video without content scan");
        }
        if (mimeType.startsWith("image/") || mimeType.startsWith("audio/") || mimeType.startsWith("text/")) {
            return new Decision(Verdict.AUTO_SAFE, "image/audio/text with clean filename");
        }
        return new Decision(Verdict.NEEDS_REVIEW, "unknown MIME");
    }

    /** Convenience: now() to stamp into {@code child_safe_review_at}. */
    public Instant reviewedAt() {
        return Instant.now();
    }
}
