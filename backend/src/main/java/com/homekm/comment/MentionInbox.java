package com.homekm.comment;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mention_inbox")
public class MentionInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Comment getComment() { return comment; }
    public void setComment(Comment v) { this.comment = v; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant v) { this.readAt = v; }
    public Instant getCreatedAt() { return createdAt; }
}
