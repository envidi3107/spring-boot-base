package com.wetech.hrms.config;

import com.wetech.hrms.common.util.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

	/**
	 * Giá trị created_by/updated_by khi không có người dùng đăng nhập (job, migration dữ liệu...).
	 */
	public static final String SYSTEM_AUDITOR = "system";

	@Bean
	AuditorAware<String> auditorAware() {
		return () -> Optional.of(SecurityUtils.getCurrentUsername().orElse(SYSTEM_AUDITOR));
	}

}
