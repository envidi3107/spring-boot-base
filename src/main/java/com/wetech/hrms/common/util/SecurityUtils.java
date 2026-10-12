package com.wetech.hrms.common.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static Optional<Jwt> getCurrentJwt() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			return Optional.of(jwt);
		}
		return Optional.empty();
	}

	/**
	 * ID người dùng hiện tại (claim {@code sub} của access token).
	 */
	public static Optional<UUID> getCurrentUserId() {
		return getCurrentJwt().map(Jwt::getSubject).flatMap(SecurityUtils::parseUuid);
	}

	/**
	 * Tên đăng nhập của người dùng hiện tại.
	 */
	public static Optional<String> getCurrentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}
		return Optional.ofNullable(authentication.getName());
	}

	private static Optional<UUID> parseUuid(String value) {
		try {
			return Optional.of(UUID.fromString(value));
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

}
