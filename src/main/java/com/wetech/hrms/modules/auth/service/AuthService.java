package com.wetech.hrms.modules.auth.service;

import com.wetech.hrms.common.exception.AppException;
import com.wetech.hrms.common.exception.CommonErrorCode;
import com.wetech.hrms.common.util.SecurityUtils;
import com.wetech.hrms.modules.auth.dto.LoginRequest;
import com.wetech.hrms.modules.auth.dto.TokenResponse;
import com.wetech.hrms.modules.auth.entity.RefreshToken;
import com.wetech.hrms.modules.auth.exception.AuthErrorCode;
import com.wetech.hrms.modules.auth.repository.RefreshTokenRepository;
import com.wetech.hrms.modules.user.dto.UserResponse;
import com.wetech.hrms.modules.user.entity.User;
import com.wetech.hrms.modules.user.mapper.UserMapper;
import com.wetech.hrms.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final UserMapper userMapper;

	@Transactional
	public TokenResponse login(LoginRequest request) {
		User user = userRepository.findByUsername(request.username())
				.filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
				.orElseThrow(() -> new AppException(AuthErrorCode.INVALID_CREDENTIALS));
		ensureEnabled(user);
		return tokenService.issueTokens(user);
	}

	/**
	 * Đổi refresh token lấy cặp token mới. Token cũ bị thu hồi; nếu một token đã thu hồi bị dùng lại
	 * (dấu hiệu bị đánh cắp) thì thu hồi toàn bộ phiên của người dùng đó.
	 */
	@Transactional(noRollbackFor = AppException.class)
	public TokenResponse refresh(String refreshToken) {
		Instant now = Instant.now();
		RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenService.hash(refreshToken))
				.orElseThrow(() -> new AppException(AuthErrorCode.INVALID_REFRESH_TOKEN));

		if (stored.isRevoked()) {
			UUID userId = stored.getUser().getId();
			log.warn("Refresh token đã thu hồi bị dùng lại, thu hồi toàn bộ phiên của user {}", userId);
			refreshTokenRepository.revokeAllByUserId(userId, now);
			throw new AppException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}
		if (stored.isExpired(now)) {
			throw new AppException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		stored.revoke(now);
		User user = stored.getUser();
		ensureEnabled(user);
		return tokenService.issueTokens(user);
	}

	/**
	 * Thu hồi refresh token. Access token hiện tại vẫn dùng được tới khi hết hạn
	 * ({@code app.security.jwt.access-token-ttl}), client cần tự xoá.
	 */
	@Transactional
	public void logout(String refreshToken) {
		refreshTokenRepository.findByTokenHash(TokenService.hash(refreshToken))
				.ifPresent(token -> token.revoke(Instant.now()));
	}

	@Transactional(readOnly = true)
	public UserResponse getCurrentUser() {
		UUID userId = SecurityUtils.getCurrentUserId()
				.orElseThrow(() -> new AppException(CommonErrorCode.UNAUTHORIZED));
		return userRepository.findWithRolesById(userId)
				.map(userMapper::toResponse)
				.orElseThrow(() -> new AppException(CommonErrorCode.UNAUTHORIZED));
	}

	private static void ensureEnabled(User user) {
		if (!user.isEnabled()) {
			throw new AppException(AuthErrorCode.ACCOUNT_DISABLED);
		}
	}

}
