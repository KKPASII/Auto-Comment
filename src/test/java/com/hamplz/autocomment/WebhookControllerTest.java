package com.hamplz.autocomment;

import com.hamplz.autocomment.config.GithubProperties;
import com.hamplz.autocomment.review.service.ReviewJobQueueService;
import com.hamplz.autocomment.review.service.PullRequestReviewService;
import com.hamplz.autocomment.review.service.ReviewRequestDeduplicationService;
import com.hamplz.autocomment.webhook.GitHubWebhookSignatureVerifier;
import com.hamplz.autocomment.webhook.WebhookController;
import com.hamplz.autocomment.webhook.WebhookEventFilter;
import com.hamplz.autocomment.webhook.WebhookPayloadParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
@Import({
    WebhookPayloadParser.class,
    WebhookEventFilter.class
})
class WebhookControllerTest {
    private static final String REVIEW_TRIGGER_LABEL = "ai-review:on";

    private static final String SIGNATURE_HEADER = "sha256=test-signature";
    private static final String INVALID_SIGNATURE_HEADER = "sha256=invalid";

    private static final String REVIEW_BRANCH = "auto-comment-logs";
    private static final String REPOSITORY = "aaaa/auto-comment";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewJobQueueService reviewJobQueueService;

    @MockitoBean
    private GithubProperties githubProperties;

    @MockitoBean
    private GitHubWebhookSignatureVerifier signatureVerifier;

    @MockitoBean
    private ReviewRequestDeduplicationService reviewRequestDeduplicationService;

    @MockitoBean
    private PullRequestReviewService pullRequestReviewService;

    @BeforeEach
    void setUp() {
        given(githubProperties.reviewBranch())
            .willReturn(REVIEW_BRANCH);

        given(signatureVerifier.isValid(
            any(byte[].class),
            eq(SIGNATURE_HEADER)
        )).willReturn(true);

        given(reviewRequestDeduplicationService.tryStart(any()))
            .willReturn(true);
    }

    @Test
    @DisplayName("리뷰 로그 브랜치의 웹훅은 무시한다")
    void ignoreAutoCommentLogsBranchEvent() throws Exception {
        String payload = createLabeledPayload(
            15,
            "docs: update review logs",
            REVIEW_BRANCH,
            "abc123"
        );

        performWebhook(payload)
            .andExpect(status().isOk())
            .andExpect(content().string("ignored"));

        verifyNoInteractions(reviewJobQueueService);
    }

    @Test
    @DisplayName("ai-review 라벨이 추가된 PR 웹훅은 리뷰 큐에 등록한다")
    void processNormalPullRequestEvent() throws Exception {
        String payload = createLabeledPayload(
            21,
            "feat: add webhook review flow",
            "feature/webhook-review",
            "abc123"
        );

        performWebhook(payload)
            .andExpect(status().isAccepted())
            .andExpect(content().string("Accepted"));

        verify(reviewJobQueueService).enqueue(argThat(webhook ->
            webhook.prNumber() == 21
                && REPOSITORY.equals(webhook.repoFullName())
                && "abc123".equals(webhook.headSha())
                && REVIEW_TRIGGER_LABEL.equals(webhook.changedLabel())
        ));
    }

    @Test
    @DisplayName("유효하지 않은 서명의 웹훅은 거부한다")
    void rejectInvalidSignature() throws Exception {
        String payload = """
            {
                "action" : "labeled",
                "number" : 21
            }
            """;

        given(signatureVerifier.isValid(
            any(byte[].class),
            eq(INVALID_SIGNATURE_HEADER)
        )).willReturn(false);

        performWebhook(payload, INVALID_SIGNATURE_HEADER)
            .andExpect(status().isUnauthorized())
            .andExpect(content().string("invalid signature"));

        verifyNoInteractions(reviewJobQueueService);
    }

    @Test
    @DisplayName("서명이 없는 웹훅은 거부한다")
    void acceptInvalidSignature() throws Exception {
        String payload = """
        {
          "action": "labeled",
          "number": 21
        }
        """;

        given(signatureVerifier.isValid(
            any(byte[].class),
            isNull()
        )).willReturn(false);

        performWebhookWithoutSignature(payload)
            .andExpect(status().isUnauthorized())
            .andExpect(content().string("invalid signature"));

        verifyNoInteractions(reviewJobQueueService);
    }

    @Test
    @DisplayName("이미 처리 중인 리뷰 요청은 중복 등록하지 않는다")
    void ignoreDuplicatedReviewRequest() throws Exception {
        given(reviewRequestDeduplicationService.tryStart(any()))
            .willReturn(false);

        String payload = createLabeledPayload(
            21,
            "feat: add webhook review flow",
            "feature/webhook-review",
            "abc123"
        );

        performWebhook(payload)
            .andExpect(status().isOk())
            .andExpect(content().string("duplicated"));

        verifyNoInteractions(reviewJobQueueService);
    }

    @Test
    @DisplayName("ai-review 라벨 제거 이벤트는 무시한다")
    void ignoreReviewLabelRemovalEvent() throws Exception {
        String payload = """
            {
              "action": "unlabeled",
              "number": 21,
              "pull_request": {
                "title": "feat: add webhook review flow",
                "diff_url": "https://example.com/pull/21.diff",
                "head": {
                  "ref": "feature/webhook-review",
                  "sha": "abc123"
                },
                "labels": []
              },
              "label": {
                "name": "%s"
              },
              "repository": {
                "full_name": "%s"
              }
            }
            """.formatted(REVIEW_TRIGGER_LABEL, REPOSITORY);

        performWebhook(payload)
            .andExpect(status().isOk())
            .andExpect(content().string("ignored"));

        verifyNoInteractions(reviewJobQueueService);
    }

    @Test
    @DisplayName("리뷰 대상이 아닌 PR 이벤트는 무시한다")
    void ignoreNonReviewTargetAction() throws Exception {
        String payload = """
            {
              "action": "closed",
              "number": 30,
              "pull_request": {
                "title": "chore: close old pr",
                "diff_url": "https://example.com/pull/30.diff",
                "head": {
                  "ref": "feature/cleanup"
                }
              },
              "repository": {
                "full_name": "%s"
              }
            }
            """.formatted(REPOSITORY);

        performWebhook(payload)
            .andExpect(status().isOk())
            .andExpect(content().string("ignored"));

        verifyNoInteractions(reviewJobQueueService);
    }

    private ResultActions performWebhook(String payload) throws Exception {
        return performWebhook(payload, SIGNATURE_HEADER);
    }

    private ResultActions performWebhook(
        String payload,
        String signatureHeader
    ) throws Exception {

        return mockMvc.perform(post("/webhook/github")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Hub-Signature-256", signatureHeader)
            .content(payload));
    }

    private ResultActions performWebhookWithoutSignature(
        String payload
    ) throws Exception {

        return mockMvc.perform(post("/webhook/github")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    private String createLabeledPayload(
        int prNumber,
        String title,
        String headRef,
        String headSha
    ) {

        return """
            {
              "action": "labeled",
              "number": %d,
              "pull_request": {
                "title": "%s",
                "diff_url": "https://example.com/pull/%d.diff",
                "head": {
                  "ref": "%s",
                  "sha": "%s"
                },
                "labels": [
                  { "name": "%s" }
                ]
              },
              "label": {
                "name": "%s"
              },
              "repository": {
                "full_name": "%s"
              }
            }
            """.formatted(
            prNumber,
            title,
            prNumber,
            headRef,
            headSha,
            REVIEW_TRIGGER_LABEL,
            REVIEW_TRIGGER_LABEL,
            REPOSITORY
        );
    }
}
