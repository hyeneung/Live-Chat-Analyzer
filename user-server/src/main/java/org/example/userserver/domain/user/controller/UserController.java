package org.example.userserver.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userserver.domain.user.dto.request.TokenReissueRequestDto;
import org.example.userserver.domain.user.dto.response.TokenReissueResponseDto;
import org.example.userserver.domain.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Redirect to Google OAuth2 login",
            description = "Initiates the Google OAuth2 login process by redirecting to the authorization endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to Google authorization page"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/login/google")
    public void redirectToGoogleLogin(HttpServletResponse response) throws IOException {
        // Redirect to Spring Security's default endpoint for initiating OAuth2 login with Google
        response.sendRedirect("/oauth2/authorization/google");
    }

    @Operation(
            summary = "Reissue access and refresh tokens",
            description = "Reissues a new access token and refresh token using an existing refresh token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens successfully reissued"),
            @ApiResponse(responseCode = "400", description = "Invalid token reissue request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid refresh token"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/reissue")
    public ResponseEntity<TokenReissueResponseDto> reissueTokens(@Valid @RequestBody TokenReissueRequestDto requestDto) {
        TokenReissueResponseDto responseDto = userService.reissueTokens(requestDto);
        return ResponseEntity.ok(responseDto);
    }
}