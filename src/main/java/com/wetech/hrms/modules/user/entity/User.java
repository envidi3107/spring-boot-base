package com.wetech.hrms.modules.user.entity;

import com.wetech.hrms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

	@Column(name = "username", nullable = false, unique = true, length = 100)
	private String username;

	/**
	 * Mật khẩu đã băm (BCrypt), không bao giờ lưu plain text.
	 */
	@Column(name = "password", nullable = false)
	private String password;

	@Column(name = "email", unique = true)
	private String email;

	@Column(name = "full_name")
	private String fullName;

	@Column(name = "enabled", nullable = false)
	private boolean enabled = true;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "user_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

}
