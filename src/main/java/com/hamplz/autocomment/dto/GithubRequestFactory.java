package com.hamplz.autocomment.dto;

public class GithubRequestFactory {
    public static GithubFileRequest createHistoryFile(
        int prNumber,
        String encodedContent,
        String branch
    ) {
        return new GithubFileRequest(
            "docs: add review history for PR #" + prNumber,
            encodedContent,
            branch,
            null
        );
    }

    public static GithubFileRequest createNewLatestFile(
        int prNumber,
        String encodedContent,
        String branch
    ) {
        return new GithubFileRequest(
            "docs: create latest review for PR #" + prNumber,
            encodedContent,
            branch,
            null
        );
    }

    public static GithubFileRequest updateLatestFile(
        int prNumber,
        String encodedContent,
        String branch,
        String sha
    ) {
        return new GithubFileRequest(
            "docs: update latest review for PR #" + prNumber,
            encodedContent,
            branch,
            sha
        );
    }
}
