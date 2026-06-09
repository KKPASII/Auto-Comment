package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.config.ReviewQueueProperties;
import com.hamplz.autocomment.review.dto.QueuedReviewJob;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = ReviewQueueProperties.WORKER_ENABLED_PROPERTY,
    havingValue = ReviewQueueProperties.WORKER_ENABLED_VALUE,
    matchIfMissing = true
)
public class ReviewJobWorker {

    private final ReviewJobQueueService reviewJobQueueService;
    private final AsyncReviewService asyncReviewService;

    public ReviewJobWorker(
        ReviewJobQueueService reviewJobQueueService,
        AsyncReviewService asyncReviewService
    ) {
        this.reviewJobQueueService = reviewJobQueueService;
        this.asyncReviewService = asyncReviewService;
    }

    @PostConstruct
    public void restoreProcessingJobs() {
        reviewJobQueueService.restoreProcessingJobs();
    }

    @Scheduled(fixedDelayString = ReviewQueueProperties.POLL_DELAY_PLACEHOLDER)
    public void poll() {
        reviewJobQueueService.claimNext().ifPresent(this::process);
    }

    private void process(QueuedReviewJob job) {
        try {
            asyncReviewService.review(job.webhook());
        } finally {
            reviewJobQueueService.complete(job);
        }
    }
}
