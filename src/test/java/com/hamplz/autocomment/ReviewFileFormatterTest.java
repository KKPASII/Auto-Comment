package com.hamplz.autocomment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ReviewFileFormatterTest {

    private final ReviewFileFormatter formatter = new ReviewFileFormatter();

    @Test
    void markdown_생성_테스트() {
        String result = formatter.buildMarkdown(
            "repo",
            1,
            "title",
            "opened",
            "url",
            "review"
        );

        assertThat(result).contains("Repository: repo");
        assertThat(result).contains("PR Number: #1");
        assertThat(result).contains("Title: title");
        assertThat(result).contains("Action: opened");
        assertThat(result).contains("Diff File URL: url");
        assertThat(result).contains("review");
    }
}