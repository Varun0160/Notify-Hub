package com.notifyhub.auth;

import com.notifyhub.auth.dto.LoginRequest;
import com.notifyhub.auth.dto.RefreshRequest;
import com.notifyhub.auth.dto.TokenResponse;
import com.notifyhub.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    public Map<String, String> me(@AuthenticationPrincipal UserPrincipal principal) {
        return Map.of(
                "userId", principal.userPublicId(),
                "email", principal.email(),
                "role", principal.role());
    }
}