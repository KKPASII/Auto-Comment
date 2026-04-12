package com.hamplz.autocomment.openai.dto;

import java.util.List;

public record ChatRequest(
    String model,
    List<Message> messages,
    Double temperature
) {
}
