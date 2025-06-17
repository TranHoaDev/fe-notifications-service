package com.fecredit.gbg.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class MessageRequest {
    @NotBlank
    private String chatId;
    @NotBlank
    private String content;
}