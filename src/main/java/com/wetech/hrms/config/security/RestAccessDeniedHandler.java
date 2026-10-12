package com.wetech.hrms.config.security;

import com.wetech.hrms.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403: đã xác thực nhưng không đủ quyền (theo rule trong {@code SecurityConfig}).
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final BearerTokenAccessDeniedHandler delegate = new BearerTokenAccessDeniedHandler();
	private final SecurityErrorResponseWriter errorResponseWriter;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		delegate.handle(request, response, accessDeniedException);
		errorResponseWriter.write(response, CommonErrorCode.FORBIDDEN);
	}

}
