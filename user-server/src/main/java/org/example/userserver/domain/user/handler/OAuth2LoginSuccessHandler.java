package org.example.userserver.domain.user.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userserver.domain.user.dto.response.GoogleLoginResponseDto;
import org.example.userserver.domain.user.entity.User;
import org.example.userserver.domain.user.service.UserService;
import org.example.userserver.global.jwt.JwtUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * This method is triggered after a user successfully authenticates via an OAuth2 provider (e.g., Google).
     * The flow leading to this method is as follows:
     * 1. After the user authenticates on the provider's site, the provider redirects the user back to the application.
     * 2. The redirect URI is typically formatted as "/login/oauth2/code/{provider}", e.g., "/login/oauth2/code/google".
     * 3. This redirect is a GET request containing an "authorization_code".
     * 4. Spring Security's OAuth2LoginAuthenticationFilter automatically intercepts this request.
     * 5. The filter exchanges the authorization_code for an access_token with the provider.
     * 6. With the access_token, it fetches the user's profile information.
     * 7. Once the user is successfully authenticated, this onAuthenticationSuccess handler is invoked.
     * Developers do NOT need to create a controller for the "/login/oauth2/code/{provider}" endpoint as Spring Security handles it.
     */
    // TODO - reset GCP redirect_uris, javascript_origins
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // Get user information from the successful authentication
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Process user using the UserService, passing all attributes
        User user = userService.processOAuth2User(attributes);

        // Generate our application's own Access and Refresh tokens
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRoleKey());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Store the Refresh Token in Redis for later validation
        redisTemplate.opsForValue().set(
                "RT:" + user.getEmail(),
                refreshToken,
                jwtUtil.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        log.info("Successfully logged in with Google. User email: {}", user.getEmail());

        // Build the DTO to be sent in the response
        GoogleLoginResponseDto loginResponse = GoogleLoginResponseDto.builder()
            .userId(user.getId())
            .userRole(user.getRoleKey())
            .profileImage(user.getProfileImage())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();

        // Write the DTO to the HTTP response body as JSON
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(loginResponse));
    }
}