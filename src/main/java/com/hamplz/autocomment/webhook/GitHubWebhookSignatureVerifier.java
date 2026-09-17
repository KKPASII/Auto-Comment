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

    public boolean isValid(byte[] payload, String signatureHeader) {
        String webhookSecret = githubProperties.webhookSecret();

        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("GitHub webhook secret is not configured.");
            return false;
        }

        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        String signatureHex = signatureHeader.substring(SIGNATURE_PREFIX.length());

        byte[] actualSignature;

        try {
            actualSignature = HexFormat.of().parseHex(signatureHex);
        } catch (IllegalArgumentException e) {
            return false;
        }

        byte[] expectedSignature = sign(payload, webhookSecret);

        return MessageDigest.isEqual(
            expectedSignature,
            actualSignature
        );
    }

    private byte[] sign(byte[] payload, String webhookSecret) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);

            SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                SIGNATURE_ALGORITHM
            );

            mac.init(secretKey);

            return mac.doFinal(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify GitHub webhook signature", e);
        }
    }
}
