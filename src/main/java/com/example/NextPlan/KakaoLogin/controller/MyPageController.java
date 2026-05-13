package com.example.NextPlan.KakaoLogin.controller;
import com.example.NextPlan.KakaoLogin.Service.MyPageService;
import com.example.NextPlan.KakaoLogin.Service.MyPageService.MyPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MyPageController {
    private final MyPageService userMyPageService;

    @GetMapping
    public ResponseEntity<MyPageResponse> getMyPage(
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        MyPageResponse response = userMyPageService.getMyPage(userId);

        return ResponseEntity.ok(response);
    }
}
