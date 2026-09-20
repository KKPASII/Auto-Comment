package com.hamplz.autocomment.github.service;

import com.hamplz.autocomment.config.GithubProperties;
import com.hamplz.autocomment.github.dto.ReviewFileSaveResult;
import com.hamplz.autocomment.review.ReviewFileFormatter;
import com.hamplz.autocomment.review.dto.DispatchTaskResult;
import com.hamplz.autocomment.support.ExternalApiRetryExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GithubFileServiceTest {

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

        githubFileService = new GithubFileService(
            restClientBuilder,
            githubProperties,
            reviewFileFormatter,
            retryExecutor
        );
    }

    @Test
    @DisplayName("History 저장 후 latest를 순차적으로 저장한다")
    void shouldSaveHistoryBeforeLatest() {
        // given
        List<String> executionOrder = new ArrayList<>();

        Supplier<DispatchTaskResult> historyTask = () -> {
            executionOrder.add("history");
            return DispatchTaskResult.success();
        };

        Supplier<DispatchTaskResult> latestTask = () -> {
            executionOrder.add("latest");
            return DispatchTaskResult.success();
        };

        // when
        ReviewFileSaveResult result =
            githubFileService.saveFilesSequentially(
                historyTask,
                latestTask
            );

        // then
        assertEquals(
            List.of("history", "latest"),
            executionOrder
        );

        assertTrue(result.isFullySucceeded());
    }

    @Test
    @DisplayName("History는 성공하고 latest가 실패하면 각각의 결과를 반환한다")
    void shouldReturnHistorySuccessAndLatestFailure() {
        // when
        ReviewFileSaveResult result =
            githubFileService.saveFilesSequentially(
                DispatchTaskResult::success,
                () -> DispatchTaskResult.failure(
                    new RuntimeException("latest 저장 실패")
                )
            );

        // then
        assertTrue(result.historyResult().succeeded());
        assertFalse(result.latestResult().succeeded());
        assertFalse(result.isFullySucceeded());

        assertEquals(
            "latest 저장 실패",
            result.latestResult().errorMessage()
        );
    }

    @Test
    @DisplayName("History가 실패하고 latest는 성공하면 각각의 결과를 반환한다")
    void shouldReturnHistoryFailureAndLatestSuccess() {
        // when
        ReviewFileSaveResult result =
            githubFileService.saveFilesSequentially(
                () -> DispatchTaskResult.failure(
                    new RuntimeException("history 저장 실패")
                ),
                DispatchTaskResult::success
            );

        // then
        assertFalse(result.historyResult().succeeded());
        assertTrue(result.latestResult().succeeded());
        assertFalse(result.isFullySucceeded());

        assertEquals(
            "history 저장 실패",
            result.historyResult().errorMessage()
        );
    }

    @Test
    @DisplayName("latest만 실패하면 History는 재시도하지 않고 latest만 재시도한다")
    void shouldRetryOnlyLatestWhenLatestFails() {
        // given
        AtomicInteger historyCount = new AtomicInteger();
        AtomicInteger latestCount = new AtomicInteger();

        Supplier<DispatchTaskResult> historyTask = () -> {
            historyCount.incrementAndGet();
            return DispatchTaskResult.success();
        };

        Supplier<DispatchTaskResult> latestTask = () -> {
            int count = latestCount.incrementAndGet();

            if (count == 1) {
                return DispatchTaskResult.failure(
                    new RuntimeException("latest 저장 실패")
                );
            }

            return DispatchTaskResult.success();
        };

        // when
        ReviewFileSaveResult firstResult =
            githubFileService.saveFilesSequentially(
                historyTask,
                latestTask
            );

        ReviewFileSaveResult finalResult =
            githubFileService.retryFailedFileTask(
                firstResult,
                historyTask,
                latestTask
            );

        // then
        assertEquals(1, historyCount.get());
        assertEquals(2, latestCount.get());

        assertTrue(finalResult.historyResult().succeeded());
        assertTrue(finalResult.latestResult().succeeded());
        assertTrue(finalResult.isFullySucceeded());
    }

    @Test
    @DisplayName("History만 실패하면 latest는 재시도하지 않고 History만 재시도한다")
    void shouldRetryOnlyHistoryWhenHistoryFails() {
        // given
        AtomicInteger historyCount = new AtomicInteger();
        AtomicInteger latestCount = new AtomicInteger();

        Supplier<DispatchTaskResult> historyTask = () -> {
            int count = historyCount.incrementAndGet();

            if (count == 1) {
                return DispatchTaskResult.failure(
                    new RuntimeException("history 저장 실패")
                );
            }

            return DispatchTaskResult.success();
        };

        Supplier<DispatchTaskResult> latestTask = () -> {
            latestCount.incrementAndGet();
            return DispatchTaskResult.success();
        };

        // when
        ReviewFileSaveResult firstResult =
            githubFileService.saveFilesSequentially(
                historyTask,
                latestTask
            );

        ReviewFileSaveResult finalResult =
            githubFileService.retryFailedFileTask(
                firstResult,
                historyTask,
                latestTask
            );

        // then
        assertEquals(2, historyCount.get());
        assertEquals(1, latestCount.get());

        assertTrue(finalResult.historyResult().succeeded());
        assertTrue(finalResult.latestResult().succeeded());
        assertTrue(finalResult.isFullySucceeded());
    }
}