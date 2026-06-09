package com.hamplz.autocomment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "review.queue")
public record ReviewQueueProperties(
    String key,
    String processingKey
) {
    private static final String DEFAULT_KEY = "review:queue";
    private static final String DEFAULT_PROCESSING_KEY = "review:queue:processing";

    public ReviewQueueProperties {
        key = isBlank(key) ? DEFAULT_KEY : key;
        processingKey = isBlank(processingKey) ? DEFAULT_PROCESSING_KEY : processingKey;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
