package com.wetech.hrms.common.exception;

import lombok.Getter;

/**
 * Exception nghiệp vụ duy nhất mà service nên ném ra; được
 * {@link GlobalExceptionHandler} chuyển thành {@code ApiResponse} với HTTP status tương ứng.
 */
@Getter
public class AppException extends RuntimeException {

	private final transient ErrorCode errorCode;

	public AppException(ErrorCode errorCode) {
		this(errorCode, errorCode.getMessage());
	}

	public AppException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public AppException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public static AppException notFound(String resource, Object id) {
		return new AppException(CommonErrorCode.RESOURCE_NOT_FOUND,
				"Không tìm thấy %s với id: %s".formatted(resource, id));
	}

}
