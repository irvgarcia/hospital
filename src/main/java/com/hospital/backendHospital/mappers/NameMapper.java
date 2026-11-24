package com.hospital.backendHospital.mappers;

import com.hospital.backendHospital.models.entity.User;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/** B8: los DTOs muestran el nombre completo, no solo el nombre de pila. */
@Component
public class NameMapper {

    @Named("fullName")
    public String fullName(User user) {
        if (user == null) {
            return null;
        }
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
