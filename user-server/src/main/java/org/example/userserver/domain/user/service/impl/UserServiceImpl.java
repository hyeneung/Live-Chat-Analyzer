package org.example.userserver.domain.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.userserver.domain.user.dto.request.TokenReissueRequestDto;
import org.example.userserver.domain.user.dto.response.TokenReissueResponseDto;
import org.example.userserver.domain.user.entity.Role;
import org.example.userserver.domain.user.entity.User;
import org.example.userserver.domain.user.repository.UserRepository;
import org.example.userserver.domain.user.service.UserService;
import org.example.userserver.domain.user.exception.UserException;
import org.example.userserver.domain.user.exception.UserExceptionDetails;
import org.example.userserver.global.jwt.JwtUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    @Transactional
    public User processOAuth2User(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String profileImage = (String) attributes.get("picture"); // Standard OIDC claim for picture

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (!user.getName().equals(name)) {
                user.updateNameTo(name);
            }
            // Also update the profile image if it has changed
            if (profileImage != null && !profileImage.equals(user.getProfileImage())) {
                user.updateProfileImageTo(profileImage);
            }
            return user;
        } else {
            // If user does not exist, create and save a new user.
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .profileImage(profileImage)
                    .role(Role.USER)
                    .build();
            return userRepository.save(newUser);
        }
    }

    @Override
    @Transactional
    public TokenReissueResponseDto reissueTokens(TokenReissueRequestDto requestDto) {
        String refreshToken = requestDto.refreshToken();

        // 1. Validate Refresh Token
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new UserException(UserExceptionDetails.INVALID_REFRESH_TOKEN);
        }

        // 2. Extract email from Refresh Token
        String email = jwtUtil.getEmailFromToken(refreshToken);

        // 3. Verify Refresh Token against Redis
        String storedRefreshToken = redisTemplate.opsForValue().get("RT:" + email);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new UserException(UserExceptionDetails.REFRESH_TOKEN_MISMATCH);
        }

        // 4. Retrieve User information
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(UserExceptionDetails.USER_NOT_FOUND));

        // 5. Generate new Access Token and Refresh Token (Rotation)
        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRoleKey());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // 6. Store new Refresh Token in Redis
        redisTemplate.opsForValue().set(
                "RT:" + user.getEmail(),
                newRefreshToken,
                jwtUtil.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        // 7. Return new tokens in DTO
        return new TokenReissueResponseDto(newAccessToken, newRefreshToken);
    }
}
