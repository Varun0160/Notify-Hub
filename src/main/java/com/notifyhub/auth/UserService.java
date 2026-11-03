package com.notifyhub.auth;

import com.notifyhub.auth.dto.CreateUserRequest;
import com.notifyhub.auth.dto.UserResponse;
import com.notifyhub.common.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User already exists: " + email);
        }
        User user = new User(email, passwordEncoder.encode(request.password()),
                request.fullName().trim(), request.role());
        return UserResponse.from(userRepository.save(user));
    }
}