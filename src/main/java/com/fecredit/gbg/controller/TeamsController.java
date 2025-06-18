package com.fecredit.gbg.controller;

import com.fecredit.gbg.client.TeamsGraphClient;
import com.fecredit.gbg.dto.MessageRequest;
import com.fecredit.gbg.dto.SetTokensRequest;
import com.fecredit.gbg.dto.TokenStatus;
import com.fecredit.gbg.exception.TokenException;
import com.fecredit.gbg.service.TokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Slf4j
public class TeamsController {

    private final TokenManager tokenManager;
    private final TeamsGraphClient teamsGraphClient;

    @PostMapping("/send-message")
    public ResponseEntity<String> sendMessage(@Valid @RequestBody MessageRequest request) {
        try {
            teamsGraphClient.sendMessage(request.getChatId(), request.getContent());
            return ResponseEntity.ok("Message sent successfully");

        } catch (TokenException e) {
            if (e.requiresReAuthentication()) {
                log.warn("Re-authentication required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Authentication required: " + tokenManager.getAuthorizationUrl());
            } else {
                log.error("Token error: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Token error: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send message: " + e.getMessage());
        }
    }

    @PostMapping("/auth/set-tokens")
    public ResponseEntity<String> setTokensManually(@RequestBody SetTokensRequest request) {
        try {
            // Update tokens directly into TokenManager
            tokenManager.setTokensDirectly(
                    request.getAccessToken(),
                    request.getRefreshToken()
            );

            return ResponseEntity.ok("Tokens set successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to set tokens: " + e.getMessage());
        }
    }

    @GetMapping("/auth/callback")
    public ResponseEntity<String> handleCallback(@RequestParam String code) {
        try {
            tokenManager.exchangeCodeForToken(code);
            return ResponseEntity.ok("Authentication successful");
        } catch (TokenException e) {
            return ResponseEntity.badRequest().body("Authentication failed: " + e.getMessage());
        }
    }

    @GetMapping("/token/status")
    public ResponseEntity<TokenStatus> getTokenStatus() {
        return ResponseEntity.ok(tokenManager.getTokenStatus());
    }

    @DeleteMapping("/auth/logout")
    public ResponseEntity<String> logout() {
        tokenManager.clearTokens();
        return ResponseEntity.ok("Logged out successfully");
    }
}
