package com.hamplz.autocomment.dto;

import java.util.Arrays;

public enum PullRequestAction {

    OPENED("opened"),
    SYNCHRONIZE("synchronize"),
    REOPENED("reopened"),
    CLOSED("closed"),
    UNKNOWN("unknown");

    private final String action;

    PullRequestAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
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
