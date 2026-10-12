package com.wetech.hrms.modules.user;

import com.wetech.hrms.config.AppProperties;
import com.wetech.hrms.modules.user.entity.Role;
import com.wetech.hrms.modules.user.entity.RoleName;
import com.wetech.hrms.modules.user.entity.User;
import com.wetech.hrms.modules.user.repository.RoleRepository;
import com.wetech.hrms.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Tạo các role hệ thống và tài khoản admin đầu tiên khi khởi động (idempotent).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AppProperties appProperties;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		Role adminRole = ensureRole(RoleName.ADMIN, "Quản trị hệ thống");
		Role userRole = ensureRole(RoleName.USER, "Người dùng");
		createBootstrapAdmin(Set.of(adminRole, userRole));
	}

	private Role ensureRole(String name, String description) {
		return roleRepository.findByName(name)
				.orElseGet(() -> roleRepository.save(new Role(name, description)));
	}

	private void createBootstrapAdmin(Set<Role> roles) {
		AppProperties.BootstrapAdmin admin = appProperties.security().bootstrapAdmin();
		if (!StringUtils.hasText(admin.password())) {
			log.info("Bỏ qua tạo tài khoản admin vì chưa cấu hình app.security.bootstrap-admin.password");
			return;
		}
		if (userRepository.existsByUsername(admin.username())) {
			return;
		}
		User user = new User();
		user.setUsername(admin.username());
		user.setPassword(passwordEncoder.encode(admin.password()));
		user.setEmail(StringUtils.hasText(admin.email()) ? admin.email() : null);
		user.setFullName("Administrator");
		user.setRoles(roles);
		userRepository.save(user);
		log.warn("Đã tạo tài khoản admin '{}'. Hãy đổi mật khẩu sau lần đăng nhập đầu tiên.", admin.username());
	}

}
