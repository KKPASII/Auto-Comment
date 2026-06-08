package com.hamplz.autocomment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "review.deduplication")
public record ReviewDeduplicationProperties(
    Duration ttl
) {
    public ReviewDeduplicationProperties {
        ttl = ttl == null ? Duration.ofHours(1) : ttl;
    }
}
