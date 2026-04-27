package com.homekm.audit;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        Long actorUserId,
        String action,
        String targetType,
        String targetId,
        String ip,
        Instant occurredAt
) {
    public static AuditEventResponse from(AuditEvent e) {
        return new AuditEventResponse(
                e.getId(),
                e.getActorUserId(),
                e.getAction(),
                e.getTargetType(),
                e.getTargetId(),
                e.getIp(),
                e.getOccurredAt()
        );
    }
}
