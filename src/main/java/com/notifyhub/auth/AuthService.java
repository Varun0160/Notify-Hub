package com.notifyhub.auth;

import com.notifyhub.auth.dto.LoginRequest;
import com.notifyhub.auth.dto.TokenResponse;
import com.notifyhub.common.exception.InvalidCredentialsException;
import com.notifyhub.security.ApiKeyGenerator;
import com.notifyhub.security.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Slf4j
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties props;
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties props) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.props = props;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        // Always run a BCrypt comparison, even for unknown emails, so response time
        // does not reveal whether an account exists.
        String hashToCheck = (user != null) ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null || !user.isEnabled() || !passwordOk) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        return issueTokens(user);
    }

    // noRollbackFor: when we detect token reuse we revoke everything and THEN throw.
    // Without this, the exception would roll back the revocation we just did.
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(ApiKeyGenerator.hash(rawRefreshToken))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (stored.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}; revoking all sessions",
                    stored.getUser().getPublicId());
            refreshTokenRepository.revokeAllByUserId(stored.getUser().getId());
            throw new InvalidCredentialsException("Invalid refresh token");
        }
        if (stored.getExpiresAt().isBefore(Instant.now()) || !stored.getUser().isEnabled()) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        stored.setRevoked(true);                 // rotation: the old token dies now
        return issueTokens(stored.getUser());    // and a brand-new pair is issued
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(ApiKeyGenerator.hash(rawRefreshToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    private TokenResponse issueTokens(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant expiresAt = Instant.now().plus(props.refreshTtlDays(), ChronoUnit.DAYS);
        refreshTokenRepository.save(new RefreshToken(user, ApiKeyGenerator.hash(rawRefresh), expiresAt));

        return new TokenResponse(
                jwtService.createAccessToken(user),
                rawRefresh,
                "Bearer",
                props.accessTtlMinutes() * 60);
    }
}