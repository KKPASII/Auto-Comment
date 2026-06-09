package com.hamplz.autocomment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "review.status")
public record ReviewStatusProperties(
    Duration ttl
) {
    public ReviewStatusProperties {
        ttl = ttl == null ? Duration.ofDays(1) : ttl;
    }
}
