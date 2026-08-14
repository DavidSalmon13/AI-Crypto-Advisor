package com.moveo.aicryptoadvisor.controller;

import com.moveo.aicryptoadvisor.dto.response.DashboardResponse;
import com.moveo.aicryptoadvisor.entity.User;
import com.moveo.aicryptoadvisor.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse getDashboard(@AuthenticationPrincipal User user) {
        return dashboardService.getDashboard(user.getId());
    }
}
