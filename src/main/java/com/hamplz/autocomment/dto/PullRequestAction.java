package com.hamplz.autocomment.dto;

import java.util.Arrays;

public enum PullRequestAction {

    OPENED("opened", true),
    SYNCHRONIZE("synchronize", true),
    REOPENED("reopened", true),
    CLOSED("closed", false),
    UNKNOWN("unknown", false);

    private final String action;
    private final boolean reviewTarget;

    PullRequestAction(String action, boolean reviewTarget) {
        this.action = action;
        this.reviewTarget = reviewTarget;
    }

    public String getAction() {
        return action;
    }

    public boolean isReviewTarget() {
        return reviewTarget;
    }

    public static PullRequestAction from(String action) {
        if (action == null) {
            return UNKNOWN;
        }

        return Arrays.stream(values())
            .filter(a -> a.action.equalsIgnoreCase(action))
            .findFirst()
            .orElse(UNKNOWN);
    }
}
