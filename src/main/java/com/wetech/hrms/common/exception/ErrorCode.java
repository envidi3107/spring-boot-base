package com.wetech.hrms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi nghiệp vụ. Mỗi module tự khai báo enum riêng implement interface này
 * (vd. {@code EmployeeErrorCode}) thay vì dồn hết vào {@link CommonErrorCode}.
 */
public interface ErrorCode {

	String getCode();

	HttpStatus getHttpStatus();

	String getMessage();

}
