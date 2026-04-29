package com.homekm.note;

import com.homekm.auth.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "note_revisions")
public class NoteRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 50)
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by", nullable = false)
    private User editedBy;

    @Column(name = "edited_at", nullable = false, updatable = false)
    private Instant editedAt = Instant.now();

    public Long getId() { return id; }
    public Note getNote() { return note; }
    public void setNote(Note v) { this.note = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public User getEditedBy() { return editedBy; }
    public void setEditedBy(User v) { this.editedBy = v; }
    public Instant getEditedAt() { return editedAt; }
}
