package com.marketnexus.newsstream.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilTest {

    @Test
    void sha256_isDeterministicForSameInput() {
        String hash1 = HashUtil.sha256("Title", "http://example.com");
        String hash2 = HashUtil.sha256("Title", "http://example.com");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void sha256_differsForDifferentTitle() {
        String hash1 = HashUtil.sha256("Title A", "http://example.com");
        String hash2 = HashUtil.sha256("Title B", "http://example.com");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void sha256_differsForDifferentUrl() {
        String hash1 = HashUtil.sha256("Title", "http://example.com/a");
        String hash2 = HashUtil.sha256("Title", "http://example.com/b");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void sha256_returns64LowercaseHexChars() {
        String hash = HashUtil.sha256("Title", "http://example.com");

        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void sha256_matchesKnownVector() {
        // Precomputed: sha256("Title|http://example.com")
        String hash = HashUtil.sha256("Title", "http://example.com");

        assertThat(hash).isEqualTo("a43c9caf16b71be61b5d2dae2f49d1bd8a114f0acaf75e64fe288e42c5e1eade");
    }
}
