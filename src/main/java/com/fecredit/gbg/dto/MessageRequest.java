package com.fecredit.gbg.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class MessageRequest {
    @NotBlank(message = "Chat ID cannot be blank")
    private String chatId;
    @NotBlank(message = "Content cannot be blank")
    private String content;
}