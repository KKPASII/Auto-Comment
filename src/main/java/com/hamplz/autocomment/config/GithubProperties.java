package com.hamplz.autocomment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "github")
public record GithubProperties(
    String token,
    String apiUrl,
    String baseUrl,
    String reviewBranch
) {
}
