package com.hospital.backendHospital.models.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda la etiqueta ("O+"). Un valor antiguo que no corresponde a ningun grupo se lee como null. */
@Converter
public class BloodTypeConverter implements AttributeConverter<BloodType, String> {

    @Override
    public String convertToDatabaseColumn(BloodType attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public BloodType convertToEntityAttribute(String dbData) {
        return BloodType.parse(dbData).orElse(null);
    }
}
