package com.hamplz.autocomment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "review.queue")
public record ReviewQueueProperties(
    String key,
    String processingKey
) {
    public static final String PREFIX = "review.queue";
    public static final String WORKER_ENABLED_PROPERTY = PREFIX + ".worker-enabled";
    public static final String WORKER_ENABLED_VALUE = "true";
    public static final String POLL_DELAY_PROPERTY = PREFIX + ".poll-delay";
    public static final String DEFAULT_POLL_DELAY_MILLIS = "1000";
    public static final String POLL_DELAY_PLACEHOLDER = "${" + POLL_DELAY_PROPERTY + ":" + DEFAULT_POLL_DELAY_MILLIS + "}";

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
