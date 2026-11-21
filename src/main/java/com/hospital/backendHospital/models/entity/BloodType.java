package com.hospital.backendHospital.models.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

/** Grupo sanguineo. En la base y en el JSON se usa la etiqueta habitual ("O+", "AB-"...). */
public enum BloodType {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String label;

    BloodType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    /** Acepta la etiqueta ("O+") o el nombre del enum ("O_POSITIVE"), sin distinguir mayusculas ni espacios. */
    public static Optional<BloodType> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.replace(" ", "").toUpperCase();
        return Arrays.stream(values())
                .filter(type -> type.label.equals(normalized) || type.name().equals(normalized))
                .findFirst();
    }

    @JsonCreator
    public static BloodType fromJson(String value) {
        return parse(value).orElseThrow(() -> new IllegalArgumentException("Invalid blood type: " + value));
    }
}
