package com.hamplz.autocomment;

import com.fasterxml.jackson.databind.JsonNode;
import com.hamplz.autocomment.dto.PullRequestWebhook;
import com.hamplz.autocomment.service.PullRequestReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookPayloadParser webhookPayloadParser;
    private final WebhookEventFilter webhookEventFilter;
    private final PullRequestReviewService pullRequestReviewService;

    public WebhookController(WebhookPayloadParser webhookPayloadParser, WebhookEventFilter webhookEventFilter, PullRequestReviewService pullRequestReviewService) {
        this.webhookPayloadParser = webhookPayloadParser;
        this.webhookEventFilter = webhookEventFilter;
        this.pullRequestReviewService = pullRequestReviewService;
    }

    @PostMapping("/github")
    public ResponseEntity<String> receive(@RequestBody JsonNode payload) {

        log.info("==== GitHub Webhook Received ====");
        PullRequestWebhook parsedWebhook = webhookPayloadParser.parse(payload);

        if (!webhookEventFilter.isReviewTarget(parsedWebhook)) {
            log.info("리뷰 대상이 아니므로 종료합니다.");
            return ResponseEntity.ok("ignored");
        }
        log.info("리뷰 대상 PR 이벤트입니다.");

        pullRequestReviewService.review(parsedWebhook);

        return ResponseEntity.ok("OK!: GitHub Webhook Received");
    }
}
