package com.insuraTrack.controller;

import com.insuraTrack.service.CompanyAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyAnalyticsController {

    private final CompanyAnalyticsService analyticsService;

    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> getCompanyAnalytics(@PathVariable String id) {
        return ResponseEntity.ok(analyticsService.getCompanyAnalytics(id));
    }
}