package com.example.NextPlan.Kakao.common;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class AiServerException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String responseBody;

    public AiServerException(HttpStatusCode statusCode, String responseBody, Throwable cause) {
        super("AI server request failed.", cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
