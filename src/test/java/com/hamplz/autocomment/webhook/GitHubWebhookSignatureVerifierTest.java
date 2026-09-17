package com.hamplz.autocomment.webhook;

import com.hamplz.autocomment.config.GithubProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class GitHubWebhookSignatureVerifierTest {

    private static final String TEST_SECRET = "test-secret";
    private static final String INVALID_SIGNATURE = "sha256=invalid";
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String INVALID_SECRET = "wrong-secret";

    private static final byte[] PAYLOAD =
        "{\"action\":\"labeled\"}".getBytes(StandardCharsets.UTF_8);

    private GithubProperties githubProperties;
    private GitHubWebhookSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        githubProperties = mock(GithubProperties.class);

        given(githubProperties.webhookSecret())
            .willReturn(TEST_SECRET);

        verifier =
            new GitHubWebhookSignatureVerifier(githubProperties);
    }

    @Test
    @DisplayName("유효한 서명이면 검증에 성공한다")
    void acceptsValidSignature() throws Exception {
        String signature = createSignature(PAYLOAD);

        assertTrue(verifier.isValid(PAYLOAD, signature));
    }

    @Test
    @DisplayName("페이로드가 변조되면 서명 검증에 실패한다")
    void rejectsModifiedPayload() throws Exception {
        String signature = createSignature(PAYLOAD);

        byte[] modifiedPayload =
            "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.isValid(modifiedPayload, signature));
    }

    @Test
    @DisplayName("유효하지 않은 서명이면 검증에 실패한다")
    void rejectsInvalidSignature() {
        assertFalse(verifier.isValid(PAYLOAD, INVALID_SIGNATURE));
    }

    @Test
    @DisplayName("다른 시크릿으로 생성한 서명이면 검증에 실패한다")
    void rejectsSignatureGeneratedWithWrongSecret() throws Exception {
        String signature = createSignature(PAYLOAD, INVALID_SECRET);

        assertFalse(verifier.isValid(PAYLOAD, signature));
    }

    @Test
    @DisplayName("서명이 없으면 검증에 실패한다")
    void rejectsMissingSignature() {
        assertFalse(verifier.isValid(PAYLOAD, null));
    }

    @Test
    @DisplayName("웹훅 시크릿이 설정되지 않으면 검증에 실패한다")
    void rejectsMissingSecret() {
        given(githubProperties.webhookSecret()).willReturn("");

        assertFalse(verifier.isValid(PAYLOAD, INVALID_SIGNATURE));
    }

    private String createSignature(byte[] payload) throws Exception {
        return createSignature(payload, TEST_SECRET);
    }

    private String createSignature(byte[] payload, String secret) throws Exception {
        Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);

        mac.init(new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8),
            SIGNATURE_ALGORITHM
        ));

        return SIGNATURE_PREFIX
            + HexFormat.of().formatHex(mac.doFinal(payload));
    }
}