package com.example.NextPlan.auth.controller;

import com.example.NextPlan.auth.CookieUtil;
import com.example.NextPlan.auth.JwtProvider;
import com.example.NextPlan.auth.Service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request.code());
        cookieUtil.addRefreshTokenCookie(response, result.refreshToken(), jwtProvider.getRefreshTokenExpirationSec());

        return ResponseEntity.ok(Map.of(
                "accessToken", result.accessToken(),
                "expiresIn", result.expiresIn(),
                "user", result.user()
        ));
    }

    @PostMapping({"/reissue", "/refresh"})
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        AuthService.ReissueResult result = authService.reissue(refreshToken);
        cookieUtil.addRefreshTokenCookie(response, result.refreshToken(), jwtProvider.getRefreshTokenExpirationSec());

        return ResponseEntity.ok(Map.of(
                "accessToken", result.accessToken(),
                "expiresIn", result.expiresIn()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        cookieUtil.expireRefreshTokenCookie(response);

        return ResponseEntity.ok(Map.of("message", "success"));
    }

    @DeleteMapping("/withdraw")
    public Map<String, String> withdraw(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        authService.withdraw(refreshToken);
        cookieUtil.expireRefreshTokenCookie(response);

        return Map.of("message", "account deleted");
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public record LoginRequest(
            @NotBlank(message = "code is required")
            String code
    ) {
    }
}
