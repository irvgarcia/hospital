package com.hospital.backendHospital;

import com.hospital.backendHospital.models.entity.RoleEnum;
import com.hospital.backendHospital.repositories.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class BackendHospitalApplicationIT extends AbstractIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void seedCreatesAllRolesInMySql() {
        for (RoleEnum roleEnum : RoleEnum.values()) {
            assertThat(roleRepository.findByRoleEnum(roleEnum)).isPresent();
        }
    }
}
