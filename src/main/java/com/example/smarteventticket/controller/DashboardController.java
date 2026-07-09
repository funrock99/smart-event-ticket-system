package com.example.smarteventticket.controller;

import com.example.smarteventticket.dto.response.DashboardSummaryResponse;
import com.example.smarteventticket.dto.response.SourceRankingResponse;
import com.example.smarteventticket.service.DashboardService;
import java.util.List;
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

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/source-ranking")
    public List<SourceRankingResponse> getSourceRankings() {
        return dashboardService.getSourceRankings();
    }
}


