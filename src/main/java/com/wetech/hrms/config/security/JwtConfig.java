package com.wetech.hrms.config.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.wetech.hrms.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Access token là JWT ký HS512 bằng {@code app.security.jwt.secret}; cùng khoá dùng để ký và xác thực.
 */
@Configuration(proxyBeanMethods = false)
public class JwtConfig {

	public static final MacAlgorithm ALGORITHM = MacAlgorithm.HS512;

	private static final int MIN_SECRET_BYTES = 64;

	@Bean
	JwtEncoder jwtEncoder(AppProperties appProperties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(appProperties)));
	}

	@Bean
	JwtDecoder jwtDecoder(AppProperties appProperties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(appProperties))
				.macAlgorithm(ALGORITHM)
				.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(appProperties.security().jwt().issuer()));
		return decoder;
	}

	/**
	 * Claim {@code roles: ["ADMIN"]} → authority {@code ROLE_ADMIN}; principal name là claim {@code username}.
	 */
	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName(JwtClaimNames.ROLES);
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		converter.setPrincipalClaimName(JwtClaimNames.USERNAME);
		return converter;
	}

	private static SecretKey secretKey(AppProperties appProperties) {
		String secret = appProperties.security().jwt().secret();
		byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"app.security.jwt.secret (JWT_SECRET) phải dài tối thiểu %d byte cho HS512".formatted(MIN_SECRET_BYTES));
		}
		return new SecretKeySpec(keyBytes, "HmacSHA512");
	}

}
