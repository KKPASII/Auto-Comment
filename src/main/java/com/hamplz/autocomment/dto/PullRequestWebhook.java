package com.hamplz.autocomment.dto;

public record PullRequestWebhook(
    PullRequestAction action,
    int prNumber,
    String title,
    String diffUrl,
    String headRef,
    String repoFullName
) {}