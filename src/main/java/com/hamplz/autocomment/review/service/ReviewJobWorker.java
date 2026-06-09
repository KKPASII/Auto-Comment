package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.review.dto.QueuedReviewJob;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "review.queue.worker-enabled", havingValue = "true", matchIfMissing = true)
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

    @Scheduled(fixedDelayString = "${review.queue.poll-delay:1000}")
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
