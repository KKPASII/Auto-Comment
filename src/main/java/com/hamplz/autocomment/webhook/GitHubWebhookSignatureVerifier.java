package com.hamplz.autocomment.webhook;

import com.hamplz.autocomment.config.GithubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class GitHubWebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookSignatureVerifier.class);
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final GithubProperties githubProperties;

    public GitHubWebhookSignatureVerifier(GithubProperties githubProperties) {
        this.githubProperties = githubProperties;
    }

    public boolean isValid(String payload, String signatureHeader) {
        if (githubProperties.webhookSecret() == null || githubProperties.webhookSecret().isBlank()) {
            log.warn("github.webhook-secret is not configured. Skipping webhook signature verification.");
            return true;
        }

        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        String expectedSignature = SIGNATURE_PREFIX + sign(payload);
        return MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            signatureHeader.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                githubProperties.webhookSecret().getBytes(StandardCharsets.UTF_8),
                SIGNATURE_ALGORITHM
            );
            mac.init(secretKey);
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify GitHub webhook signature", e);
        }
    }
}
