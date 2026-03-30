package com.hamplz.autocomment.dto;

import java.util.List;

public record ChatRequest(
    String model,
    List<Message> messages,
    Double temperature
) {
}
