package com.hamplz.autocomment.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamplz.autocomment.review.service.AsyncReviewService;
import com.hamplz.autocomment.review.service.ReviewRequestDeduplicationService;
import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private static final String RESPONSE_INVALID_SIGNATURE = "invalid signature";
    private static final String RESPONSE_INVALID_PAYLOAD = "invalid payload";
    private static final String RESPONSE_IGNORED = "ignored";
    private static final String RESPONSE_DUPLICATED = "duplicated";
    private static final String RESPONSE_ACCEPTED = "Accepted";

    private final ObjectMapper objectMapper;
    private final AsyncReviewService asyncReviewService;
    private final WebhookPayloadParser webhookPayloadParser;
    private final WebhookEventFilter webhookEventFilter;
    private final GitHubWebhookSignatureVerifier signatureVerifier;
    private final ReviewRequestDeduplicationService reviewRequestDeduplicationService;

    public WebhookController(
        ObjectMapper objectMapper,
        AsyncReviewService asyncReviewService,
        WebhookPayloadParser webhookPayloadParser,
        WebhookEventFilter webhookEventFilter,
        GitHubWebhookSignatureVerifier signatureVerifier,
        ReviewRequestDeduplicationService reviewRequestDeduplicationService
    ) {
        this.objectMapper = objectMapper;
        this.asyncReviewService = asyncReviewService;
        this.webhookPayloadParser = webhookPayloadParser;
        this.webhookEventFilter = webhookEventFilter;
        this.signatureVerifier = signatureVerifier;
        this.reviewRequestDeduplicationService = reviewRequestDeduplicationService;
    }

    @PostMapping("/github")
    public ResponseEntity<String> receive(
        @RequestBody String payload,
        @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader
    ) {

        log.info("==== GitHub Webhook Received ====");

        if (!signatureVerifier.isValid(payload, signatureHeader)) {
            log.info("GitHub webhook signature is invalid.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(RESPONSE_INVALID_SIGNATURE);
        }

        JsonNode parsedPayload;
        try {
            parsedPayload = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            log.info("GitHub webhook payload is invalid.", e);
            return ResponseEntity.badRequest().body(RESPONSE_INVALID_PAYLOAD);
        }

        PullRequestWebhook parsedWebhook = webhookPayloadParser.parse(parsedPayload);

        if (!webhookEventFilter.isReviewTarget(parsedWebhook)) {
            log.info("리뷰 대상이 아니므로 종료합니다.");
            return ResponseEntity.ok(RESPONSE_IGNORED);
        }

        log.info("webhook 수신 - {} PR #{}",
            parsedWebhook.repoFullName(),
            parsedWebhook.prNumber()
        );

        if (!reviewRequestDeduplicationService.tryStart(parsedWebhook)) {
            log.info("Duplicated review request ignored - {} PR #{}",
                parsedWebhook.repoFullName(),
                parsedWebhook.prNumber()
            );
            return ResponseEntity.ok(RESPONSE_DUPLICATED);
        }

        asyncReviewService.reviewAsync(parsedWebhook);

        return ResponseEntity.accepted().body(RESPONSE_ACCEPTED);
    }
}
