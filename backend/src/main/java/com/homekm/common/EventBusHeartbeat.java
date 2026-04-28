package com.homekm.common;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventBusHeartbeat {

    private final EventBus bus;

    public EventBusHeartbeat(EventBus bus) {
        this.bus = bus;
    }

    @Scheduled(fixedRate = 30_000L)
    public void heartbeat() {
        bus.heartbeat();
    }
}
