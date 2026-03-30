package com.hamplz.autocomment.service;

import com.hamplz.autocomment.ReviewFileFormatter;
import com.hamplz.autocomment.config.GithubProperties;
import com.hamplz.autocomment.dto.GithubFileRequest;
import com.hamplz.autocomment.dto.GithubRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Map;

@Service
public class GithubFileService {

    private static final Logger log = LoggerFactory.getLogger(GithubFileService.class);

    private final RestClient restClient;
    private final GithubProperties githubProperties;
    private final ReviewFileFormatter reviewFileFormatter;

    public GithubFileService(RestClient.Builder restClientBuilder, GithubProperties githubProperties, ReviewFileFormatter reviewFileFormatter) {
        this.restClient = restClientBuilder.build();
        this.githubProperties = githubProperties;
        this.reviewFileFormatter = reviewFileFormatter;
    }

    public void saveReviewFile(String repoFullName,
                               int prNumber,
                               String title,
                               String action,
                               String reviewComment) {
        validateGithubProperties();

        ensureBranchExists(repoFullName, githubProperties.reviewBranch());

        String markdownContent = reviewFileFormatter.buildMarkdown(
            repoFullName,
            prNumber,
            title,
            action,
            buildFileChangedPath(repoFullName, prNumber),
            reviewComment
        );

        saveHistoryFile(repoFullName, prNumber, markdownContent);

        saveLatestFile(repoFullName, prNumber, markdownContent);
    }

    private void saveHistoryFile(String repoFullName, int prNumber, String markdown) {

        String path = buildFilePath(prNumber);

        String encoded = Base64.getEncoder()
            .encodeToString(markdown.getBytes(StandardCharsets.UTF_8));

        GithubFileRequest request = GithubRequestFactory.createHistoryFile(
            prNumber,
            encoded,
            githubProperties.reviewBranch()
        );

        restClient.put()
            .uri(githubProperties.apiUrl() + "/repos/" + repoFullName + "/contents/" + path)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubProperties.token())
            .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toBodilessEntity();

        log.info("히스토리 파일 저장 완료: {}", path);
    }

    private void saveLatestFile(String repoFullName, int prNumber, String content) {

        String path = buildLatestFilePath(prNumber);

        String sha = getFileSha(repoFullName, path, githubProperties.reviewBranch());

        String encoded = Base64.getEncoder()
            .encodeToString(content.getBytes(StandardCharsets.UTF_8));

        GithubFileRequest request;

        if (sha == null) {
            request = GithubRequestFactory.createNewLatestFile(
                prNumber,
                encoded,
                githubProperties.reviewBranch()
            );
        } else {
            request = GithubRequestFactory.updateLatestFile(
                prNumber,
                encoded,
                githubProperties.reviewBranch(),
                sha
            );
        }

        restClient.put()
            .uri(githubProperties.apiUrl() + "/repos/" + repoFullName + "/contents/" + path)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubProperties.token())
            .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toBodilessEntity();

        log.info("latest.md 저장 완료");
    }

    private String getFileSha(String repoFullName, String path, String branch) {
        try {
            Map response = restClient.get()
                .uri(githubProperties.apiUrl()
                    + "/repos/" + repoFullName
                    + "/contents/" + path
                    + "?ref=" + branch)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubProperties.token())
                .retrieve()
                .body(Map.class);

            return (String) response.get("sha");

        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    private void ensureBranchExists(String repoFullName, String branch) {
        try {
            restClient.get()
                .uri(githubProperties.apiUrl() + "/repos/" + repoFullName + "/branches/" + branch)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubProperties.token())
                .retrieve()
                .toBodilessEntity();

            log.info("브랜치 이미 존재: {}", branch);

        } catch (HttpClientErrorException.NotFound e) {

            log.info("브랜치 없음 -> 생성 시작: {}", branch);

            String baseSha = getBaseBranchSha(repoFullName, "main");

            Map<String, Object> body = Map.of(
                "ref", "refs/heads/" + branch,
                "sha", baseSha
            );

            restClient.post()
                .uri(githubProperties.apiUrl() + "/repos/" + repoFullName + "/git/refs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubProperties.token())
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

            log.info("브랜치 생성 완료: {}", branch);
        }
    }

    private String getBaseBranchSha(String repoFullName, String baseBranch) {
        var response = restClient.get()
            .uri(githubProperties.apiUrl() + "/repos/" + repoFullName + "/git/ref/heads/" + baseBranch)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubProperties.token())
            .retrieve()
            .body(Map.class);

        Map object = (Map) response.get("object");
        return (String) object.get("sha");
    }

    private String buildFilePath(int prNumber) {
        String date = LocalDate.now().toString();
        String time = LocalTime.now().withNano(0).toString();

        return "reviews/pr-" + prNumber + "/" + date + "/" + time + ".md";
    }

    private String buildLatestFilePath(int prNumber) {
        return "reviews/pr-" + prNumber + "/latest.md";
    }

    private String buildFileChangedPath(String repoFullName, int prNumber) {
        String url = githubProperties.baseUrl() + repoFullName + "/pull/" + prNumber + "/changes";
        log.info("FileChangedPath: {}", url);
        return url;
    }

    private void validateGithubProperties() {
        if (githubProperties.token() == null || githubProperties.token().isBlank()) {
            throw new IllegalArgumentException("GITHUB_TOKEN이 설정되지 않았습니다.");
        }
        if (githubProperties.apiUrl() == null || githubProperties.apiUrl().isBlank()) {
            throw new IllegalArgumentException("github.api-url이 설정되지 않았습니다.");
        }
        if (githubProperties.reviewBranch() == null || githubProperties.reviewBranch().isBlank()) {
            throw new IllegalArgumentException("github.review-branch가 설정되지 않았습니다.");
        }
    }
}
