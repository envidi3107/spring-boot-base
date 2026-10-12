package com.wetech.hrms.config.security;

/**
 * Claim tuỳ biến trong access token (ngoài các claim chuẩn {@code sub}, {@code iss}, {@code exp}...).
 * {@code sub} là id người dùng.
 */
public final class JwtClaimNames {

	public static final String USERNAME = "username";
	public static final String ROLES = "roles";

	private JwtClaimNames() {
	}

}
