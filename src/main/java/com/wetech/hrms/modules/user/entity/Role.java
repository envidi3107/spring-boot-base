package com.wetech.hrms.modules.user.entity;

import com.wetech.hrms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

	/**
	 * Viết hoa, không có tiền tố {@code ROLE_} (vd. {@code ADMIN}); xem {@code RoleName}.
	 */
	@Column(name = "name", nullable = false, unique = true, length = 50)
	private String name;

	@Column(name = "description")
	private String description;

	public Role(String name, String description) {
		this.name = name;
		this.description = description;
	}

}
