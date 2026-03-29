package com.hamplz.autocomment;

import com.fasterxml.jackson.databind.JsonNode;
import com.hamplz.autocomment.dto.PullRequestWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WebhookPayloadParser {

    private static final Logger log = LoggerFactory.getLogger(WebhookPayloadParser.class);

    public PullRequestWebhook parse(JsonNode webhookPayload) {

        String action = webhookPayload.path("action").asText();
        int prNumber = webhookPayload.path("number").asInt();

        JsonNode pr = webhookPayload.path("pull_request");
        if (pr.isMissingNode()) {
            return new PullRequestWebhook(
                action,
                prNumber,
                null,
                null,
                null,
                null
            );
        }

        String title = pr.path("title").asText();
        String diffUrl = pr.path("diff_url").asText();
        String headRef = pr.path("head").path("ref").asText();
        String repoFullName = webhookPayload.path("repository").path("full_name").asText();

        log.info("action = {}", action);
        log.info("prNumber = {}", prNumber);
        log.info("title = {}", title);
        log.info("repoFullName = {}", repoFullName);
        log.info("diffUrl = {}", diffUrl);
        log.info("headRef = {}", headRef);

        return new PullRequestWebhook(
            action,
            prNumber,
            title,
            diffUrl,
            headRef,
            repoFullName
        );
    }
}
