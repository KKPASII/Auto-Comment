package com.hamplz.autocomment.review.dto;

import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;

public record QueuedReviewJob(
    PullRequestWebhook webhook,
    String rawPayload
) {
}
