package com.homekm.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TokenHasherTest {

    @Test
    void sha256_producesConsistentHash() {
        String input = "test-token-value";

        String hash1 = TokenHasher.sha256(input);
        String hash2 = TokenHasher.sha256(input);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void sha256_produces64CharHexString() {
        String hash = TokenHasher.sha256("some-input");

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void sha256_differentInputsProduceDifferentHashes() {
        String hash1 = TokenHasher.sha256("input-one");
        String hash2 = TokenHasher.sha256("input-two");

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
