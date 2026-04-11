package com.example.NextPlan.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    ACCESS_TOKEN_EXPIRED("AUTH-401-001", "Access Token 만료", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("AUTH-401-002", "Refresh Token 만료", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("AUTH-403-001", "권한 없음", HttpStatus.FORBIDDEN),

    INVALID_REQUEST("COM-400-001", "필수 파라미터 누락 또는 잘못된 요청", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("COM-500-001", "서버 내부 오류", HttpStatus.INTERNAL_SERVER_ERROR),

    KAKAO_AUTH_FAILED("AUTH-400-003", "카카오 인증 실패", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
