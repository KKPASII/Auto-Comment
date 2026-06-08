package com.hamplz.autocomment.review.service;

import com.hamplz.autocomment.github.service.GithubCommentService;
import com.hamplz.autocomment.github.service.GithubFileService;
import com.hamplz.autocomment.review.dto.DispatchTaskResult;
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
    public CompletableFuture<DispatchTaskResult> commentAsync(
        String repoFullName,
        int prNumber,
        String reviewComment
    ) {
        try {
            log.info("댓글 등록 시작 - {} PR #{}", repoFullName, prNumber);
            githubCommentService.createComment(repoFullName, prNumber, reviewComment);
            log.info("댓글 등록 완료 - {} PR #{}", repoFullName, prNumber);
            return CompletableFuture.completedFuture(DispatchTaskResult.success());
        } catch (Exception e) {
            log.error("댓글 등록 실패 - {} PR #{}", repoFullName, prNumber, e);
            return CompletableFuture.completedFuture(DispatchTaskResult.failure(e));
        }
    }

    @Async("dispatchTaskExecutor")
    public CompletableFuture<DispatchTaskResult> saveReviewAsync(
        String repoFullName,
        int prNumber,
        String title,
        String action,
        String reviewComment
    ) {
        try {
            log.info("리뷰 파일 저장 시작 - {} PR #{}", repoFullName, prNumber);
            githubFileService.saveReviewFile(repoFullName, prNumber, title, action, reviewComment);
            log.info("리뷰 파일 저장 완료 - {} PR #{}", repoFullName, prNumber);
            return CompletableFuture.completedFuture(DispatchTaskResult.success());
        } catch(Exception e) {
            log.error("리뷰 파일 저장 실패 - {} PR #{}", repoFullName, prNumber, e);
            return CompletableFuture.completedFuture(DispatchTaskResult.failure(e));
        }
    }
}