package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.models.dto.dashboard.DashboardDto;
import com.hospital.backendHospital.services.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final IDashboardService dashboardService;

    @GetMapping
    public DashboardDto getDashboard() {
        return dashboardService.getDashboard();
    }
}
