package vn.sevenleven.shop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import vn.sevenleven.shop.service.impl.AuthServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("dan")
                .passwordHash("$2a$hashed")
                .role(Role.CUSTOMER)
                .fullName("Viet Dan")
                .build();

        testUserResponse = new UserResponse(1L, "dan", "Viet Dan", "CUSTOMER");
    }

    // ───────────────────────────────────���──────────────────────
    // login
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: valid credentials — returns token and user info")
    void login_validCredentials_shouldReturnLoginResponse() {
        LoginRequest request = new LoginRequest("dan", "secret123");

        when(userRepository.findByUsername("dan")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken("dan", "CUSTOMER")).thenReturn("mock.jwt.token");
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        LoginResponse result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("mock.jwt.token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isPositive();
        assertThat(result.user().username()).isEqualTo("dan");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: wrong password — throws BadCredentialsException, no token issued")
    void login_wrongPassword_shouldThrowBadCredentialsException() {
        LoginRequest request = new LoginRequest("dan", "wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        verify(jwtUtil, never()).generateToken(anyString(), anyString());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("login: auth passes but user missing in DB — throws ResourceNotFoundException")
    void login_userMissingAfterAuth_shouldThrowResourceNotFoundException() {
        LoginRequest request = new LoginRequest("ghost", "pass");

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(request));

        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("login: authenticate is called with correct username and password")
    void login_shouldPassCorrectCredentialsToAuthManager() {
        LoginRequest request = new LoginRequest("dan", "secret123");
        when(userRepository.findByUsername("dan")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("token");
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        authService.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("dan");
        assertThat(captor.getValue().getCredentials()).isEqualTo("secret123");
    }

    // ──────────────────────────────────────────────────────────
    // register
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: new username — saves user and returns UserResponse")
    void register_newUsername_shouldSaveAndReturnUserResponse() {
        RegisterRequest request = new RegisterRequest("newuser", "pass123", "New User");

        User saved = User.builder()
                .id(2L).username("newuser")
                .passwordHash("$2a$hashed").role(Role.CUSTOMER).fullName("New User")
                .build();
        UserResponse expected = new UserResponse(2L, "newuser", "New User", "CUSTOMER");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(expected);

        UserResponse result = authService.register(request);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.username()).isEqualTo("newuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: duplicate username — throws BusinessException, nothing saved")
    void register_duplicateUsername_shouldThrowBusinessException() {
        RegisterRequest request = new RegisterRequest("dan", "pass123", "Viet Dan");

        when(userRepository.existsByUsername("dan")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("register: password is encoded, not stored in plain text")
    void register_shouldEncodePasswordBeforeSaving() {
        RegisterRequest request = new RegisterRequest("newuser", "plaintext", "Full Name");
        User saved = User.builder().id(3L).username("newuser")
                .passwordHash("$2a$encoded").role(Role.CUSTOMER).build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("plaintext")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(
                new UserResponse(3L, "newuser", null, "CUSTOMER"));

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$encoded");
        assertThat(captor.getValue().getPasswordHash()).doesNotContain("plaintext");
    }

    @Test
    @DisplayName("register: new user is always assigned CUSTOMER role")
    void register_shouldAlwaysAssignCustomerRole() {
        RegisterRequest request = new RegisterRequest("newuser", "pass123", null);
        User saved = User.builder().id(4L).username("newuser")
                .passwordHash("hash").role(Role.CUSTOMER).build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(
                new UserResponse(4L, "newuser", null, "CUSTOMER"));

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.CUSTOMER);
    }
}
