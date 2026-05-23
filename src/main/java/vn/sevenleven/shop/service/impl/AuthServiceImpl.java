package vn.sevenleven.shop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sevenleven.shop.dto.request.LoginRequest;
import vn.sevenleven.shop.dto.request.RegisterRequest;
import vn.sevenleven.shop.dto.response.LoginResponse;
import vn.sevenleven.shop.dto.response.UserResponse;
import vn.sevenleven.shop.entity.User;
import vn.sevenleven.shop.enums.Role;
import vn.sevenleven.shop.exception.BusinessException;
import vn.sevenleven.shop.exception.ResourceNotFoundException;
import vn.sevenleven.shop.mapper.UserMapper;
import vn.sevenleven.shop.repository.UserRepository;
import vn.sevenleven.shop.security.JwtUtil;
import vn.sevenleven.shop.service.AuthService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            log.warn("Login failed for user: {}", request.username());
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        long expiresIn = 86400L;

        log.info("User logged in successfully: {}", request.username());
        return new LoginResponse(token, "Bearer", expiresIn, userMapper.toResponse(user));
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already taken: " + request.username());
        }

        User user = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .fullName(request.fullName())
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getUsername());
        return userMapper.toResponse(saved);
    }
}
