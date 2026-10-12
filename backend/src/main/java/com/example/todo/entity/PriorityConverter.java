package com.example.todo.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the priority as a small number so {@code ORDER BY priority} needs no CASE expression. */
@Converter(autoApply = true)
public class PriorityConverter implements AttributeConverter<Priority, Short> {

    @Override
    public Short convertToDatabaseColumn(Priority priority) {
        return priority == null ? null : (short) priority.ordinal();
    }

    @Override
    public Priority convertToEntityAttribute(Short value) {
        return value == null ? null : Priority.values()[value];
    }
}
