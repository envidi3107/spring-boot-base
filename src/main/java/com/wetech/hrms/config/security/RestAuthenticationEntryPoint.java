package com.wetech.hrms.config.security;

import com.wetech.hrms.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401: thiếu token hoặc token không hợp lệ. Vẫn giữ header {@code WWW-Authenticate} chuẩn RFC 6750.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final BearerTokenAuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();
	private final SecurityErrorResponseWriter errorResponseWriter;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		delegate.commence(request, response, authException);
		errorResponseWriter.write(response, authException instanceof OAuth2AuthenticationException
				? CommonErrorCode.INVALID_TOKEN
				: CommonErrorCode.UNAUTHORIZED);
	}

}
