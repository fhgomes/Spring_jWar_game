package br.com.bnuuy.jwar.server.api;

import br.com.bnuuy.jwar.server.auth.FirebaseAuthService;
import br.com.bnuuy.jwar.server.dto.RegisterRequest;
import br.com.bnuuy.jwar.server.dto.RegisterResponse;
import br.com.bnuuy.jwar.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Account creation and authentication helpers")
public class AuthController {

    private final FirebaseAuthService firebaseAuthService;
    private final UserService userService;

    @Operation(summary = "Create a new email/password account in Firebase and bootstrap the local user row")
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        FirebaseAuthService.CreatedUser created = firebaseAuthService.createUser(
            request.email(), request.password(), request.displayName());
        userService.bootstrap(new FirebaseAuthService.VerifiedToken(
            created.uid(), created.email(), false, created.displayName(), "password"));
        String customToken = firebaseAuthService.createCustomToken(created.uid());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new RegisterResponse(created.uid(), customToken));
    }
}
