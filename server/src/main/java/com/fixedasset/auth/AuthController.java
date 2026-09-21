package com.fixedasset.auth;

import com.fixedasset.auth.dto.LoginRequest;
import com.fixedasset.auth.dto.LoginResponse;
import com.fixedasset.auth.dto.ProfileUpdateRequest;
import com.fixedasset.auth.dto.UserProfileResponse;
import com.fixedasset.common.model.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // 登录接口公开访问，成功后返回 JWT 和当前用户资料。
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // 当前为无状态 JWT，退出由客户端删除 Token；Redis 黑名单可作为后续增强。
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        return ApiResponse.ok(authService.currentProfile());
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAuthority('profile:update')")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(authService.updateProfile(request));
    }
}
