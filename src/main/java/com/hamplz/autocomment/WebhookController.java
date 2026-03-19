package com.hamplz.autocomment;

import com.fasterxml.jackson.databind.JsonNode;
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

    private final GithubDiffService githubDiffService;

    public WebhookController(GithubDiffService githubDiffService) {
        this.githubDiffService = githubDiffService;
    }

    @PostMapping("/github")
    public ResponseEntity<String> receive(@RequestBody JsonNode payload) {
        log.info("==== GitHub Webhook Received ====");

        String action = payload.path("action").asText();
        int prNumber = payload.path("number").asInt();

        JsonNode pr = payload.path("pull_request");
        String title = pr.path("title").asText();
        String diffUrl = pr.path("diff_url").asText();

        JsonNode repository = payload.path("repository");
        String repoFullName = repository.path("full_name").asText();

        log.info("action = {}", action);
        log.info("prNumber = {}", prNumber);
        log.info("title = {}", title);
        log.info("repoFullName = {}", repoFullName);
        log.info("diffUrl = {}", diffUrl);

        if (!isReviewTargetAction(action)) {
            log.info("리뷰 대상 action이 아니므로 종료합니다.");
            return ResponseEntity.ok("ignored");
        }

        log.info("리뷰 대상 PR 이벤트입니다.");

        try {
            String diffContent = githubDiffService.getPullRequestDiff(diffUrl);

            log.info("==== PR DIFF START ====\n{}\n==== PR DIFF END ====", diffContent);

        } catch (Exception e) {
            log.error("==== DIFF 조회 실패 ====", e);
        }

        return ResponseEntity.ok("OK!: GitHub Webhook Received");
    }

    private boolean isReviewTargetAction(String action) {
        return switch (action) {
            case "opened", "synchronize", "reopened" -> true;
            default -> false;
        };
    }
}
