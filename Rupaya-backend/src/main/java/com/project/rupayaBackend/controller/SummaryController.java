package com.project.rupayaBackend.controller;

import com.project.rupayaBackend.dto.SummaryResponse;
import com.project.rupayaBackend.security.CustomUserDetails;
import com.project.rupayaBackend.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/summary/")
public class SummaryController {

    @Autowired
    SummaryService summaryService;

    @GetMapping
    public ResponseEntity<SummaryResponse> getSummary(@AuthenticationPrincipal CustomUserDetails principal, @RequestParam(value = "group_id",required = false) UUID groupId) {
        return ResponseEntity.ok(summaryService.getSummary(principal.getId(),groupId));
    }
}
