package com.homekm.reminder;

import com.homekm.auth.User;
import jakarta.persistence.*;

@Entity
@Table(name = "reminder_recipients")
public class ReminderRecipient {

    @EmbeddedId
    private ReminderRecipientId id = new ReminderRecipientId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reminderId")
    @JoinColumn(name = "reminder_id")
    private Reminder reminder;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    public ReminderRecipient() {}

    public ReminderRecipient(Reminder reminder, User user) {
        this.reminder = reminder;
        this.user = user;
        this.id = new ReminderRecipientId(reminder.getId(), user.getId());
    }

    public ReminderRecipientId getId() { return id; }
    public Reminder getReminder() { return reminder; }
    public User getUser() { return user; }
}
