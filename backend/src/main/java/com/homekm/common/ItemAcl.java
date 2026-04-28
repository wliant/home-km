package com.homekm.common;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "item_acls")
public class ItemAcl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_type", nullable = false, length = 16)
    private String itemType;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String role = "VIEWER";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void init() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getItemType() { return itemType; }
    public void setItemType(String v) { this.itemType = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public Instant getCreatedAt() { return createdAt; }
}
