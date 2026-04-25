package com.homekm.reminder;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ReminderRecipientId implements Serializable {

    private Long reminderId;
    private Long userId;

    public ReminderRecipientId() {}

    public ReminderRecipientId(Long reminderId, Long userId) {
        this.reminderId = reminderId;
        this.userId = userId;
    }

    public Long getReminderId() { return reminderId; }
    public Long getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReminderRecipientId r)) return false;
        return Objects.equals(reminderId, r.reminderId) && Objects.equals(userId, r.userId);
    }

    @Override
    public int hashCode() { return Objects.hash(reminderId, userId); }
}
