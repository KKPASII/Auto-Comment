package com.hamplz.autocomment.support;

import com.hamplz.autocomment.config.RetryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

@Component
public class ExternalApiRetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiRetryExecutor.class);

    private final RetryProperties retryProperties;

    public ExternalApiRetryExecutor(RetryProperties retryProperties) {
        this.retryProperties = retryProperties;
    }

    public <T> T execute(ExternalApiOperation operation, Supplier<T> supplier) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= retryProperties.maxAttempts(); attempt++) {
            try {
                return supplier.get();
            } catch (RuntimeException e) {
                lastFailure = e;
                if (attempt >= retryProperties.maxAttempts() || !isRetryable(e)) {
                    throw e;
                }
                log.warn("{} failed. retrying attempt {}/{}", operation.logName(), attempt + 1, retryProperties.maxAttempts(), e);
                sleepBeforeRetry();
            }
        }

        throw lastFailure;
    }

    public void execute(ExternalApiOperation operation, Runnable runnable) {
        execute(operation, () -> {
            runnable.run();
            return null;
        });
    }

    private boolean isRetryable(RuntimeException e) {
        if (e instanceof RestClientResponseException responseException) {
            int statusCode = responseException.getStatusCode().value();
            return statusCode == 429 || statusCode >= 500;
        }
        return true;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(retryProperties.backoff().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", e);
        }
    }
}
