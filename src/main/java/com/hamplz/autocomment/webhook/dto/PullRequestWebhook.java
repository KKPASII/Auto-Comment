package com.hamplz.autocomment.webhook.dto;

import java.util.List;

public record PullRequestWebhook(
    PullRequestAction action,
    int prNumber,
    String title,
    String diffUrl,
    String headRef,
    String repoFullName,
    List<String> labels,
    String changedLabel
) {}