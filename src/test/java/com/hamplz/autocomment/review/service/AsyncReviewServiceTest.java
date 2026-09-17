package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.github.service.GithubDiffService;
import com.hamplz.autocomment.openai.GptReviewService;
import com.hamplz.autocomment.review.dto.DispatchTaskResult;
import com.hamplz.autocomment.webhook.dto.PullRequestAction;
import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AsyncReviewServiceTest {

    @Mock
    private GithubDiffService githubDiffService;

    @Mock
    private GptReviewService gptReviewService;

    @Mock
    private AsyncResultDispatchService asyncResultDispatchService;

    @Mock
    private ReviewJobStatusService reviewJobStatusService;

    private AsyncReviewService asyncReviewService;

    @BeforeEach
    void setUp() {
        PullRequestReviewService pullRequestReviewService =
            new PullRequestReviewService(
                githubDiffService,
                gptReviewService,
                asyncResultDispatchService
            );

        asyncReviewService =
            new AsyncReviewService(
                pullRequestReviewService,
                reviewJobStatusService
            );
    }

    @Test
    void PR코멘트나_리뷰파일저장_중_하나라도_실패하면_SUCCESS로_처리하면_안된다() {
        // given
        PullRequestWebhook webhook = createWebhook();

        when(githubDiffService.getPullRequestDiff(anyString()))
            .thenReturn("test diff");

        when(gptReviewService.generateReview("test diff"))
            .thenReturn("test review");

        when(asyncResultDispatchService.commentAsync(
            anyString(),
            anyInt(),
            anyString()
        )).thenReturn(
            CompletableFuture.completedFuture(
                DispatchTaskResult.success()
            )
        );

        when(asyncResultDispatchService.saveReviewAsync(
            anyString(),
            anyInt(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(
            CompletableFuture.completedFuture(
                DispatchTaskResult.failure(
                    new RuntimeException("리뷰 파일 저장 실패")
                )
            )
        );

        // when
        asyncReviewService.review(webhook);

        // then
        verify(reviewJobStatusService, never())
            .markSuccess(webhook);

        verify(reviewJobStatusService)
            .markPartialFailed(
                eq(webhook),
                contains("saveReview=failed")
            );
    }

    private PullRequestWebhook createWebhook() {
        return new PullRequestWebhook(
            PullRequestAction.LABELED,
            1,
            "test PR",
            "https://github.com/test/test/pull/1.diff",
            "feature/test",
            "abc123",
            "test/test",
            "ai-review:on"
        );
    }
}
