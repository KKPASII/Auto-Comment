package com.hamplz.autocomment.service;

import com.hamplz.autocomment.dto.PullRequestWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class PullRequestReviewService {

    private static final Logger log = LoggerFactory.getLogger(PullRequestReviewService.class);

    private final GithubDiffService githubDiffService;
    private final GptReviewService gptReviewService;
    private final AsyncResultDispatchService asyncResultDispatchService;

    public PullRequestReviewService(GithubDiffService githubDiffService, GptReviewService gptReviewService, AsyncResultDispatchService asyncResultDispatchService) {
        this.githubDiffService = githubDiffService;
        this.gptReviewService = gptReviewService;
        this.asyncResultDispatchService = asyncResultDispatchService;
    }

    public void review(PullRequestWebhook webhook) {
        log.info("리뷰 시작 - {} PR #{}", webhook.repoFullName(), webhook.prNumber());

        String diffContent = githubDiffService.getPullRequestDiff(webhook.diffUrl());
        log.info("\n==== PR DIFF START ====\n{}\n==== PR DIFF END ====\n", diffContent);

        String reviewComment = gptReviewService.generateReview(diffContent);
        log.info("\n==== GPT REVIEW START ====\n{}\n==== GPT REVIEW END ====\n", reviewComment);

        CompletableFuture<Void> commentFuture =
            asyncResultDispatchService.commentAsync(
                webhook.repoFullName(),
                webhook.prNumber(),
                reviewComment
            );

        CompletableFuture<Void> saveReviewFuture =
            asyncResultDispatchService.saveReviewAsync(
                webhook.repoFullName(),
                webhook.prNumber(),
                webhook.title(),
                webhook.action().getAction(),
                reviewComment
            );

        CompletableFuture.allOf(commentFuture, saveReviewFuture).join();

        log.info("리뷰 완료 - {} PR #{}", webhook.repoFullName(), webhook.prNumber());
    }
}
