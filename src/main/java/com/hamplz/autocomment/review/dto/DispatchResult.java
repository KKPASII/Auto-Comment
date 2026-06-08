package com.hamplz.autocomment.review.dto;

public record DispatchResult(
    DispatchTaskResult commentResult,
    DispatchTaskResult saveReviewResult
) {
    public boolean isFullySucceeded() {
        return commentResult.succeeded() && saveReviewResult.succeeded();
    }

    public String summary() {
        return "comment=" + status(commentResult)
            + ", saveReview=" + status(saveReviewResult);
    }

    private static String status(DispatchTaskResult result) {
        if (result.succeeded()) {
            return "success";
        }
        return "failed(" + result.errorMessage() + ")";
    }
}
