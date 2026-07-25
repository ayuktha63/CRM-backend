package com.orque.crm.google;

import com.orque.crm.google.crypto.GoogleTokenConverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleTokenConverterTest {

    private final GoogleTokenConverter converter = new GoogleTokenConverter();

    @Test
    void roundTripsPlainTextThroughEncryptionAndDecryption() {
        String token = "ya29.a0AfH6SMB_example_access_token";

        String encrypted = converter.convertToDatabaseColumn(token);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertThat(encrypted).isNotEqualTo(token);
        assertThat(decrypted).isEqualTo(token);
    }

    @Test
    void neverStoresTokenInPlaintext() {
        String token = "super-secret-refresh-token-value";
        String encrypted = converter.convertToDatabaseColumn(token);

        assertThat(encrypted).doesNotContain(token);
    }

    @Test
    void producesDifferentCiphertextEachTimeDueToRandomIv() {
        String token = "same-input-token";
        String first = converter.convertToDatabaseColumn(token);
        String second = converter.convertToDatabaseColumn(token);

        assertThat(first).isNotEqualTo(second);
        assertThat(converter.convertToEntityAttribute(first)).isEqualTo(token);
        assertThat(converter.convertToEntityAttribute(second)).isEqualTo(token);
    }

    @Test
    void nullAndBlankPassThroughUnchanged() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToDatabaseColumn("")).isEmpty();
    }
}
