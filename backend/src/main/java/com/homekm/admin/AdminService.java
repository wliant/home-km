package com.homekm.admin;

import com.homekm.admin.dto.CreateUserRequest;
import com.homekm.admin.dto.ResetPasswordRequest;
import com.homekm.admin.dto.UpdateUserRequest;
import com.homekm.auth.User;
import com.homekm.auth.UserPrincipal;
import com.homekm.auth.UserRepository;
import com.homekm.auth.dto.UserResponse;
import com.homekm.common.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        if (req.isAdmin() && req.isChild()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
        }
        User user = new User();
        user.setEmail(req.email().toLowerCase());
        user.setDisplayName(req.displayName());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setAdmin(req.isAdmin());
        user.setChild(req.isChild());
        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(long id, UpdateUserRequest req, UserPrincipal actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));

        if (req.isAdmin() != null && !req.isAdmin() && actor.getId() == id) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CANNOT_REMOVE_OWN_ADMIN");
        }

        boolean newAdmin = req.isAdmin() != null ? req.isAdmin() : user.isAdmin();
        boolean newChild = req.isChild() != null ? req.isChild() : user.isChild();
        if (newAdmin && newChild) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        }

        if (req.displayName() != null) user.setDisplayName(req.displayName());
        if (req.isAdmin() != null) user.setAdmin(req.isAdmin());
        if (req.isChild() != null) user.setChild(req.isChild());
        if (req.isActive() != null) user.setActive(req.isActive());

        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(long id, UserPrincipal actor) {
        if (actor.getId() == id) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CANNOT_DELETE_SELF");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(long id, ResetPasswordRequest req, UserPrincipal actor) {
        if (actor.getId() == id) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use PUT /api/auth/me to change your own password");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    /**
     * Bulk-create accounts. Each successful row gets a randomly-generated
     * placeholder password — the user is expected to follow up with a
     * standard password-reset (admin can copy the email address from the
     * UI and trigger /api/auth/password-reset/request, or the operator can
     * dispatch one of the documented invitation flows). Duplicates are
     * skipped without aborting the batch so a CSV typo doesn't lose the
     * other rows.
     */
    @Transactional
    public AdminController.BulkImportResult bulkImport(List<AdminController.BulkRow> rows) {
        int created = 0;
        int skipped = 0;
        java.util.List<AdminController.BulkImportError> errors = new java.util.ArrayList<>();
        for (AdminController.BulkRow row : rows) {
            String email = row.email().toLowerCase().trim();
            if (row.isAdmin() && row.isChild()) {
                errors.add(new AdminController.BulkImportError(email, "isAdmin and isChild cannot both be true"));
                skipped++;
                continue;
            }
            if (userRepository.existsByEmail(email)) {
                errors.add(new AdminController.BulkImportError(email, "already exists"));
                skipped++;
                continue;
            }
            User user = new User();
            user.setEmail(email);
            user.setDisplayName(row.displayName());
            // Placeholder hash — user must reset before sign-in works.
            user.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            user.setAdmin(row.isAdmin());
            user.setChild(row.isChild());
            user.setActive(false);
            userRepository.save(user);
            created++;
        }
        return new AdminController.BulkImportResult(created, skipped, errors);
    }
}
