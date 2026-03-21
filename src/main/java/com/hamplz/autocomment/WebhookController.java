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
    private static final String AUTO_COMMENT_BRANCH_NAME = "auto-comment-logs";

    private final GithubDiffService githubDiffService;
    private final GptReviewService gptReviewService;
    private final GithubCommentService githubCommentService;
    private final GithubFileService githubFileService;

    public WebhookController(GithubDiffService githubDiffService, GptReviewService gptReviewService, GithubCommentService githubCommentService, GithubFileService githubFileService) {
        this.githubDiffService = githubDiffService;
        this.gptReviewService = gptReviewService;
        this.githubCommentService = githubCommentService;
        this.githubFileService = githubFileService;
    }

    @PostMapping("/github")
    public ResponseEntity<String> receive(@RequestBody JsonNode payload) {
        log.info("==== GitHub Webhook Received ====");

        String action = payload.path("action").asText();
        int prNumber = payload.path("number").asInt();

        JsonNode pr = payload.path("pull_request");
        if (pr.isMissingNode()) {
            log.warn("pull_request 없음 -> 무시");
            return ResponseEntity.ok("ignored");
        }
        String title = pr.path("title").asText();
        String diffUrl = pr.path("diff_url").asText();
        String headRef = pr.path("head").path("ref").asText();

        JsonNode repository = payload.path("repository");
        String repoFullName = repository.path("full_name").asText();

        log.info("action = {}", action);
        log.info("prNumber = {}", prNumber);
        log.info("title = {}", title);
        log.info("repoFullName = {}", repoFullName);
        log.info("diffUrl = {}", diffUrl);
        log.info("headRef = {}", headRef);

        if (AUTO_COMMENT_BRANCH_NAME.equals(headRef)) {
            log.info("자동 생성 브랜치 이벤트 무시됨");
            return ResponseEntity.ok("ignored");
        }

        if (!isReviewTargetAction(action)) {
            log.info("리뷰 대상 action이 아니므로 종료합니다.");
            return ResponseEntity.ok("ignored");
        }

        log.info("리뷰 대상 PR 이벤트입니다.");

        try {
            String diffContent = githubDiffService.getPullRequestDiff(diffUrl);
            log.info("\n==== PR DIFF START ====\n{}\n==== PR DIFF END ====\n", diffContent);

            String reviewComment = gptReviewService.generateReview(diffContent);
            log.info("\n==== GPT REVIEW START ====\n{}\n==== GPT REVIEW END ====\n", reviewComment);

            githubCommentService.createComment(repoFullName, prNumber, reviewComment);

            githubFileService.saveReviewFile(
                repoFullName,
                prNumber,
                title,
                action,
                reviewComment
            );

        } catch (Exception e) {
            log.error("==== PR 처리 실패 ====", e);
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
