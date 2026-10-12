package com.wetech.hrms.modules.auth.service;

import com.wetech.hrms.config.AppProperties;
import com.wetech.hrms.config.security.JwtClaimNames;
import com.wetech.hrms.config.security.JwtConfig;
import com.wetech.hrms.modules.auth.dto.TokenResponse;
import com.wetech.hrms.modules.auth.entity.RefreshToken;
import com.wetech.hrms.modules.auth.repository.RefreshTokenRepository;
import com.wetech.hrms.modules.user.entity.Role;
import com.wetech.hrms.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Phát hành cặp access token (JWT) + refresh token (chuỗi ngẫu nhiên lưu dạng băm trong DB).
 */
@Service
@RequiredArgsConstructor
public class TokenService {

	private static final String TOKEN_TYPE = "Bearer";
	private static final int REFRESH_TOKEN_BYTES = 48;

	private final JwtEncoder jwtEncoder;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AppProperties appProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	public TokenResponse issueTokens(User user) {
		AppProperties.Jwt jwtProperties = appProperties.security().jwt();
		Instant now = Instant.now();

		List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(jwtProperties.issuer())
				.subject(user.getId().toString())
				.id(UUID.randomUUID().toString())
				.issuedAt(now)
				.expiresAt(now.plus(jwtProperties.accessTokenTtl()))
				.claim(JwtClaimNames.USERNAME, user.getUsername())
				.claim(JwtClaimNames.ROLES, roles)
				.build();
		String accessToken = jwtEncoder
				.encode(JwtEncoderParameters.from(JwsHeader.with(JwtConfig.ALGORITHM).build(), claims))
				.getTokenValue();

		String refreshToken = generateRefreshToken();
		refreshTokenRepository.save(new RefreshToken(user, hash(refreshToken), now.plus(jwtProperties.refreshTokenTtl())));

		return new TokenResponse(accessToken, refreshToken, TOKEN_TYPE,
				jwtProperties.accessTokenTtl().toSeconds(), jwtProperties.refreshTokenTtl().toSeconds());
	}

	public static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 không khả dụng", ex);
		}
	}

	private String generateRefreshToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
