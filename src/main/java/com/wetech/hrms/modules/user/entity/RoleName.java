package com.wetech.hrms.modules.user.entity;

/**
 * Các role hệ thống luôn tồn tại (được tạo khi khởi động). Dùng trong
 * {@code @PreAuthorize("hasRole('ADMIN')")}.
 */
public final class RoleName {

	public static final String ADMIN = "ADMIN";
	public static final String USER = "USER";

	private RoleName() {
	}

}
