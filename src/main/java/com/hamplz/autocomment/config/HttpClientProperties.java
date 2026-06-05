package com.hamplz.autocomment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "http.client")
public record HttpClientProperties(
    Duration connectTimeout,
    Duration readTimeout
) {
    public HttpClientProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(30) : readTimeout;
    }
}
