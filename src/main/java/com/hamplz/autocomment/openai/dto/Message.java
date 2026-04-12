package com.hamplz.autocomment.openai.dto;

public record Message(
    String role,
    String content
) {
}