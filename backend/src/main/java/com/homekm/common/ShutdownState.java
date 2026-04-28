package com.homekm.common;

import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ShutdownState {

    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    @EventListener
    public void onClose(ContextClosedEvent event) {
        shuttingDown.set(true);
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }
}
