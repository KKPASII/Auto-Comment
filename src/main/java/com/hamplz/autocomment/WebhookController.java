package com.hamplz.autocomment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @PostMapping("/github")
    public ResponseEntity<String> receive(@RequestBody JsonNode payload) {
        System.out.println("==== GitHub Webhook Received ====");

        String action = payload.path("action").asText();
        int prNumber = payload.path("number").asInt();

        JsonNode pr = payload.path("pull_request");
        String title = pr.path("title").asText();
        String diffUrl = pr.path("diff_url").asText();

        JsonNode repository = payload.path("repository");
        String repoFullName = repository.path("full_name").asText();

        System.out.println("action = " + action);
        System.out.println("prNumber = " + prNumber);
        System.out.println("title = " + title);
        System.out.println("repoFullName = " + repoFullName);
        System.out.println("diffUrl = " + diffUrl);

        if (!isReviewTargetAction(action)) {
            System.out.println("리뷰 대상 action이 아니므로 종료합니다.");
            return ResponseEntity.ok("ignored");
        }

        System.out.println("리뷰 대상 PR 이벤트입니다.");

        return ResponseEntity.ok("OK!: GitHub Webhook Received");
    }

    private boolean isReviewTargetAction(String action) {
        return "opened".equals(action)
            || "synchronize".equals(action)
            || "reopened".equals(action);
    }
}
