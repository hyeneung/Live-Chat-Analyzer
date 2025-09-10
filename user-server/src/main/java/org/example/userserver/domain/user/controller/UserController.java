package org.example.userserver.domain.user.controller;

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

    /**
     * Redirects the user to the Google OAuth2 authorization endpoint.
     * This endpoint is the starting point for the Google login process.
     *
     * @param response The HttpServletResponse to perform the redirect.
     * @throws IOException If an I/O error occurs during the redirect.
     */
    @GetMapping("/login/google")
    public void redirectToGoogleLogin(HttpServletResponse response) throws IOException {
        // Redirect to Spring Security's default endpoint for initiating OAuth2 login with Google
        response.sendRedirect("/oauth2/authorization/google");
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenReissueResponseDto> reissueTokens(@Valid @RequestBody TokenReissueRequestDto requestDto) {
        TokenReissueResponseDto responseDto = userService.reissueTokens(requestDto);
        return ResponseEntity.ok(responseDto);
    }
}