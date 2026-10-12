package com.wetech.hrms.config.security;

import com.wetech.hrms.common.exception.ErrorCode;
import com.wetech.hrms.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Ghi lỗi 401/403 xảy ra ở tầng filter (trước khi vào controller) theo cùng định dạng {@link ApiResponse}.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

	private final JsonMapper jsonMapper;

	public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(response.getOutputStream(), ApiResponse.error(errorCode));
	}

}
