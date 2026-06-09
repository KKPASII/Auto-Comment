package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.config.ReviewDeduplicationProperties;
import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReviewRequestDeduplicationService {

    private static final String IN_PROGRESS_VALUE = "in-progress";

    private final StringRedisTemplate redisTemplate;
    private final ReviewDeduplicationProperties reviewDeduplicationProperties;
    private final ReviewRedisKeyFactory reviewRedisKeyFactory;

    public ReviewRequestDeduplicationService(
        StringRedisTemplate redisTemplate,
        ReviewDeduplicationProperties reviewDeduplicationProperties,
        ReviewRedisKeyFactory reviewRedisKeyFactory
    ) {
        this.redisTemplate = redisTemplate;
        this.reviewDeduplicationProperties = reviewDeduplicationProperties;
        this.reviewRedisKeyFactory = reviewRedisKeyFactory;
    }

    public boolean tryStart(PullRequestWebhook webhook) {
        Boolean saved = redisTemplate.opsForValue()
            .setIfAbsent(
                reviewRedisKeyFactory.deduplicationKey(webhook),
                IN_PROGRESS_VALUE,
                reviewDeduplicationProperties.ttl()
            );

        return Boolean.TRUE.equals(saved);
    }
}
