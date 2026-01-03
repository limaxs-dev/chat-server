package tech.limaxs.chat.infra.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter for handling String to PostgreSQL JSONB column mapping.
 * Converts Java String to PostgreSQL JSONB type and vice versa.
 */
@Converter(autoApply = false)
public class JsonbStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        // If the string is empty, return null instead of empty string for JSONB
        if (attribute.trim().isEmpty()) {
            return null;
        }
        // Return the string as-is - PostgreSQL will validate it as JSON
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return dbData;
    }
}
