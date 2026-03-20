package com.hamplz.autocomment;

import com.hamplz.autocomment.config.GithubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class GithubCommentService {

    private static final Logger log = LoggerFactory.getLogger(GithubCommentService.class);

    private final RestClient restClient;
    private final GithubProperties githubProperties;

    public GithubCommentService(RestClient.Builder restClientBuilder, GithubProperties githubProperties) {
        this.restClient = restClientBuilder.build();
        this.githubProperties = githubProperties;
    }

    public void createComment(String repoFullName,int prNumber, String commentBody) {
        validateGithubProperties();

        String url = githubProperties.apiUrl()
            + "/repos/"
            + repoFullName
            + "/issues/"
            + prNumber
            + "/comments";

        log.info("GitHub PR 댓글 등록 시작: repo={}, prNumber={}", repoFullName, prNumber);

        restClient.post()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubProperties.token())
            .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("body", commentBody))
            .retrieve()
            .toBodilessEntity();

        log.info("GitHub PR 댓글 등록 완료");
    }

    private void validateGithubProperties() {
        if (githubProperties.token() == null || githubProperties.token().isBlank()) {
            throw new IllegalArgumentException("GITHUB_TOKEN이 설정되지 않았습니다.");
        }
        if (githubProperties.apiUrl() == null || githubProperties.apiUrl().isBlank()) {
            throw new IllegalArgumentException("github.api-url이 설정되지 않았습니다.");
        }
    }
}
