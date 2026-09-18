package com.hamplz.autocomment.github.service;

import com.hamplz.autocomment.config.GithubProperties;
import com.hamplz.autocomment.github.dto.ReviewFileSaveResult;
import com.hamplz.autocomment.review.ReviewFileFormatter;
import com.hamplz.autocomment.review.dto.DispatchTaskResult;
import com.hamplz.autocomment.support.ExternalApiRetryExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GithubFileServiceTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private GithubProperties githubProperties;

    @Mock
    private ReviewFileFormatter reviewFileFormatter;

    @Mock
    private ExternalApiRetryExecutor retryExecutor;

    private GithubFileService githubFileService;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build())
            .thenReturn(restClient);

        Executor directExecutor = Runnable::run;

        githubFileService = new GithubFileService(
            restClientBuilder,
            githubProperties,
            reviewFileFormatter,
            retryExecutor,
            directExecutor
        );
    }

    @Test
    void history는_성공하고_latest가_실패하면_각각의_결과를_반환한다() {
        // when
        ReviewFileSaveResult result =
            githubFileService.saveFilesInParallel(
                DispatchTaskResult::success,
                () -> DispatchTaskResult.failure(
                    new RuntimeException("latest 저장 실패")
                )
            );

        // then
        assertTrue(
            result.historyResult().succeeded()
        );

        assertFalse(
            result.latestResult().succeeded()
        );

        assertFalse(
            result.isFullySucceeded()
        );

        assertEquals(
            "latest 저장 실패",
            result.latestResult().errorMessage()
        );
    }

    @Test
    void history가_실패하고_latest는_성공하면_각각의_결과를_반환한다() {
        // when
        ReviewFileSaveResult result =
            githubFileService.saveFilesInParallel(
                () -> DispatchTaskResult.failure(
                    new RuntimeException("history 저장 실패")
                ),
                DispatchTaskResult::success
            );

        // then
        assertFalse(
            result.historyResult().succeeded()
        );

        assertTrue(
            result.latestResult().succeeded()
        );

        assertFalse(
            result.isFullySucceeded()
        );

        assertEquals(
            "history 저장 실패",
            result.historyResult().errorMessage()
        );
    }
}
