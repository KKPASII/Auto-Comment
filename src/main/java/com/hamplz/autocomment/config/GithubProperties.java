package com.hamplz.autocomment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github")
public record GithubProperties(
    String token,
    String webhookSecret,
    String apiUrl,
    String baseUrl,
    String reviewBranch
) {
}
