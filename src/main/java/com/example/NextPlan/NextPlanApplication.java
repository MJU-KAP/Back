package com.example.NextPlan;

import com.example.NextPlan.KakaoLogin.config.CorsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(CorsConfig.class)
public class NextPlanApplication {
    public static void main(String[] args) {
        SpringApplication.run(NextPlanApplication.class, args);
    }
}
