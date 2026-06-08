package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.config.ReviewDeduplicationProperties;
import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReviewRequestDeduplicationService {

    private static final String KEY_PREFIX = "review:dedup";
    private static final String KEY_SEPARATOR = ":";
    private static final String UNKNOWN_VALUE = "unknown";
    private static final String IN_PROGRESS_VALUE = "in-progress";

    private final StringRedisTemplate redisTemplate;
    private final ReviewDeduplicationProperties reviewDeduplicationProperties;

    public ReviewRequestDeduplicationService(
        StringRedisTemplate redisTemplate,
        ReviewDeduplicationProperties reviewDeduplicationProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.reviewDeduplicationProperties = reviewDeduplicationProperties;
    }

    public boolean tryStart(PullRequestWebhook webhook) {
        Boolean saved = redisTemplate.opsForValue()
            .setIfAbsent(buildKey(webhook), IN_PROGRESS_VALUE, reviewDeduplicationProperties.ttl());

        return Boolean.TRUE.equals(saved);
    }

    private String buildKey(PullRequestWebhook webhook) {
        return KEY_PREFIX
            + KEY_SEPARATOR + safe(webhook.repoFullName())
            + KEY_SEPARATOR + webhook.prNumber()
            + KEY_SEPARATOR + safe(firstNonBlank(webhook.headSha(), webhook.headRef()))
            + KEY_SEPARATOR + safe(webhook.changedLabel());
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_VALUE;
        }
        return value.replaceAll("\\s+", "_");
    }
}
