package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncReviewService {

    private static final Logger log = LoggerFactory.getLogger(AsyncReviewService.class);
    private static final String LOG_REVIEW_STARTED = "Review job started - {} PR #{}";
    private static final String LOG_REVIEW_SUCCEEDED = "Review job succeeded - {} PR #{}";
    private static final String LOG_REVIEW_FAILED = "Review job failed - {} PR #{}";

    private final PullRequestReviewService pullRequestReviewService;
    private final ReviewJobStatusService reviewJobStatusService;

    public AsyncReviewService(
        PullRequestReviewService pullRequestReviewService,
        ReviewJobStatusService reviewJobStatusService
    ) {
        this.pullRequestReviewService = pullRequestReviewService;
        this.reviewJobStatusService = reviewJobStatusService;
    }

    @Async("reviewTaskExecutor")
    public void reviewAsync(PullRequestWebhook parsedWebhook) {
        review(parsedWebhook);
    }

    public void review(PullRequestWebhook parsedWebhook) {
        try {
            reviewJobStatusService.markRunning(parsedWebhook);
            log.info(LOG_REVIEW_STARTED, parsedWebhook.repoFullName(), parsedWebhook.prNumber());
            pullRequestReviewService.review(parsedWebhook);
            reviewJobStatusService.markSuccess(parsedWebhook);
            log.info(LOG_REVIEW_SUCCEEDED, parsedWebhook.repoFullName(), parsedWebhook.prNumber());
        } catch (Exception e) {
            reviewJobStatusService.markFailed(parsedWebhook, e);
            log.error(LOG_REVIEW_FAILED, parsedWebhook.repoFullName(), parsedWebhook.prNumber(), e);
        }
    }
}