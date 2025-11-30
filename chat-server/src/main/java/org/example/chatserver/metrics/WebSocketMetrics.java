package org.example.chatserver.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebSocketMetrics {

    private final AtomicInteger activeSessions = new AtomicInteger(0);

    public WebSocketMetrics(MeterRegistry meterRegistry) {
        Gauge.builder("websocket.sessions.active", activeSessions, AtomicInteger::get)
                .description("The number of active WebSocket sessions")
                .register(meterRegistry);
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        activeSessions.incrementAndGet();
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        int currentValue;
        int newValue;
        do {
            currentValue = activeSessions.get();
            newValue = Math.max(0, currentValue - 1);
        } while (!activeSessions.compareAndSet(currentValue, newValue));
    }
}
