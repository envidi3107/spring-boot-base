package com.wetech.hrms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wetech.hrms.common.exception.ErrorCode;

import java.time.Instant;
import java.util.List;

/**
 * Định dạng response chuẩn cho mọi API (cả thành công lẫn lỗi).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
		boolean success,
		String code,
		String message,
		T data,
		List<FieldViolation> errors,
		Instant timestamp) {

	public static final String SUCCESS_CODE = "SUCCESS";
	private static final String SUCCESS_MESSAGE = "Thành công";

	public static ApiResponse<Void> ok() {
		return ok(null);
	}

	public static <T> ApiResponse<T> ok(T data) {
		return ok(data, SUCCESS_MESSAGE);
	}

	public static <T> ApiResponse<T> ok(T data, String message) {
		return new ApiResponse<>(true, SUCCESS_CODE, message, data, null, Instant.now());
	}

	public static ApiResponse<Void> error(ErrorCode errorCode) {
		return error(errorCode, errorCode.getMessage(), null);
	}

	public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
		return error(errorCode, message, null);
	}

	public static ApiResponse<Void> error(ErrorCode errorCode, String message, List<FieldViolation> errors) {
		return new ApiResponse<>(false, errorCode.getCode(), message, null, errors, Instant.now());
	}

}
