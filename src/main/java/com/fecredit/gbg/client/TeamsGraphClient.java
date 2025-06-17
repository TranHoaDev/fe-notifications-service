package com.fecredit.gbg.client;

import com.fecredit.gbg.service.TokenManager;
import com.fecredit.gbg.exception.TokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@RequiredArgsConstructor
public class TeamsGraphClient {

    private final TokenManager tokenManager;
    private final RestTemplate restTemplate;

    public void sendMessage(String chatId, String content) throws TokenException {
        String token = tokenManager.getDelegatedToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String messageBody = String.format("{\n" +
                "    \"body\": {\n" +
                "        \"content\": \"%s\"\n" +
                "    }\n" +
                "}", content);

        HttpEntity<String> entity = new HttpEntity<>(messageBody, headers);

        String graphUrl = String.format(
                "https://graph.microsoft.com/v1.0/chats/%s/messages", chatId);

        ResponseEntity<String> response = restTemplate.exchange(
                graphUrl, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to send message: " + response.getStatusCode());
        }

        log.info("Message sent successfully to chat: {}", chatId);
    }
}