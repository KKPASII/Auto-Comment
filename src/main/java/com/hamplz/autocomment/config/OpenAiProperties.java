package com.hamplz.autocomment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
    String apiKey,
    String apiUrl,
    String model,
    Double temperature
) {
}