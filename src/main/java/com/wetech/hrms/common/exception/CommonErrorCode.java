package com.wetech.hrms.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau"),
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
	MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Nội dung request không đúng định dạng"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập"),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức HTTP không được hỗ trợ"),
	CONFLICT(HttpStatus.CONFLICT, "Dữ liệu bị xung đột hoặc đã tồn tại"),
	CONCURRENT_UPDATE(HttpStatus.CONFLICT, "Dữ liệu đã bị người khác thay đổi, vui lòng tải lại và thử lại"),
	PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "Dung lượng request vượt quá giới hạn cho phép"),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Định dạng dữ liệu không được hỗ trợ");

	private final HttpStatus httpStatus;
	private final String message;

	@Override
	public String getCode() {
		return name();
	}

	/**
	 * Mã lỗi chung đầu tiên khớp HTTP status; không có thì quy về BAD_REQUEST / INTERNAL_ERROR.
	 */
	public static CommonErrorCode fromStatus(HttpStatusCode status) {
		return Arrays.stream(values())
				.filter(code -> code.httpStatus.value() == status.value())
				.findFirst()
				.orElse(status.is4xxClientError() ? BAD_REQUEST : INTERNAL_ERROR);
	}

}
