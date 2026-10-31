package com.cinema.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    @NotBlank
    private String issuer = "cinema";

    @NotBlank
    private String secret;

    private Duration accessTokenTtl = Duration.ofMinutes(15);

    private Duration refreshTokenTtl = Duration.ofDays(7);

    @NotBlank
    private String refreshCookieName = "refresh_token";

    private boolean refreshCookieSecure = false;

    private String refreshCookieSameSite = "Lax";
}
