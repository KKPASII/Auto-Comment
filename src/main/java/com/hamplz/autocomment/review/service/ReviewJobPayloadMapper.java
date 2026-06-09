package com.hamplz.autocomment.review.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamplz.autocomment.webhook.dto.PullRequestWebhook;
import org.springframework.stereotype.Component;

@Component
public class ReviewJobPayloadMapper {

    private static final String SERIALIZE_ERROR_MESSAGE = "Failed to serialize review job";
    private static final String DESERIALIZE_ERROR_MESSAGE = "Failed to deserialize review job";

    private final ObjectMapper objectMapper;

    public ReviewJobPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(PullRequestWebhook webhook) {
        try {
            return objectMapper.writeValueAsString(webhook);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(SERIALIZE_ERROR_MESSAGE, e);
        }
    }

    public PullRequestWebhook deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, PullRequestWebhook.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(DESERIALIZE_ERROR_MESSAGE, e);
        }
    }
}
