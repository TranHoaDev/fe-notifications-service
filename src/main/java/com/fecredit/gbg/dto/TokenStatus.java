package com.fecredit.gbg.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TokenStatus {
    private boolean hasValidAccessToken;
    private boolean hasValidRefreshToken;
    private LocalDateTime accessTokenExpiresAt;
    private LocalDateTime refreshTokenExpiresAt;
    private long minutesUntilExpiry;
}

