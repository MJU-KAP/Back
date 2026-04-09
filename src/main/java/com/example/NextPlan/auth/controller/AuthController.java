package com.example.NextPlan.auth.controller;

import com.example.NextPlan.auth.CookieUtil;
import com.example.NextPlan.auth.JwtProvider;
import com.example.NextPlan.auth.Service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtProvider jwtProvider;

    @PostMapping("/kakao/login")
    public Map<String, Object> kakaoLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthService.LoginResult result = authService.login(request.code());

        cookieUtil.addRefreshTokenCookie(
                response,
                result.refreshToken(),
                jwtProvider.getRefreshTokenExpirationSec()
        );

        return Map.of(
                "accessToken", result.accessToken(),
                "expiresIn", result.expiresIn(),
                "user", Map.of(
                        "id", result.user().id(),
                        "nickname", result.user().nickname(),
                        "profileImage", result.user().profileImage() == null ? "" : result.user().profileImage()
                )
        );
    }

    @PostMapping("/reissue")
    public Map<String, Object> reissue(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractRefreshToken(request);
        AuthService.ReissueResult result = authService.reissue(refreshToken);

        cookieUtil.addRefreshTokenCookie(
                response,
                result.refreshToken(),
                jwtProvider.getRefreshTokenExpirationSec()
        );

        return Map.of(
                "accessToken", result.accessToken(),
                "expiresIn", result.expiresIn()
        );
    }

    @PostMapping("/logout")
    public Map<String, String> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractRefreshToken(request);
        authService.logout(refreshToken);
        cookieUtil.expireRefreshTokenCookie(response);

        return Map.of("message", "success");
    }

    @DeleteMapping("/withdraw")
    public Map<String, String> withdraw(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractRefreshToken(request);
        authService.withdraw(refreshToken);
        cookieUtil.expireRefreshTokenCookie(response);

        return Map.of("message", "account deleted");
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public record LoginRequest(
            @NotBlank(message = "code는 필수입니다.")
            String code
    ) {}
}
