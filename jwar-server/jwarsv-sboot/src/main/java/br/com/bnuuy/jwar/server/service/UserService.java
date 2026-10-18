package br.com.bnuuy.jwar.server.service;

import br.com.bnuuy.jwar.server.auth.FirebaseAuthService;
import br.com.bnuuy.jwar.server.domain.User;
import br.com.bnuuy.jwar.server.dto.UpdateUserRequest;
import br.com.bnuuy.jwar.server.exception.NotFoundException;
import br.com.bnuuy.jwar.server.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final FirebaseAuthService firebaseAuthService;

    /**
     * Loads or creates the local User row for a verified Firebase identity. Idempotent.
     */
    @Transactional
    public User bootstrap(FirebaseAuthService.VerifiedToken verified) {
        return userRepository.findByFirebaseUid(verified.uid())
            .map(existing -> syncFromVerified(existing, verified))
            .orElseGet(() -> createFromVerified(verified));
    }

    private User createFromVerified(FirebaseAuthService.VerifiedToken verified) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirebaseUid(verified.uid());
        user.setEmail(verified.email() != null ? verified.email() : verified.uid() + "@unknown.local");
        user.setEmailVerified(verified.emailVerified());
        user.setDisplayName(resolveDisplayName(verified));
        user.setProvider(verified.provider() != null ? verified.provider() : "password");
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        User saved = userRepository.save(user);
        log.info("Bootstrapped new user uid={} id={}", saved.getFirebaseUid(), saved.getId());
        return saved;
    }

    private User syncFromVerified(User existing, FirebaseAuthService.VerifiedToken verified) {
        boolean dirty = false;
        if (verified.email() != null && !verified.email().equals(existing.getEmail())) {
            existing.setEmail(verified.email());
            dirty = true;
        }
        if (existing.isEmailVerified() != verified.emailVerified()) {
            existing.setEmailVerified(verified.emailVerified());
            dirty = true;
        }
        if (existing.getDisplayName() == null || existing.getDisplayName().isBlank()) {
            String fallback = resolveDisplayName(verified);
            if (fallback != null) {
                existing.setDisplayName(fallback);
                dirty = true;
            }
        }
        if (dirty) {
            existing.setUpdatedAt(Instant.now());
            existing = userRepository.save(existing);
        }
        return existing;
    }

    private String resolveDisplayName(FirebaseAuthService.VerifiedToken verified) {
        if (verified.name() != null && !verified.name().isBlank()) {
            return verified.name();
        }
        if (verified.email() != null && verified.email().contains("@")) {
            return verified.email().substring(0, verified.email().indexOf('@'));
        }
        return verified.uid();
    }

    @Transactional(readOnly = true)
    public User getById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    @Transactional
    public User updateProfile(UUID userId, UpdateUserRequest request) {
        User user = getById(userId);
        boolean dirty = false;
        if (request.displayName() != null && !request.displayName().equals(user.getDisplayName())) {
            user.setDisplayName(request.displayName());
            dirty = true;
        }
        if (request.photoUrl() != null && !request.photoUrl().equals(user.getPhotoUrl())) {
            user.setPhotoUrl(request.photoUrl());
            dirty = true;
        }
        if (dirty) {
            user.setUpdatedAt(Instant.now());
            user = userRepository.save(user);
            firebaseAuthService.updateUser(user.getFirebaseUid(), user.getDisplayName(), user.getPhotoUrl());
        }
        return user;
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = getById(userId);
        firebaseAuthService.deleteUser(user.getFirebaseUid());
        userRepository.delete(user);
        log.info("Deleted user id={} firebaseUid={}", userId, user.getFirebaseUid());
    }
}
