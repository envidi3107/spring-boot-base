package com.wetech.hrms.modules.auth.service;

import com.wetech.hrms.modules.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

	private final RefreshTokenRepository refreshTokenRepository;

	/**
	 * Xoá refresh token đã hết hạn, chạy lúc 3h sáng mỗi ngày.
	 */
	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void deleteExpiredTokens() {
		int deleted = refreshTokenRepository.deleteExpiredBefore(Instant.now());
		log.info("Đã xoá {} refresh token hết hạn", deleted);
	}

}
