package com.example.NextPlan.auth.Service;

import com.example.NextPlan.Entity.RefreshToken;
import com.example.NextPlan.Entity.User;
import com.example.NextPlan.Repository.RefreshTokenRepository;
import com.example.NextPlan.Repository.UserRepository;
import com.example.NextPlan.auth.JwtProvider;
import com.example.NextPlan.common.CustomException;
import com.example.NextPlan.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import io.jsonwebtoken.Claims;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.token-uri}")
    private String tokenUri;

    @Value("${kakao.user-info-uri}")
    private String userInfoUri;

    @Value("${kakao.unlink-uri}")
    private String unlinkUri;

    //외부 HTTP API 호출용
    private final WebClient webClient = WebClient.builder().build();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public LoginResult login(String code) {
        KakaoTokenResponse kakaoToken = requestKakaoToken(code);
        KakaoUserResponse kakaoUser = requestKakaoUser(kakaoToken.access_token());
        String kakaoId = String.valueOf(kakaoUser.id());

        User user = userRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .kakaoId(kakaoId)
                                .nickname(kakaoUser.properties().nickname())
                                .profileImg(kakaoUser.properties().profile_image())
                                .email(null)
                                .isOnboarded(false)
                                .createdAt(OffsetDateTime.now())
                                .build()
                ));

        String accessToken = jwtProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId());

        saveOrUpdateRefreshToken(user, refreshToken);

        return new LoginResult(
                accessToken,
                refreshToken,
                jwtProvider.getAccessTokenExpirationSec(),
                new UserInfo(
                        user.getUserId(),
                        user.getNickname(),
                        user.getProfileImg()
                )
        );
    }

    public ReissueResult reissue(String refreshToken) {

        jwtProvider.validateRefreshToken(refreshToken);
        UUID userId = jwtProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));

        RefreshToken saved = refreshTokenRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED));

        if (!saved.getToken().equals(refreshToken)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        String newAccessToken = jwtProvider.createAccessToken(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        saved.updateToken(newRefreshToken,
                OffsetDateTime.now().plusSeconds(jwtProvider.getRefreshTokenExpirationSec()));

        refreshTokenRepository.save(saved);

        return new ReissueResult(
                newAccessToken,
                newRefreshToken,
                jwtProvider.getAccessTokenExpirationSec()
        );
}

    public void logout(String refreshToken) {
        UUID userId = jwtProvider.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));

        refreshTokenRepository.deleteByUser(user);
    }

    public void withdraw(String refreshToken) {
        UUID userId = jwtProvider.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));

        refreshTokenRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    private KakaoTokenResponse requestKakaoToken(String code) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "authorization_code");
            formData.add("client_id", clientId);
            formData.add("redirect_uri", redirectUri);
            formData.add("code", code);

            if (clientSecret != null && !clientSecret.isBlank()) {
                formData.add("client_secret", clientSecret);
            }

            KakaoTokenResponse response = webClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(KakaoTokenResponse.class)
                    .block();

            if (response == null || response.access_token() == null) {
                throw new CustomException(ErrorCode.KAKAO_AUTH_FAILED);
            }

            return response;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.KAKAO_AUTH_FAILED);
        }
    }

    private KakaoUserResponse requestKakaoUser(String kakaoAccessToken) {
        try {
            KakaoUserResponse response = webClient.post()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserResponse.class)
                    .block();

            if (response == null || response.id() == null) {
                throw new CustomException(ErrorCode.KAKAO_AUTH_FAILED);
            }

            return response;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.KAKAO_AUTH_FAILED);
        }
    }

    private void requestKakaoUnlink(String kakaoAccessToken) {
        try {
            webClient.post()
                    .uri(unlinkUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.KAKAO_AUTH_FAILED);
        }
    }

    public record LoginResult(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserInfo user
    ) {}

    public record ReissueResult(
            String accessToken,
            String refreshToken,
            long expiresIn
    ) {}

    public record UserInfo(
            UUID id,
            String nickname,
            String profileImage
    ) {}

    public record KakaoTokenResponse(
            String token_type,
            String access_token,
            Long expires_in,
            String refresh_token,
            Long refresh_token_expires_in
    ) {}

    public record KakaoUserResponse(
            Long id,
            Properties properties
    ) {
        public record Properties(
                String nickname,
                String profile_image
        ) {}
    }
    private void saveOrUpdateRefreshToken(User user, String token) {

        OffsetDateTime expiry =
                OffsetDateTime.now().plusSeconds(jwtProvider.getRefreshTokenExpirationSec());

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .map(rt -> {
                    rt.updateToken(token, expiry);
                    return rt;
                })
                .orElseGet(() ->
                        RefreshToken.builder()
                                .user(user)
                                .token(token)
                                .expiresAt(expiry)
                                .createdAt(OffsetDateTime.now())
                                .updatedAt(OffsetDateTime.now())
                                .build()
                );

        refreshTokenRepository.save(refreshToken);
    }
}
