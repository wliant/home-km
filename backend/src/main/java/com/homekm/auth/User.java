package com.homekm.auth;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "is_admin", nullable = false)
    private boolean admin = false;

    @Column(name = "is_child", nullable = false)
    private boolean child = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "UTC";

    @Column(name = "locale", nullable = false, length = 16)
    private String locale = "en";

    @Column(name = "ics_token", length = 64, unique = true)
    private String icsToken;

    /**
     * Per-user notification routing. Open-ended JSON so future event types
     * (mentions, share invites) can be added without a schema change.
     * Currently: {@code {"reminders": true|false}}. Absent = default true.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notification_prefs", nullable = false, columnDefinition = "jsonb")
    private String notificationPrefs = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
    public boolean isChild() { return child; }
    public void setChild(boolean child) { this.child = child; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String v) { this.timezone = v; }
    public String getLocale() { return locale; }
    public void setLocale(String v) { this.locale = v; }
    public String getIcsToken() { return icsToken; }
    public void setIcsToken(String v) { this.icsToken = v; }
    public String getNotificationPrefs() { return notificationPrefs; }
    public void setNotificationPrefs(String v) { this.notificationPrefs = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
