package com.hamplz.autocomment.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamplz.autocomment.review.service.ReviewJobQueueService;
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
    private static final String LOG_WEBHOOK_RECEIVED = "GitHub webhook received";
    private static final String LOG_INVALID_SIGNATURE = "GitHub webhook signature is invalid.";
    private static final String LOG_INVALID_PAYLOAD = "GitHub webhook payload is invalid.";
    private static final String LOG_IGNORED = "GitHub webhook ignored - {} PR #{}";
    private static final String LOG_REVIEW_REQUEST_RECEIVED = "GitHub webhook review request received - {} PR #{}";
    private static final String LOG_DUPLICATED_REVIEW_REQUEST = "Duplicated review request ignored - {} PR #{}";

    private final ObjectMapper objectMapper;
    private final ReviewJobQueueService reviewJobQueueService;
    private final WebhookPayloadParser webhookPayloadParser;
    private final WebhookEventFilter webhookEventFilter;
    private final GitHubWebhookSignatureVerifier signatureVerifier;
    private final ReviewRequestDeduplicationService reviewRequestDeduplicationService;

    public WebhookController(
        ObjectMapper objectMapper,
        ReviewJobQueueService reviewJobQueueService,
        WebhookPayloadParser webhookPayloadParser,
        WebhookEventFilter webhookEventFilter,
        GitHubWebhookSignatureVerifier signatureVerifier,
        ReviewRequestDeduplicationService reviewRequestDeduplicationService
    ) {
        this.objectMapper = objectMapper;
        this.reviewJobQueueService = reviewJobQueueService;
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

        log.info(LOG_WEBHOOK_RECEIVED);

        if (!signatureVerifier.isValid(payload, signatureHeader)) {
            log.info(LOG_INVALID_SIGNATURE);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(RESPONSE_INVALID_SIGNATURE);
        }

        JsonNode parsedPayload;
        try {
            parsedPayload = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            log.info(LOG_INVALID_PAYLOAD, e);
            return ResponseEntity.badRequest().body(RESPONSE_INVALID_PAYLOAD);
        }

        PullRequestWebhook parsedWebhook = webhookPayloadParser.parse(parsedPayload);

        if (!webhookEventFilter.isReviewTarget(parsedWebhook)) {
            log.info(LOG_IGNORED, parsedWebhook.repoFullName(), parsedWebhook.prNumber());
            return ResponseEntity.ok(RESPONSE_IGNORED);
        }

        log.info(LOG_REVIEW_REQUEST_RECEIVED,
            parsedWebhook.repoFullName(),
            parsedWebhook.prNumber()
        );

        if (!reviewRequestDeduplicationService.tryStart(parsedWebhook)) {
            log.info(LOG_DUPLICATED_REVIEW_REQUEST,
                parsedWebhook.repoFullName(),
                parsedWebhook.prNumber()
            );
            return ResponseEntity.ok(RESPONSE_DUPLICATED);
        }

        reviewJobQueueService.enqueue(parsedWebhook);

        return ResponseEntity.accepted().body(RESPONSE_ACCEPTED);
    }
}