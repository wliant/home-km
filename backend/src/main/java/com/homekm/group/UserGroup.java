package com.homekm.group;

import com.homekm.auth.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Lightweight household membership list. Built-in groups (Everyone/Adults/Kids)
 * have {@code system=true} and {@code kind} != {@code CUSTOM}; their members are
 * derived dynamically by {@link GroupService} from {@code users.is_child} so they
 * stay accurate as accounts are added or have their child flag flipped.
 */
@Entity
@Table(name = "user_groups")
public class UserGroup {

    public enum Kind { CUSTOM, SYSTEM_EVERYONE, SYSTEM_ADULTS, SYSTEM_KIDS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Kind kind = Kind.CUSTOM;

    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> members = new HashSet<>();

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public Kind getKind() { return kind; }
    public void setKind(Kind v) { this.kind = v; }
    public boolean isSystem() { return system; }
    public void setSystem(boolean v) { this.system = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<User> getMembers() { return members; }
    public void setMembers(Set<User> v) { this.members = v; }
}
