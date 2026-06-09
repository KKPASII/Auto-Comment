package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.config.ReviewQueueProperties;
import com.hamplz.autocomment.review.dto.QueuedReviewJob;
import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReviewJobQueueService {

    private static final long REMOVE_ONE_MATCHING_JOB = 1L;

    private final StringRedisTemplate redisTemplate;
    private final ReviewQueueProperties reviewQueueProperties;
    private final ReviewJobPayloadMapper reviewJobPayloadMapper;

    public ReviewJobQueueService(
        StringRedisTemplate redisTemplate,
        ReviewQueueProperties reviewQueueProperties,
        ReviewJobPayloadMapper reviewJobPayloadMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.reviewQueueProperties = reviewQueueProperties;
        this.reviewJobPayloadMapper = reviewJobPayloadMapper;
    }

    public void enqueue(PullRequestWebhook webhook) {
        redisTemplate.opsForList().leftPush(queueKey(), reviewJobPayloadMapper.serialize(webhook));
    }

    public Optional<QueuedReviewJob> claimNext() {
        String payload = redisTemplate.opsForList()
            .rightPopAndLeftPush(queueKey(), processingQueueKey());

        if (payload == null) {
            return Optional.empty();
        }

        return Optional.of(new QueuedReviewJob(reviewJobPayloadMapper.deserialize(payload), payload));
    }

    public void complete(QueuedReviewJob job) {
        redisTemplate.opsForList().remove(processingQueueKey(), REMOVE_ONE_MATCHING_JOB, job.rawPayload());
    }

    public void restoreProcessingJobs() {
        String payload;
        while ((payload = redisTemplate.opsForList().rightPop(processingQueueKey())) != null) {
            redisTemplate.opsForList().leftPush(queueKey(), payload);
        }
    }

    private String queueKey() {
        return reviewQueueProperties.key();
    }

    private String processingQueueKey() {
        return reviewQueueProperties.processingKey();
    }
}
