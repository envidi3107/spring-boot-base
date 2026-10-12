package com.wetech.hrms.modules.user.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
		UUID id,
		String username,
		String email,
		String fullName,
		boolean enabled,
		Set<String> roles,
		Instant createdAt) {
}
