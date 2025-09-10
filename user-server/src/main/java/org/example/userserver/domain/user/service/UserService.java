package org.example.userserver.domain.user.service;

import org.example.userserver.domain.user.dto.response.TokenReissueResponseDto;
import org.example.userserver.domain.user.entity.User;

/**
 * Service interface for managing users.
 */
public interface UserService {

    /**
     * Processes a user from OAuth2 authentication.
     * If the user with the given email exists, it updates their information.
     * If the user does not exist, it creates a new user.
     *
     * @param attributes The user attributes from the OAuth2 provider.
     * @return The existing or newly created User entity.
     */
    User processOAuth2User(java.util.Map<String, Object> attributes);

    /**
     * Reissues access and refresh tokens using a valid refresh token.
     *
     * @param requestDto DTO containing the refresh token.
     * @return A DTO containing the new access and refresh tokens.
     */
    TokenReissueResponseDto reissueTokens(org.example.userserver.domain.user.dto.request.TokenReissueRequestDto requestDto);
}