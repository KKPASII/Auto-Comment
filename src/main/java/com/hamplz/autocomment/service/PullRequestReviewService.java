package com.hamplz.autocomment.service;

import com.hamplz.autocomment.dto.PullRequestWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PullRequestReviewService {

    private static final Logger log = LoggerFactory.getLogger(PullRequestReviewService.class);

    private final GithubDiffService githubDiffService;
    private final GithubFileService githubFileService;
    private final GithubCommentService githubCommentService;
    private final GptReviewService gptReviewService;

    public PullRequestReviewService(GithubDiffService githubDiffService, GithubFileService githubFileService, GithubCommentService githubCommentService, GptReviewService gptReviewService) {
        this.githubDiffService = githubDiffService;
        this.githubFileService = githubFileService;
        this.githubCommentService = githubCommentService;
        this.gptReviewService = gptReviewService;
    }

    public void review(PullRequestWebhook webhook) {

        try {
            String diffContent = githubDiffService.getPullRequestDiff(webhook.diffUrl());
            log.info("\n==== PR DIFF START ====\n{}\n==== PR DIFF END ====\n", diffContent);

            String reviewComment = gptReviewService.generateReview(diffContent);
            log.info("\n==== GPT REVIEW START ====\n{}\n==== GPT REVIEW END ====\n", reviewComment);

            githubCommentService.createComment(webhook.repoFullName(), webhook.prNumber(), reviewComment);

            githubFileService.saveReviewFile(
                webhook.repoFullName(),
                webhook.prNumber(),
                webhook.title(),
                webhook.action().getAction(),
                reviewComment
            );
        } catch(Exception e) {
            log.error("==== PR 코멘트 작성 실패 ====", e);
        }

    }
}
