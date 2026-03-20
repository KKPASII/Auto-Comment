package com.hamplz.autocomment;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ReviewFileFormatter {

    public String buildMarkdown(
        String repoFullName,
        int prNumber,
        String title,
        String action,
        String diffUrl,
        String reviewComment
    ) {
        String reviewdAt = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return """
            # PR Review Log
            
            - Repository: %s
            - PR Number: %d
            - Title: %s
            - Action: %s
            - Review At: %s
            - Diff URL: %s
            
            ## GPT Review
            
            %s
            """.formatted(
                repoFullName,
                prNumber,
                title,
                action,
                reviewdAt,
                diffUrl,
                reviewComment
        );
    }
}
