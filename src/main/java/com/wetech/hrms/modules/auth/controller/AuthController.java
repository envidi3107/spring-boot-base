package com.wetech.hrms.modules.auth.controller;

import com.wetech.hrms.common.constant.ApiPaths;
import com.wetech.hrms.common.response.ApiResponse;
import com.wetech.hrms.modules.auth.dto.LoginRequest;
import com.wetech.hrms.modules.auth.dto.RefreshTokenRequest;
import com.wetech.hrms.modules.auth.dto.TokenResponse;
import com.wetech.hrms.modules.auth.service.AuthService;
import com.wetech.hrms.modules.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Đăng nhập, làm mới token, đăng xuất")
@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "Đăng nhập bằng username/password")
	@SecurityRequirements
	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok(authService.login(request));
	}

	@Operation(summary = "Đổi refresh token lấy cặp token mới")
	@SecurityRequirements
	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.ok(authService.refresh(request.refreshToken()));
	}

	@Operation(summary = "Đăng xuất (thu hồi refresh token)")
	@SecurityRequirements
	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request.refreshToken());
		return ApiResponse.ok();
	}

	@Operation(summary = "Thông tin người dùng hiện tại")
	@GetMapping("/me")
	public ApiResponse<UserResponse> me() {
		return ApiResponse.ok(authService.getCurrentUser());
	}

}
