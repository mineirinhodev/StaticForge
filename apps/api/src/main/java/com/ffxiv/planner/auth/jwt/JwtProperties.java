package com.ffxiv.planner.auth.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    @NotBlank
    @Size(min = 32, message = "JWT secret must be at least 32 characters for HS256")
    private String secret;

    @NotNull
    private Duration accessExpiration = Duration.ofMinutes(15);

    @NotNull
    private Duration refreshExpiration = Duration.ofDays(30);
}
