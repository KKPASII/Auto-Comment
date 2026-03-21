package com.hamplz.autocomment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GithubDiffService {

    private static final Logger log = LoggerFactory.getLogger(GithubDiffService.class);

    private final RestClient restClient;

    public GithubDiffService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public String getPullRequestDiff(String diffUrl) {
        log.info("PR diff 요청 시작: {}", diffUrl);

        String diffContent = restClient.get()
            .uri(diffUrl)
            .header("Accept", "application/vnd.github.v3.diff")
            .retrieve()
            .body(String.class);

        log.info("PR diff 조회 완료");
        return diffContent;
    }
}
