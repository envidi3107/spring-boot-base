package com.wetech.hrms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * Cấu hình riêng của ứng dụng, prefix {@code app.*} trong application.yaml.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(@DefaultValue Cors cors, @DefaultValue Security security) {

	/**
	 * @param allowedOrigins origin (hỗ trợ pattern, vd. {@code https://*.wetech.vn}) được phép gọi API
	 * @param maxAge         thời gian (giây) trình duyệt cache kết quả preflight
	 */
	public record Cors(List<String> allowedOrigins, @DefaultValue("3600") long maxAge) {

		public Cors {
			allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
		}

	}

	/**
	 * @param publicPaths    các path không cần token, bổ sung vào danh sách mặc định trong {@code SecurityConfig}
	 * @param jwt            cấu hình access/refresh token
	 * @param bootstrapAdmin tài khoản admin tạo tự động khi khởi động nếu chưa tồn tại
	 */
	public record Security(List<String> publicPaths, @DefaultValue Jwt jwt, @DefaultValue BootstrapAdmin bootstrapAdmin) {

		public Security {
			publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
		}

	}

	/**
	 * @param secret          khoá ký HS512, tối thiểu 64 byte
	 * @param issuer          giá trị claim {@code iss}
	 * @param accessTokenTtl  thời hạn access token
	 * @param refreshTokenTtl thời hạn refresh token
	 */
	public record Jwt(
			String secret,
			@DefaultValue("hrms") String issuer,
			@DefaultValue("15m") Duration accessTokenTtl,
			@DefaultValue("7d") Duration refreshTokenTtl) {
	}

	/**
	 * Bỏ trống {@code password} để không tạo admin.
	 */
	public record BootstrapAdmin(@DefaultValue("admin") String username, String password, String email) {
	}

}
