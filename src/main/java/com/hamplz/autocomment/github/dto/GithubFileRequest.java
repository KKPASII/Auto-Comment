package com.hamplz.autocomment.github.dto;

public record GithubFileRequest(
    String message,
    String content,
    String branch,
    String sha
) {
}
