package com.homekm.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository repo;

    @InjectMocks
    private AuditService auditService;

    @Test
    void record_savesEventWithCorrectFields() {
        auditService.record(42L, "NOTE_CREATED", "note", "99", null, null, null);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getActorUserId()).isEqualTo(42L);
        assertThat(saved.getAction()).isEqualTo("NOTE_CREATED");
        assertThat(saved.getTargetType()).isEqualTo("note");
        assertThat(saved.getTargetId()).isEqualTo("99");
    }

    @Test
    void record_capturesIpAndUserAgentFromRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Test");

        auditService.record(1L, "AUTH_LOGIN", "user", "1", null, null, request);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getIp()).isEqualTo("192.168.1.100");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0 Test");
    }

    @Test
    void record_handlesNullRequest() {
        // Should not throw NPE
        auditService.record(1L, "AUTH_LOGOUT", "user", "1", null, null, null);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repo).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getIp()).isNull();
        assertThat(saved.getUserAgent()).isNull();
    }
}
