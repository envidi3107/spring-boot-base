package com.wetech.hrms.modules.auth.exception;

import com.wetech.hrms.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng"),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn, vui lòng đăng nhập lại"),
	ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Tài khoản đã bị khoá");

	private final HttpStatus httpStatus;
	private final String message;

	@Override
	public String getCode() {
		return name();
	}

}
