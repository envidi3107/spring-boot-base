package com.wetech.hrms.common.exception;

import com.wetech.hrms.common.response.ApiResponse;
import com.wetech.hrms.common.response.FieldViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AppException.class)
	public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
		ErrorCode errorCode = ex.getErrorCode();
		if (errorCode.getHttpStatus().is5xxServerError()) {
			log.error("Application error [{}]: {}", errorCode.getCode(), ex.getMessage(), ex);
		} else {
			log.debug("Application error [{}]: {}", errorCode.getCode(), ex.getMessage());
		}
		return build(errorCode, ex.getMessage(), null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		List<FieldViolation> errors = new ArrayList<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.add(new FieldViolation(error.getField(), error.getDefaultMessage())));
		ex.getBindingResult().getGlobalErrors()
				.forEach(error -> errors.add(new FieldViolation(error.getObjectName(), error.getDefaultMessage())));
		return validationError(errors);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
		List<FieldViolation> errors = new ArrayList<>();
		ex.getParameterValidationResults().forEach(result -> {
			String parameter = result.getMethodParameter().getParameterName();
			result.getResolvableErrors().forEach(error -> errors.add(new FieldViolation(
					error instanceof FieldError fieldError ? fieldError.getField() : parameter,
					error.getDefaultMessage())));
		});
		return validationError(errors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
		List<FieldViolation> errors = ex.getConstraintViolations().stream()
				.map(violation -> new FieldViolation(violation.getPropertyPath().toString(), violation.getMessage()))
				.toList();
		return validationError(errors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
		log.debug("Malformed request body: {}", ex.getMessage());
		return build(CommonErrorCode.MALFORMED_REQUEST, CommonErrorCode.MALFORMED_REQUEST.getMessage(), null);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return build(CommonErrorCode.BAD_REQUEST, "Tham số '%s' không hợp lệ".formatted(ex.getName()), null);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
		return build(CommonErrorCode.BAD_REQUEST,
				"Thiếu tham số bắt buộc '%s'".formatted(ex.getParameterName()), null);
	}

	@ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
	public ResponseEntity<ApiResponse<Void>> handleNoResource(Exception ex) {
		return build(CommonErrorCode.RESOURCE_NOT_FOUND, CommonErrorCode.RESOURCE_NOT_FOUND.getMessage(), null);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		return build(CommonErrorCode.METHOD_NOT_ALLOWED, CommonErrorCode.METHOD_NOT_ALLOWED.getMessage(), null);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
		return build(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE, CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage(), null);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
		return build(CommonErrorCode.PAYLOAD_TOO_LARGE, CommonErrorCode.PAYLOAD_TOO_LARGE.getMessage(), null);
	}

	/**
	 * Lỗi từ {@code @PreAuthorize}... ném ra trong controller/service. Phải bắt riêng,
	 * nếu không sẽ rơi xuống handler {@link Exception} và trả về 500.
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
		return build(CommonErrorCode.FORBIDDEN, CommonErrorCode.FORBIDDEN.getMessage(), null);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
		return build(CommonErrorCode.UNAUTHORIZED, CommonErrorCode.UNAUTHORIZED.getMessage(), null);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockingFailureException ex) {
		log.warn("Optimistic locking failure: {}", ex.getMessage());
		return build(CommonErrorCode.CONCURRENT_UPDATE, CommonErrorCode.CONCURRENT_UPDATE.getMessage(), null);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
		log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
		return build(CommonErrorCode.CONFLICT, CommonErrorCode.CONFLICT.getMessage(), null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
		// Các exception còn lại của Spring MVC đã mang sẵn HTTP status (ErrorResponse)
		if (ex instanceof ErrorResponse errorResponse) {
			HttpStatusCode status = errorResponse.getStatusCode();
			CommonErrorCode errorCode = CommonErrorCode.fromStatus(status);
			log.debug("Spring MVC error {}: {}", status, ex.getMessage());
			return ResponseEntity.status(status).body(ApiResponse.error(errorCode));
		}
		log.error("Unexpected error", ex);
		return build(CommonErrorCode.INTERNAL_ERROR, CommonErrorCode.INTERNAL_ERROR.getMessage(), null);
	}

	private ResponseEntity<ApiResponse<Void>> validationError(List<FieldViolation> errors) {
		return build(CommonErrorCode.VALIDATION_ERROR, CommonErrorCode.VALIDATION_ERROR.getMessage(), errors);
	}

	private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode, String message, List<FieldViolation> errors) {
		return ResponseEntity.status(errorCode.getHttpStatus())
				.body(ApiResponse.error(errorCode, message, errors));
	}

}
