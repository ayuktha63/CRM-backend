package com.orque.crm.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATETIME_MILLIS =
            new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .optionalStart().appendPattern(".SSS").optionalEnd()
                    .toFormatter();

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();

        // LocalDateTime: accept "2026-07-01" OR "2026-07-01T12:00:00" OR "2026-07-01T12:00:00.000"
        module.addDeserializer(LocalDateTime.class, new StdDeserializer<>(LocalDateTime.class) {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                String value = p.getText();
                if (value == null || value.isBlank()) return null;
                try {
                    return LocalDateTime.parse(value, DATETIME_MILLIS);
                } catch (DateTimeParseException e1) {
                    try {
                        return LocalDate.parse(value, DATE_ONLY).atStartOfDay();
                    } catch (DateTimeParseException e2) {
                        throw new IOException("Cannot parse date: " + value, e2);
                    }
                }
            }
        });

        // LocalDate: accept "2026-07-01" OR "2026-07-01T12:00:00"
        module.addDeserializer(LocalDate.class, new StdDeserializer<>(LocalDate.class) {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                String value = p.getText();
                if (value == null || value.isBlank()) return null;
                try {
                    return LocalDate.parse(value, DATE_ONLY);
                } catch (DateTimeParseException e1) {
                    try {
                        return LocalDateTime.parse(value, DATETIME_MILLIS).toLocalDate();
                    } catch (DateTimeParseException e2) {
                        throw new IOException("Cannot parse date: " + value, e2);
                    }
                }
            }
        });

        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DATE_ONLY));

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        mapper.registerModule(javaTimeModule);
        mapper.registerModule(module);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // The frontend's generic form-drawer is shared across every resource and can carry
        // incidental fields (e.g. license-only bookkeeping) that a given DTO doesn't declare.
        // A stray extra field should be ignored, not hard-fail the whole save.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }
}
