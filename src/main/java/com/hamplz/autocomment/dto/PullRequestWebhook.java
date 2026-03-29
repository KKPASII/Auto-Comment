package com.hamplz.autocomment.dto;

public record PullRequestWebhook(
    String action,
    int prNumber,
    String title,
    String diffUrl,
    String headRef,
    String repoFullName
) {}