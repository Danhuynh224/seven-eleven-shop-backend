package vn.sevenleven.shop.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.sevenleven.shop.dto.response.UserResponse;
import vn.sevenleven.shop.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    UserResponse toResponse(User user);
}
