package com.hamplz.autocomment.review.dto;

public record DispatchTaskResult(
    boolean succeeded,
    String errorMessage
) {
    public static DispatchTaskResult success() {
        return new DispatchTaskResult(true, null);
    }

    public static DispatchTaskResult failure(Exception e) {
        return new DispatchTaskResult(false, e.getMessage());
    }
}
