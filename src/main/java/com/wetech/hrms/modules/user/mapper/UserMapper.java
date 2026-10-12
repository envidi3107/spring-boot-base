package com.wetech.hrms.modules.user.mapper;

import com.wetech.hrms.modules.user.dto.UserResponse;
import com.wetech.hrms.modules.user.entity.Role;
import com.wetech.hrms.modules.user.entity.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

	UserResponse toResponse(User user);

	default String toRoleName(Role role) {
		return role.getName();
	}

}
