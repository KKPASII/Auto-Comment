package com.hamplz.autocomment;

import com.hamplz.autocomment.config.GithubProperties;
import com.hamplz.autocomment.review.service.AsyncReviewService;
import com.hamplz.autocomment.review.service.PullRequestReviewService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
@Import({WebhookPayloadParser.class, WebhookEventFilter.class})
class WebhookControllerTest {
    private static final String REVIEW_TRIGGER_LABEL = "ai-review:on";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsyncReviewService asyncReviewService;

    @MockitoBean
    private GithubProperties githubProperties;

    @MockitoBean
    private GitHubWebhookSignatureVerifier signatureVerifier;

    @MockitoBean
    private PullRequestReviewService pullRequestReviewService;

    @BeforeEach
    void setUp() {
        given(githubProperties.reviewBranch()).willReturn("auto-comment-logs");
        given(signatureVerifier.isValid(anyString(), org.mockito.ArgumentMatchers.isNull())).willReturn(true);
    }

    @Test
    @DisplayName("ignores configured review-log branch")
    void ignoreAutoCommentLogsBranchEvent() throws Exception {
        String payload = """
            {
              "action": "labeled",
              "number": 15,
              "pull_request": {
                "title": "docs: update review logs",
                "diff_url": "https://example.com/pull/15.diff",
                "head": {
                  "ref": "auto-comment-logs"
                },
                "labels": [
                  { "name": "%s" }
                ]
              },
              "label": {
                "name": "%s"
              },
              "repository": {
                "full_name": "hamplz/auto-comment"
              }
            }
            """.formatted(REVIEW_TRIGGER_LABEL, REVIEW_TRIGGER_LABEL);

        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(content().string("ignored"));

        verifyNoInteractions(asyncReviewService);
    }

    @Test
    @DisplayName("accepts ai-review label event")
    void processNormalPullRequestEvent() throws Exception {
        String payload = """
            {
              "action": "labeled",
              "number": 21,
              "pull_request": {
                "title": "feat: add webhook review flow",
                "diff_url": "https://example.com/pull/21.diff",
                "head": {
                  "ref": "feature/webhook-review"
                },
                "labels": [
                  { "name": "%s" }
                ]
              },
              "label": {
                "name": "%s"
              },
              "repository": {
                "full_name": "hamplz/auto-comment"
              }
            }
            """.formatted(REVIEW_TRIGGER_LABEL, REVIEW_TRIGGER_LABEL);

        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isAccepted())
            .andExpect(content().string("Accepted"));

        verify(asyncReviewService).reviewAsync(argThat(webhook ->
            webhook.prNumber() == 21
                && "hamplz/auto-comment".equals(webhook.repoFullName())
                && REVIEW_TRIGGER_LABEL.equals(webhook.changedLabel())
        ));
    }

    @Test
    @DisplayName("ignores event without review label")
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
                "full_name": "hamplz/auto-comment"
              }
            }
            """;

        mockMvc.perform(post("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(content().string("ignored"));

        verifyNoInteractions(asyncReviewService);
    }
}
