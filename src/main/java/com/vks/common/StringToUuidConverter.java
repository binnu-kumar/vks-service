package com.vks.common;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StringToUuidConverter implements Converter<String, UUID> {

    @Override
    public UUID convert(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        // Remove curly braces if present
        String cleanedSource = source.replaceAll("[{}]", "");

        try {
            return UUID.fromString(cleanedSource);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid UUID format: '" + source + "'. Expected format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", e);
        }
    }
}
