package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.config.ReviewStatusProperties;
import com.hamplz.autocomment.review.dto.ReviewJobStatus;
import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReviewJobStatusService {

    private static final String STATUS_FIELD = "status";
    private static final String UPDATED_AT_FIELD = "updatedAt";
    private static final String ERROR_MESSAGE_FIELD = "errorMessage";

    private final StringRedisTemplate redisTemplate;
    private final ReviewStatusProperties reviewStatusProperties;
    private final ReviewRedisKeyFactory reviewRedisKeyFactory;

    public ReviewJobStatusService(
        StringRedisTemplate redisTemplate,
        ReviewStatusProperties reviewStatusProperties,
        ReviewRedisKeyFactory reviewRedisKeyFactory
    ) {
        this.redisTemplate = redisTemplate;
        this.reviewStatusProperties = reviewStatusProperties;
        this.reviewRedisKeyFactory = reviewRedisKeyFactory;
    }

    public void markRunning(PullRequestWebhook webhook) {
        save(webhook, ReviewJobStatus.RUNNING, null);
    }

    public void markSuccess(PullRequestWebhook webhook) {
        save(webhook, ReviewJobStatus.SUCCESS, null);
    }

    public void markFailed(PullRequestWebhook webhook, Exception e) {
        save(webhook, ReviewJobStatus.FAILED, e.getMessage());
    }

    private void save(PullRequestWebhook webhook, ReviewJobStatus status, String errorMessage) {
        String key = reviewRedisKeyFactory.statusKey(webhook);
        Map<String, String> values = new HashMap<>();
        values.put(STATUS_FIELD, status.name());
        values.put(UPDATED_AT_FIELD, Instant.now().toString());

        if (errorMessage != null && !errorMessage.isBlank()) {
            values.put(ERROR_MESSAGE_FIELD, errorMessage);
        }

        redisTemplate.opsForHash().putAll(key, values);
        redisTemplate.expire(key, reviewStatusProperties.ttl());
    }
}
