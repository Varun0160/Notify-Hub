package com.hookrelay.hookrelay.auth;

import com.hookrelay.hookrelay.common.DuplicateResourceException;
import com.hookrelay.hookrelay.common.UnauthorizedException;
import com.hookrelay.hookrelay.security.JwtService;
import com.hookrelay.hookrelay.tenant.Tenant;
import com.hookrelay.hookrelay.tenant.TenantRepository;
import com.hookrelay.hookrelay.user.Role;
import com.hookrelay.hookrelay.user.User;
import com.hookrelay.hookrelay.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(TenantRepository tenantRepository, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email) || tenantRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.companyName().trim());
        tenant.setEmail(email);
        tenant = tenantRepository.save(tenant);

        User user = new User();
        user.setTenant(tenant);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.ADMIN);
        user = userRepository.save(user);

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generate(user), "Bearer", jwtService.expirySeconds());
    }
}