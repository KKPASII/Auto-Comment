package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.springframework.stereotype.Component;

@Component
public class ReviewRedisKeyFactory {

    private static final String KEY_SEPARATOR = ":";
    private static final String UNKNOWN_VALUE = "unknown";
    private static final String DEDUP_KEY_PREFIX = "review:dedup";
    private static final String STATUS_KEY_PREFIX = "review:status";

    public String deduplicationKey(PullRequestWebhook webhook) {
        return buildKey(DEDUP_KEY_PREFIX, webhook);
    }

    public String statusKey(PullRequestWebhook webhook) {
        return buildKey(STATUS_KEY_PREFIX, webhook);
    }

    private String buildKey(String prefix, PullRequestWebhook webhook) {
        return prefix
            + KEY_SEPARATOR + safe(webhook.repoFullName())
            + KEY_SEPARATOR + webhook.prNumber()
            + KEY_SEPARATOR + safe(firstNonBlank(webhook.headSha(), webhook.headRef()))
            + KEY_SEPARATOR + safe(webhook.changedLabel());
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_VALUE;
        }
        return value.replaceAll("\\s+", "_");
    }
}
