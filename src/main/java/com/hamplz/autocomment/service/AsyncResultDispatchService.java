package com.hamplz.autocomment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncResultDispatchService {

    private static final Logger log = LoggerFactory.getLogger(AsyncResultDispatchService.class);

    private final GithubFileService githubFileService;
    private final GithubCommentService githubCommentService;

    public AsyncResultDispatchService(GithubFileService githubFileService, GithubCommentService githubCommentService) {
        this.githubFileService = githubFileService;
        this.githubCommentService = githubCommentService;
    }

    @Async("dispatchTaskExecutor")
    public CompletableFuture<Void> commentAsync(
        String repoFullName,
        int prNumber,
        String reviewComment
    ) {
        try {
            log.info("코멘트 등록 시작 - {} PR #{}", repoFullName, prNumber);
            githubCommentService.createComment(repoFullName, prNumber, reviewComment);
            log.info("코멘트 등록 완료 - {} PR #{}", repoFullName, prNumber);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("코멘트 등록 실패 - {} PR #{}", repoFullName, prNumber);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("dispatchTaskExecutor")
    public CompletableFuture<Void> saveReviewAsync(
        String repoFullName,
        int prNumber,
        String title,
        String action,
        String reviewComment
    ) {
        try {
            log.info("코멘트 등록 시작 - {} PR #{}", repoFullName, prNumber);
            githubFileService.saveReviewFile(repoFullName, prNumber, title, action, reviewComment);
            log.info("코멘트 등록 완료 - {} PR #{}", repoFullName, prNumber);
            return CompletableFuture.completedFuture(null);
        } catch(Exception e) {
            log.error("코멘트 등록 실패 - {} PR #{}", repoFullName, prNumber);
            return CompletableFuture.failedFuture(e);
        }
    }
}
