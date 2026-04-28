package com.homekm.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper;

    public EventBus(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        Subscriber sub = new Subscriber(userId, emitter);
        subscribers.add(sub);
        emitter.onCompletion(() -> subscribers.remove(sub));
        emitter.onTimeout(() -> {
            emitter.complete();
            subscribers.remove(sub);
        });
        emitter.onError(e -> {
            subscribers.remove(sub);
        });
        try {
            emitter.send(SseEmitter.event().name("hello").data(Map.of("at", Instant.now().toString())));
        } catch (IOException e) {
            subscribers.remove(sub);
        }
        return emitter;
    }

    public void publish(Event event) {
        if (subscribers.isEmpty()) return;
        for (Subscriber s : subscribers) {
            if (event.audience() != null && !event.audience().contains(s.userId)) continue;
            try {
                s.emitter.send(SseEmitter.event().name(event.type()).data(event.payload()));
            } catch (IOException | IllegalStateException e) {
                subscribers.remove(s);
            }
        }
    }

    public void heartbeat() {
        for (Subscriber s : subscribers) {
            try {
                s.emitter.send(SseEmitter.event().comment("hb"));
            } catch (Exception e) {
                subscribers.remove(s);
            }
        }
    }

    private record Subscriber(Long userId, SseEmitter emitter) {}

    /** Event with optional audience (null = broadcast). */
    public record Event(String type, Object payload, java.util.Set<Long> audience) {
        public static Event broadcast(String type, Object payload) {
            return new Event(type, payload, null);
        }
        public static Event toUsers(String type, Object payload, java.util.Set<Long> users) {
            return new Event(type, payload, users);
        }
    }
}
