package vn.sevenleven.shop.service;

import vn.sevenleven.shop.dto.request.LoginRequest;
import vn.sevenleven.shop.dto.request.RegisterRequest;
import vn.sevenleven.shop.dto.response.LoginResponse;
import vn.sevenleven.shop.dto.response.UserResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UserResponse register(RegisterRequest request);
}
