package com.wetech.hrms.modules.user.repository;

import com.wetech.hrms.modules.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

	@EntityGraph(attributePaths = "roles")
	Optional<User> findByUsername(String username);

	@EntityGraph(attributePaths = "roles")
	Optional<User> findWithRolesById(UUID id);

	boolean existsByUsername(String username);

}
