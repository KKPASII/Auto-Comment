package com.hamplz.autocomment;

import com.fasterxml.jackson.databind.JsonNode;
import com.hamplz.autocomment.dto.PullRequestWebhook;
import com.hamplz.autocomment.service.GithubCommentService;
import com.hamplz.autocomment.service.GithubDiffService;
import com.hamplz.autocomment.service.GithubFileService;
import com.hamplz.autocomment.service.GptReviewService;
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
    private final WebhookPayloadParser webhookPayloadParser;

    public WebhookController(GithubDiffService githubDiffService, GptReviewService gptReviewService, GithubCommentService githubCommentService, GithubFileService githubFileService, WebhookPayloadParser webhookPayloadParser) {
        this.githubDiffService = githubDiffService;
        this.gptReviewService = gptReviewService;
        this.githubCommentService = githubCommentService;
        this.githubFileService = githubFileService;
        this.webhookPayloadParser = webhookPayloadParser;
    }

    @PostMapping("/github")
    public ResponseEntity<String> receive(@RequestBody JsonNode payload) {
        log.info("==== GitHub Webhook Received ====");

        PullRequestWebhook parsedWebhook = webhookPayloadParser.parse(payload);

        if (AUTO_COMMENT_BRANCH_NAME.equals(parsedWebhook.headRef())) {
            log.info("자동 생성 브랜치 이벤트 무시됨");
            return ResponseEntity.ok("ignored");
        }

        if (!isReviewTargetAction(parsedWebhook.action().getAction())) {
            log.info("리뷰 대상 action이 아니므로 종료합니다.");
            return ResponseEntity.ok("ignored");
        }

        log.info("리뷰 대상 PR 이벤트입니다.");

        try {
            String diffContent = githubDiffService.getPullRequestDiff(parsedWebhook.diffUrl());
            log.info("\n==== PR DIFF START ====\n{}\n==== PR DIFF END ====\n", diffContent);

            String reviewComment = gptReviewService.generateReview(diffContent);
            log.info("\n==== GPT REVIEW START ====\n{}\n==== GPT REVIEW END ====\n", reviewComment);

            githubCommentService.createComment(parsedWebhook.repoFullName(), parsedWebhook.prNumber(), reviewComment);

            githubFileService.saveReviewFile(
                parsedWebhook.repoFullName(),
                parsedWebhook.prNumber(),
                parsedWebhook.title(),
                parsedWebhook.action().getAction(),
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
