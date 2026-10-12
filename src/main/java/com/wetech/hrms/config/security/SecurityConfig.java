package com.wetech.hrms.config.security;

import com.wetech.hrms.common.constant.ApiPaths;
import com.wetech.hrms.common.web.RequestIdFilter;
import com.wetech.hrms.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.stream.Stream;

/**
 * API stateless: client đăng nhập qua {@code /api/v1/auth/login} lấy access token (JWT)
 * rồi gửi kèm header {@code Authorization: Bearer <token>}.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private static final List<String> DEFAULT_PUBLIC_PATHS = List.of(
			"/error",
			"/actuator/health/**",
			"/actuator/info",
			"/v3/api-docs/**",
			"/swagger-ui/**",
			"/swagger-ui.html",
			ApiPaths.V1 + "/auth/login",
			ApiPaths.V1 + "/auth/refresh",
			ApiPaths.V1 + "/auth/logout");

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AppProperties appProperties,
			JwtAuthenticationConverter jwtAuthenticationConverter,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler) throws Exception {
		String[] publicPaths = Stream.concat(DEFAULT_PUBLIC_PATHS.stream(),
				appProperties.security().publicPaths().stream()).toArray(String[]::new);

		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(publicPaths).permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
		AppProperties.Cors cors = appProperties.cors();
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(cors.allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION, RequestIdFilter.REQUEST_ID_HEADER));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(cors.maxAge());

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}
