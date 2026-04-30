package com.homekm.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homekm.push.PushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;

/**
 * Polls the {@code outbox_events} table and dispatches each ready row to
 * the appropriate sink. Today only one sink is wired
 * ({@code REMINDER_PUSH} → {@link PushService}); future event types
 * (mentions, share invites) hook into the same loop by adding a case to
 * {@link #dispatch}.
 *
 * Dispatch is at-least-once: the row is deleted only after the sink call
 * returns. Failures bump {@code attempts} and push {@code next_attempt_at}
 * out by an exponential backoff. Rows that hit the attempt cap remain
 * in the table for forensic inspection — a future cleanup job (or the
 * RUNBOOK's "Common errors" entry) covers manual triage.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxRepository repository;
    private final PushService pushService;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxRepository repository, PushService pushService, ObjectMapper objectMapper) {
        this.repository = repository;
        this.pushService = pushService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-millis:5000}")
    @Transactional
    public void poll() {
        var ready = repository.findReady(Instant.now(), MAX_ATTEMPTS);
        for (OutboxEvent event : ready) {
            try {
                dispatch(event);
                repository.delete(event);
            } catch (Exception e) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(e.getMessage());
                long backoffSeconds = (long) Math.pow(2, event.getAttempts()) * 30;
                event.setNextAttemptAt(Instant.now().plus(backoffSeconds, ChronoUnit.SECONDS));
                repository.save(event);
                log.warn("Outbox event {} failed (attempt {}): {}",
                        event.getId(), event.getAttempts(), e.getMessage());
            }
        }
    }

    private void dispatch(OutboxEvent event) throws Exception {
        switch (event.getEventType()) {
            case "REMINDER_PUSH" -> {
                Map<String, Object> p = objectMapper.readValue(event.getPayload(), new TypeReference<>() {});
                String title = (String) p.get("title");
                String body = (String) p.get("body");
                String url = (String) p.get("url");
                Long reminderId = p.get("reminderId") != null ? ((Number) p.get("reminderId")).longValue() : null;
                pushService.sendToUsers(Arrays.asList(event.getUserIds()), title, body, url, reminderId);
            }
            default -> throw new IllegalStateException("Unknown event type: " + event.getEventType());
        }
    }
}
