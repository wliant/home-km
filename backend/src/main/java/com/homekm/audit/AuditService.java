package com.homekm.audit;

import com.homekm.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo) {
        this.repo = repo;
    }

    public void record(Long actorUserId, String action, String targetType, String targetId,
                       String beforeState, String afterState, HttpServletRequest request) {
        AuditEvent event = new AuditEvent();
        event.setActorUserId(actorUserId);
        event.setAction(action);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setBeforeState(beforeState);
        event.setAfterState(afterState);
        if (request != null) {
            event.setIp(request.getRemoteAddr());
            event.setUserAgent(request.getHeader("User-Agent"));
        }
        repo.save(event);
        log.debug("Audit: action={} actor={} target={}/{}", action, actorUserId, targetType, targetId);
    }

    public void record(Long actorUserId, String action, String targetType, String targetId) {
        record(actorUserId, action, targetType, targetId, null, null, null);
    }

    public PageResponse<AuditEventResponse> list(Long actorId, String action, Instant from, Instant to,
                                                   int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100));
        var events = repo.findFiltered(actorId, action, from, to, pageable);
        return PageResponse.of(events.map(AuditEventResponse::from));
    }
}
