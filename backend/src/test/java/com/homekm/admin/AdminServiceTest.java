package com.homekm.admin;

import com.homekm.admin.dto.ResetPasswordRequest;
import com.homekm.admin.dto.UpdateUserRequest;
import com.homekm.auth.User;
import com.homekm.auth.UserPrincipal;
import com.homekm.auth.UserRepository;
import com.homekm.common.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    UserRepository userRepository = mock(UserRepository.class);
    AdminService service;

    UserPrincipal adminPrincipal;
    User targetUser;

    @BeforeEach
    void setUp() {
        service = new AdminService(userRepository, new BCryptPasswordEncoder(4));

        adminPrincipal = new UserPrincipal(1L, "admin@example.com", true, false);

        targetUser = new User();
        ReflectionTestUtils.setField(targetUser, "id", 2L);
        targetUser.setEmail("user@example.com");
        targetUser.setDisplayName("User");
        targetUser.setPasswordHash("$2a$04$hash");
        targetUser.setActive(true);
    }

    @Test
    void deleteUser_selfDelete_throwsBadRequest() {
        assertThatThrownBy(() -> service.deleteUser(1L, adminPrincipal))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("CANNOT_DELETE_SELF");
    }

    @Test
    void deleteUser_otherUser_softDeletes() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        service.deleteUser(2L, adminPrincipal);

        assertThat(targetUser.isActive()).isFalse();
        verify(userRepository).save(targetUser);
    }

    @Test
    void deleteUser_nonExistent_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteUser(99L, adminPrincipal))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateUser_removeOwnAdmin_throwsBadRequest() {
        var req = new UpdateUserRequest(null, false, null, null);

        assertThatThrownBy(() -> service.updateUser(1L, req, adminPrincipal))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("CANNOT_REMOVE_OWN_ADMIN");
    }

    @Test
    void updateUser_adminAndChildFlag_throwsBadRequest() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        var req = new UpdateUserRequest(null, true, true, null);

        assertThatThrownBy(() -> service.updateUser(2L, req, adminPrincipal))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void resetPassword_self_throwsBadRequest() {
        var req = new ResetPasswordRequest("newpass");

        assertThatThrownBy(() -> service.resetPassword(1L, req, adminPrincipal))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("PUT /api/auth/me");
    }

    @Test
    void resetPassword_otherUser_updatesHash() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        var req = new ResetPasswordRequest("NewPassword1");

        service.resetPassword(2L, req, adminPrincipal);

        assertThat(targetUser.getPasswordHash()).isNotEqualTo("$2a$04$hash");
        verify(userRepository).save(targetUser);
    }
}
