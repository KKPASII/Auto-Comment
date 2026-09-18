package com.hamplz.autocomment.github.dto;

import com.hamplz.autocomment.review.dto.DispatchTaskResult;

import static org.springframework.http.ResponseEntity.status;

public record ReviewFileSaveResult(
    DispatchTaskResult historyResult,
    DispatchTaskResult latestResult
) {
    public boolean isFullySucceeded() {
        return historyResult.succeeded()
            && latestResult.succeeded();
    }

    public String summary() {
        return "history=" + status(historyResult)
            + ", latest=" + status(latestResult);
    }

    private String status(DispatchTaskResult result) {
        if (result.succeeded()) {
            return "success";
        }

        return "failed(" + result.errorMessage() + ")";
    }
}
