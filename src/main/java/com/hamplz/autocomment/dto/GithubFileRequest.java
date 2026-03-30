package com.hamplz.autocomment.dto;

public record GithubFileRequest(
    String message,
    String content,
    String branch,
    String sha
) {
}
