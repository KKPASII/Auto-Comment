package com.hamplz.autocomment.support;

import java.util.Locale;

public enum ExternalApiOperation {
    GITHUB_CREATE_COMMENT,
    GITHUB_GET_PR_DIFF,
    GITHUB_SAVE_REVIEW_HISTORY_FILE,
    GITHUB_SAVE_REVIEW_LATEST_FILE,
    GITHUB_GET_REVIEW_FILE_SHA,
    GITHUB_CHECK_REVIEW_BRANCH,
    GITHUB_CREATE_REVIEW_BRANCH,
    GITHUB_GET_BASE_BRANCH_SHA,
    OPENAI_GENERATE_REVIEW;

    public String logName() {
        return name().toLowerCase(Locale.ROOT).replace('_', '.');
    }
}
