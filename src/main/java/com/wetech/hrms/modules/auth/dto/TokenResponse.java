package com.wetech.hrms.modules.auth.dto;

/**
 * @param expiresIn        số giây access token còn hiệu lực
 * @param refreshExpiresIn số giây refresh token còn hiệu lực
 */
public record TokenResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresIn,
		long refreshExpiresIn) {
}
