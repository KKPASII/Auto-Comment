package com.hamplz.autocomment.github.service;

import com.hamplz.autocomment.support.ExternalApiRetryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import static com.hamplz.autocomment.support.ExternalApiOperation.GITHUB_GET_PR_DIFF;

@Service
public class GithubDiffService {

    private static final Logger log = LoggerFactory.getLogger(GithubDiffService.class);

    private final RestClient restClient;
    private final ExternalApiRetryExecutor retryExecutor;

    public GithubDiffService(RestClient.Builder restClientBuilder, ExternalApiRetryExecutor retryExecutor) {
        this.restClient = restClientBuilder.build();
        this.retryExecutor = retryExecutor;
    }

    public String getPullRequestDiff(String diffUrl) {
        log.info("PR diff 요청 시작: {}", diffUrl);

        String diffContent = retryExecutor.execute(GITHUB_GET_PR_DIFF, () -> restClient.get()
            .uri(diffUrl)
            .header("Accept", "application/vnd.github.v3.diff")
            .retrieve()
            .body(String.class)
        );

        log.info("PR diff 조회 완료");
        return diffContent;
    }
}
