package com.wetech.hrms.common.response;

/**
 * Một lỗi validate gắn với một field/tham số cụ thể.
 */
public record FieldViolation(String field, String message) {
}
