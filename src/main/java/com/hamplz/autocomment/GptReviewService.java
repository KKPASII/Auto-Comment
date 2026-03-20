package com.hamplz.autocomment;

import com.fasterxml.jackson.databind.JsonNode;
import com.hamplz.autocomment.config.OpenAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GptReviewService {

    private static final Logger log = LoggerFactory.getLogger(GptReviewService.class);

    private final RestClient restClient;
    private final OpenAiProperties openAiProperties;

    public GptReviewService(RestClient.Builder restClientBuilder, OpenAiProperties openAiProperties) {
        this.restClient = restClientBuilder.build();
        this.openAiProperties = openAiProperties;
    }

    private void validateOpenAiProperties() {
        if (openAiProperties.apiKey() == null || openAiProperties.apiKey().isBlank()) {
            throw new IllegalArgumentException("OPENAI_API_KEY가 설정되지 않았습니다.");
        }
        if (openAiProperties.apiUrl() == null || openAiProperties.apiUrl().isBlank() ) {
            throw new IllegalArgumentException("openai.api-url이 설정되지 않았습니다.");
        }
        if (openAiProperties.model() == null || openAiProperties.model().isBlank()) {
            throw new IllegalArgumentException("openai.model이 설정되지 않았습니다.");
        }
    }

    public String generateReview(String diffContent) {
        validateOpenAiProperties();

        log.info("GPT 리뷰 생성 시작");

        String prompt = buildPrompt(diffContent);

        Map<String, Object> requestBody = Map.of(
            "model", openAiProperties.model(),
            "messages", List.of(
                Map.of("role", "system", "content", "당신은 정확하고 실용적인 시니어 백엔드 코드 리뷰어이다."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.2
        );

        JsonNode response = restClient.post()
            .uri(openAiProperties.apiUrl())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .retrieve()
            .body(JsonNode.class);

        String review = response.path("choices")
            .get(0)
            .path("message")
            .path("content")
            .asText();

        log.info("GPT 리뷰 생성 완료");
        return review;
    }

    private String buildPrompt(String diffContent) {
        return """
                당신은 PR 리뷰를 수행하는 시니어 백엔드 개발자이다.
                아래 diff를 보고 코드 리뷰 코멘트를 한국어로 작성하라.

                요구사항:
                1. 문제점이 있다면 구체적으로 설명하라.
                2. 개선 방향을 제안하라.
                3. 가능하면 더 나은 코드 예시를 짧게 보여주어라.
                4. 너무 장황하지 않게, 실제 PR 코멘트처럼 작성하라.
                5. 문제 없으면 좋은 점과 함께 간단한 개선 포인트만 적어주어라.

                PR diff:
                """ + diffContent;
    }
}
