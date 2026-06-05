package com.hamplz.autocomment.webhook;

import com.hamplz.autocomment.config.GithubProperties;
import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventFilter {
    private static final String REVIEW_TRIGGER_LABEL = "ai-review:on";

    private final GithubProperties githubProperties;

    public WebhookEventFilter(GithubProperties githubProperties) {
        this.githubProperties = githubProperties;
    }

    public boolean isReviewTarget(PullRequestWebhook parsedWebhook) {
        if (parsedWebhook == null) return false;

        if (parsedWebhook.headRef() == null || parsedWebhook.headRef().isBlank()) {
            return false;
        }

        if (githubProperties.reviewBranch()
                .equalsIgnoreCase(parsedWebhook.headRef())) {
            return false;
        }

        // Check if the review trigger label was added.
        String changedLabel = parsedWebhook.changedLabel();
        if (changedLabel == null || changedLabel.isBlank()) {
            return false;
        }

        return REVIEW_TRIGGER_LABEL.equals(changedLabel);
    }
}
