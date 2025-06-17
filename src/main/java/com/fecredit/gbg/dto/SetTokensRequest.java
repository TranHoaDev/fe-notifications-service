package com.fecredit.gbg.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SetTokensRequest {
    private String accessToken;

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
