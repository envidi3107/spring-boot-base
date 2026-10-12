package com.wetech.hrms.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Entity gốc: khoá chính UUID v7 (sắp xếp theo thời gian, thân thiện với index),
 * optimistic locking qua {@code version} và các cột audit.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity extends AuditableEntity {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
			return false;
		}
		return id != null && id.equals(((BaseEntity) o).getId());
	}

	@Override
	public final int hashCode() {
		return Hibernate.getClass(this).hashCode();
	}

}
