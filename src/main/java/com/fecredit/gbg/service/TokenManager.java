package com.fecredit.gbg.service;

import com.fecredit.gbg.dto.TokenResponse;
import com.fecredit.gbg.dto.TokenStatus;
import com.fecredit.gbg.exception.TokenException;

public interface TokenManager {
    String getDelegatedToken() throws TokenException;
    TokenResponse exchangeCodeForToken(String authorizationCode) throws TokenException;
    void clearTokens();
    String getAuthorizationUrl();
    TokenStatus getTokenStatus();
    void setTokensDirectly(String accessToken, String refreshToken);
}
