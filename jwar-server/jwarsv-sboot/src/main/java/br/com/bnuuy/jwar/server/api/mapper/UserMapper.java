package br.com.bnuuy.jwar.server.api.mapper;

import br.com.bnuuy.jwar.server.domain.User;
import br.com.bnuuy.jwar.server.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
