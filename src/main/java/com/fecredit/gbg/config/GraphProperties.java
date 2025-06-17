package com.fecredit.gbg.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "microsoft.graph")
@Data
@Configuration
public class GraphProperties {
    private String clientId;
    private String clientSecret;
    private String tenantId;
    private String redirectUri;
    private String scope = "User.Read Chat.ReadWrite ChatMessage.Send offline_access";
    private int tokenBufferMinutes = 5;
    private int refreshTokenDays = 90;
}
