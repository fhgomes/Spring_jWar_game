package br.com.bnuuy.jwar.server.api;

import br.com.bnuuy.jwar.server.api.mapper.UserMapper;
import br.com.bnuuy.jwar.server.auth.CurrentUser;
import br.com.bnuuy.jwar.server.dto.UpdateUserRequest;
import br.com.bnuuy.jwar.server.dto.UserResponse;
import br.com.bnuuy.jwar.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Authenticated user profile management")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final CurrentUser currentUser;

    @Operation(summary = "Get the current authenticated user's profile")
    @GetMapping
    public UserResponse me() {
        return userMapper.toResponse(userService.getById(currentUser.userId()));
    }

    @Operation(summary = "Update the current user's profile")
    @PatchMapping
    public UserResponse update(@Valid @RequestBody UpdateUserRequest request) {
        return userMapper.toResponse(userService.updateProfile(currentUser.userId(), request));
    }

    @Operation(summary = "Delete the current user's account")
    @DeleteMapping
    public ResponseEntity<Void> delete() {
        userService.deleteAccount(currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
