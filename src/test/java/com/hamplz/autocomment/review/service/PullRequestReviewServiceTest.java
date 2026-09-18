package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.github.service.GithubDiffService;
import com.hamplz.autocomment.openai.GptReviewService;
import com.hamplz.autocomment.review.dto.DispatchResult;
import com.hamplz.autocomment.review.dto.DispatchTaskResult;
import com.hamplz.autocomment.webhook.dto.PullRequestAction;
import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PullRequestReviewServiceTest {

    @Mock
    private GithubDiffService githubDiffService;

    @Mock
    private GptReviewService gptReviewService;

    @Mock
    private AsyncResultDispatchService asyncResultDispatchService;

    private PullRequestReviewService pullRequestReviewService;

    @BeforeEach
    void setUp() {
        pullRequestReviewService =
            new PullRequestReviewService(
                githubDiffService,
                gptReviewService,
                asyncResultDispatchService
            );
    }

    @Test
    void 리뷰파일만_실패하면_comment는_retry하지_않고_리뷰파일만_retry한다() {
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
            ),
            CompletableFuture.completedFuture(
                DispatchTaskResult.success()
            )
        );

        // when
        DispatchResult result =
            pullRequestReviewService.review(webhook);

        // then
        verify(
            asyncResultDispatchService,
            times(1)
        ).commentAsync(
            anyString(),
            anyInt(),
            anyString()
        );

        verify(
            asyncResultDispatchService,
            times(2)
        ).saveReviewAsync(
            anyString(),
            anyInt(),
            anyString(),
            anyString(),
            anyString()
        );

        assertTrue(
            result.isFullySucceeded()
        );
    }

    @Test
    void comment만_실패하면_리뷰파일은_retry하지_않고_comment만_retry한다() {
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
                DispatchTaskResult.failure(
                    new RuntimeException("댓글 등록 실패")
                )
            ),
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
                DispatchTaskResult.success()
            )
        );

        // when
        DispatchResult result =
            pullRequestReviewService.review(webhook);

        // then
        verify(
            asyncResultDispatchService,
            times(2)
        ).commentAsync(
            anyString(),
            anyInt(),
            anyString()
        );

        verify(
            asyncResultDispatchService,
            times(1)
        ).saveReviewAsync(
            anyString(),
            anyInt(),
            anyString(),
            anyString(),
            anyString()
        );

        assertTrue(
            result.isFullySucceeded()
        );
    }

    @Test
    void 리뷰파일이_retry까지_실패하면_최종결과도_실패한다() {
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
                    new RuntimeException("1차 실패")
                )
            ),
            CompletableFuture.completedFuture(
                DispatchTaskResult.failure(
                    new RuntimeException("retry 실패")
                )
            )
        );

        DispatchResult result =
            pullRequestReviewService.review(webhook);

        verify(asyncResultDispatchService, times(1))
            .commentAsync(
                anyString(),
                anyInt(),
                anyString()
            );

        verify(asyncResultDispatchService, times(2))
            .saveReviewAsync(
                anyString(),
                anyInt(),
                anyString(),
                anyString(),
                anyString()
            );

        assertFalse(result.isFullySucceeded());
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
