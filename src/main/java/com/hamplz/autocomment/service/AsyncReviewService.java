package com.hamplz.autocomment.service;

import com.hamplz.autocomment.dto.PullRequestWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncReviewService {

    private static final Logger log = LoggerFactory.getLogger(AsyncReviewService.class);

    private final PullRequestReviewService pullRequestReviewService;

    public AsyncReviewService(PullRequestReviewService pullRequestReviewService) {
        this.pullRequestReviewService = pullRequestReviewService;
    }

    @Async("reviewTaskExecutor")
    public void reviewAsync(PullRequestWebhook parsedWebhook) {
        try {
            log.info("비동기 리뷰 시작 - {} PR #{}", parsedWebhook.repoFullName(), parsedWebhook.prNumber());
            pullRequestReviewService.review(parsedWebhook);
            log.info("비동기 리뷰 완료 - {} PR #{}", parsedWebhook.repoFullName(), parsedWebhook.prNumber());
        } catch(Exception e) {
            log.error("비동기 리뷰 실패 - {} PR #{}", parsedWebhook.repoFullName(), parsedWebhook.prNumber(), e);
        }
    }
}