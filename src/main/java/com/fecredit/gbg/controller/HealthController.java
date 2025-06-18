package com.fecredit.gbg.controller;

import com.fecredit.gbg.dto.TokenStatus;
import com.fecredit.gbg.service.TokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final TokenManager tokenManager;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        TokenStatus tokenStatus = tokenManager.getTokenStatus();

        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now());

        Map<String, Object> authStatus = new HashMap<>();
        authStatus.put("hasValidTokens", tokenStatus.isHasValidAccessToken());
        authStatus.put("minutesUntilExpiry", tokenStatus.getMinutesUntilExpiry());

        status.put("authentication", authStatus);

        return ResponseEntity.ok(status);
    }
}