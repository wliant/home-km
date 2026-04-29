package com.homekm.note;

import com.homekm.auth.User;
import com.homekm.folder.Folder;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notes")
public class Note {

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
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 50)
    private String label = "custom";

    @Column(name = "is_child_safe", nullable = false)
    private boolean childSafe = false;

    @Column(name = "visibility", nullable = false, length = 16)
    private String visibility = "household";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "pinned_at")
    private Instant pinnedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Timestamp when an admin reviewed the child-safe flag. Null = pending. */
    @Column(name = "child_safe_review_at")
    private Instant childSafeReviewAt;

    @Column(name = "is_template", nullable = false)
    private boolean template = false;

    /**
     * Optimistic-concurrency token. Hibernate increments this on every
     * persisted update; controllers compare the client-supplied value and
     * surface 409 CONFLICT when the row moved underneath the editor.
     * See {@code gaps/sync/conflict-resolution.md}.
     */
    @jakarta.persistence.Version
    @Column(nullable = false)
    private long version = 0;

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Folder getFolder() { return folder; }
    public void setFolder(Folder folder) { this.folder = folder; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isChildSafe() { return childSafe; }
    public void setChildSafe(boolean childSafe) { this.childSafe = childSafe; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPinnedAt() { return pinnedAt; }
    public void setPinnedAt(Instant pinnedAt) { this.pinnedAt = pinnedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public Instant getChildSafeReviewAt() { return childSafeReviewAt; }
    public void setChildSafeReviewAt(Instant v) { this.childSafeReviewAt = v; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String v) { this.visibility = v; }
    public boolean isTemplate() { return template; }
    public void setTemplate(boolean v) { this.template = v; }
    public long getVersion() { return version; }
    public void setVersion(long v) { this.version = v; }
}
