package com.homekm.search;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "saved_searches")
public class SavedSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String name;

    /** Serialised JSON capturing q + filter state. Opaque to the backend. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String query;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public Instant getCreatedAt() { return createdAt; }
}
