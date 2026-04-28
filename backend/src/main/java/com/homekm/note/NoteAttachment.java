package com.homekm.note;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "note_attachments")
public class NoteAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void init() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getNoteId() { return noteId; }
    public void setNoteId(Long v) { this.noteId = v; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long v) { this.fileId = v; }
    public int getPosition() { return position; }
    public void setPosition(int v) { this.position = v; }
    public Instant getCreatedAt() { return createdAt; }
}
