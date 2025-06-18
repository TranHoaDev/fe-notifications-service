package com.fecredit.gbg.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fecredit.gbg.config.GraphProperties;
import com.fecredit.gbg.dto.TokenResponse;
import com.fecredit.gbg.dto.TokenStatus;
import com.fecredit.gbg.exception.TokenException;
import com.fecredit.gbg.service.TokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class MicrosoftGraphTokenManager implements TokenManager {

    private final RestTemplate restTemplate;
    private final GraphProperties graphProperties;
    private final ReentrantLock tokenLock = new ReentrantLock();

    // Token storage
    private volatile String delegatedToken;
    private volatile String refreshToken;
    private volatile LocalDateTime tokenExpiry;
    private volatile LocalDateTime refreshTokenExpiry;

    public void setTokensDirectly(String accessToken, String refreshToken) {
        tokenLock.lock();
        try {
            // Only set access token if available
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                this.delegatedToken = accessToken;
                this.tokenExpiry = extractExpiryFromToken(accessToken);
            }

            this.refreshToken = refreshToken; // Refresh token is always required
            this.refreshTokenExpiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(90); //  Default 90 days
            log.info("Tokens set manually - Access: {}, Refresh expires at: {}",
                    accessToken != null ? "provided" : "not provided", refreshTokenExpiry);
        } finally {
            tokenLock.unlock();
        }
    }

    private LocalDateTime extractExpiryFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getDecoder().decode(parts[1]));

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(payload);
            long exp = jsonNode.get("exp").asLong();

            return LocalDateTime.ofInstant(Instant.ofEpochSecond(exp), ZoneOffset.UTC)
                    .minusMinutes(5);
        } catch (Exception e) {
            log.warn("Failed to extract expiry from token, using default", e);
            return LocalDateTime.now(ZoneOffset.UTC).plusHours(1).minusMinutes(5);
        }
    }

    @Override
    public String getDelegatedToken() throws TokenException {
        tokenLock.lock();
        try {
            if (isTokenValid()) {
                log.debug("Using existing valid token");
                return delegatedToken;
            }

            if (canRefreshToken()) {
                log.info("Access token expired or empty, fetching by refresh_token...");
                refreshAccessToken();
                return delegatedToken;
            }

            log.warn("Both access and refresh tokens expired");
            throw new TokenException("Authentication required",
                    TokenException.ErrorType.AUTHENTICATION_REQUIRED);

        } finally {
            tokenLock.unlock();
        }
    }

    @Override
    public TokenResponse exchangeCodeForToken(String authorizationCode) throws TokenException {
        log.info("Exchanging authorization code for tokens");

        String tokenUrl = buildTokenUrl();
        MultiValueMap<String, String> body = buildCodeExchangeBody(authorizationCode);

        try {
            TokenResponse response = makeTokenRequest(tokenUrl, body);
            updateTokens(response);
            log.info("Successfully obtained initial tokens");
            return response;

        } catch (Exception e) {
            log.error("Failed to exchange authorization code", e);
            throw new TokenException("Code exchange failed: " + e.getMessage(),
                    TokenException.ErrorType.TOKEN_EXCHANGE_FAILED);
        }
    }

    private void refreshAccessToken() throws TokenException {
        log.info("Refreshing access token");

        String tokenUrl = buildTokenUrl();
        MultiValueMap<String, String> body = buildRefreshTokenBody();

        try {
            TokenResponse response = makeTokenRequest(tokenUrl, body);
            updateTokens(response);
            log.info("Successfully refreshed access token");

        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            clearTokens();
            throw new TokenException("Token refresh failed: " + e.getMessage(),
                    TokenException.ErrorType.REFRESH_FAILED);
        }
    }

    private TokenResponse makeTokenRequest(String url, MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<TokenResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, request, TokenResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Token request failed with status: " + response.getStatusCode());
        }

        return response.getBody();
    }

    private void updateTokens(TokenResponse response) {
        this.delegatedToken = response.getAccessToken();
        this.tokenExpiry = LocalDateTime.now(ZoneOffset.UTC)
                .plusSeconds(response.getExpiresIn() - (graphProperties.getTokenBufferMinutes() * 60));

        if (response.getRefreshToken() != null) {
            this.refreshToken = response.getRefreshToken();
            this.refreshTokenExpiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(graphProperties.getRefreshTokenDays());
        }

        log.debug("Tokens updated - expires at: {}", tokenExpiry);
    }

    private boolean isTokenValid() {
        return delegatedToken != null &&
                tokenExpiry != null &&
                LocalDateTime.now(ZoneOffset.UTC).isBefore(tokenExpiry);
    }

    private boolean canRefreshToken() {
        return refreshToken != null &&
                refreshTokenExpiry != null &&
                LocalDateTime.now(ZoneOffset.UTC).isBefore(refreshTokenExpiry);
    }

    @Override
    public void clearTokens() {
        log.info("Clearing all tokens");
        this.delegatedToken = null;
        this.refreshToken = null;
        this.tokenExpiry = null;
        this.refreshTokenExpiry = null;
    }

    @Override
    public String getAuthorizationUrl() {
        return String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize" +
                        "?client_id=%s&response_type=code&redirect_uri=%s&scope=%s&response_mode=query",
                graphProperties.getTenantId(),
                graphProperties.getClientId(),
                graphProperties.getRedirectUri(),
                graphProperties.getScope().replace(" ", "%20")
        );
    }

    @Override
    public TokenStatus getTokenStatus() {
        return TokenStatus.builder()
                .hasValidAccessToken(isTokenValid())
                .hasValidRefreshToken(canRefreshToken())
                .accessTokenExpiresAt(tokenExpiry)
                .refreshTokenExpiresAt(refreshTokenExpiry)
                .minutesUntilExpiry(tokenExpiry != null ?
                        ChronoUnit.MINUTES.between(LocalDateTime.now(ZoneOffset.UTC), tokenExpiry) : -1)
                .build();
    }

    private String buildTokenUrl() {
        return String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/token",
                graphProperties.getTenantId());
    }

    private MultiValueMap<String, String> buildCodeExchangeBody(String authorizationCode) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", graphProperties.getClientId());
        body.add("client_secret", graphProperties.getClientSecret());
        body.add("code", authorizationCode);
        body.add("redirect_uri", graphProperties.getRedirectUri());
        body.add("scope", graphProperties.getScope());
        return body;
    }

    private MultiValueMap<String, String> buildRefreshTokenBody() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", graphProperties.getClientId());
        body.add("client_secret", graphProperties.getClientSecret());
        body.add("refresh_token", refreshToken);
        body.add("scope", graphProperties.getScope());
        return body;
    }
}
