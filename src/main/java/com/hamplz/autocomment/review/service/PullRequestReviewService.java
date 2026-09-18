package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.github.service.GithubDiffService;
import com.hamplz.autocomment.openai.GptReviewService;
import com.hamplz.autocomment.review.dto.DispatchResult;
import com.hamplz.autocomment.review.dto.DispatchTaskResult;
import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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

    public DispatchResult review(PullRequestWebhook webhook) {
        log.info("리뷰 시작 - {} PR #{}", webhook.repoFullName(), webhook.prNumber());

        String diffContent = githubDiffService.getPullRequestDiff(webhook.diffUrl());

        String reviewComment = gptReviewService.generateReview(diffContent);

        Supplier<CompletableFuture<DispatchTaskResult>> commentTask =
            () -> asyncResultDispatchService.commentAsync(
                webhook.repoFullName(),
                webhook.prNumber(),
                reviewComment
            );

        Supplier<CompletableFuture<DispatchTaskResult>> saveReviewTask =
            () -> asyncResultDispatchService.saveReviewAsync(
                webhook.repoFullName(),
                webhook.prNumber(),
                webhook.title(),
                webhook.action().getAction(),
                reviewComment
            );

        DispatchResult dispatchResult =
            dispatchInParallel(commentTask, saveReviewTask);

        if (!dispatchResult.isFullySucceeded()) {
            log.warn(
                "리뷰 결과 처리 일부 실패 - {} PR #{} {}",
                webhook.repoFullName(),
                webhook.prNumber(),
                dispatchResult.summary()
            );

            dispatchResult = retryFailedDispatchTasks(
                dispatchResult,
                commentTask,
                saveReviewTask
            );
        }

        log.info(
            "리뷰 완료 - {} PR #{} {}",
            webhook.repoFullName(),
            webhook.prNumber(),
            dispatchResult.summary()
        );

        return dispatchResult;
    }

    DispatchResult dispatchInParallel(
        Supplier<CompletableFuture<DispatchTaskResult>> commentTask,
        Supplier<CompletableFuture<DispatchTaskResult>> saveReviewTask
    ) {
        CompletableFuture<DispatchTaskResult> commentFuture = commentTask.get();
        CompletableFuture<DispatchTaskResult> saveReviewFuture = saveReviewTask.get();

        return new DispatchResult(
            commentFuture.join(),
            saveReviewFuture.join()
        );
    }

    DispatchResult retryFailedDispatchTasks(
        DispatchResult previousResult,
        Supplier<CompletableFuture<DispatchTaskResult>> commentTask,
        Supplier<CompletableFuture<DispatchTaskResult>> saveReviewTask

    ) {
        Supplier<CompletableFuture<DispatchTaskResult>> commentRetryTask =
            previousResult.commentResult().succeeded()
                ? () -> CompletableFuture.completedFuture(
                previousResult.commentResult()
            )
                : commentTask;

        Supplier<CompletableFuture<DispatchTaskResult>> saveReviewRetryTask =
            previousResult.saveReviewResult().succeeded()
                ? () -> CompletableFuture.completedFuture(
                previousResult.saveReviewResult()
            )
                : saveReviewTask;

        return dispatchInParallel(
            commentRetryTask,
            saveReviewRetryTask
        );
    }
}