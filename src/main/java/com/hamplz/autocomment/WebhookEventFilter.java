package com.hamplz.autocomment;

import com.hamplz.autocomment.config.GithubProperties;
import com.hamplz.autocomment.dto.PullRequestAction;
import com.hamplz.autocomment.dto.PullRequestWebhook;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventFilter {
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

        PullRequestAction action = parsedWebhook.action();

        return action != null && action.isReviewTarget();
    }
}
