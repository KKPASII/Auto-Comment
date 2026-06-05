package com.hamplz.autocomment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "external-api.retry")
public record RetryProperties(
    int maxAttempts,
    Duration backoff
) {
    public RetryProperties {
        maxAttempts = maxAttempts <= 0 ? 3 : maxAttempts;
        backoff = backoff == null ? Duration.ofSeconds(1) : backoff;
    }
}
