package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncReviewService {

    private static final Logger log = LoggerFactory.getLogger(AsyncReviewService.class);

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
            log.info("비동기 리뷰 시작 - {} PR #{}", parsedWebhook.repoFullName(), parsedWebhook.prNumber());
            pullRequestReviewService.review(parsedWebhook);
            reviewJobStatusService.markSuccess(parsedWebhook);
            log.info("비동기 리뷰 완료 - {} PR #{}", parsedWebhook.repoFullName(), parsedWebhook.prNumber());
        } catch(Exception e) {
            reviewJobStatusService.markFailed(parsedWebhook, e);
            log.error("비동기 리뷰 실패 - {} PR #{}", parsedWebhook.repoFullName(), parsedWebhook.prNumber(), e);
        }
    }
}