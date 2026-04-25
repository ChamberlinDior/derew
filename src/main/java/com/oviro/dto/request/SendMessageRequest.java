package com.oviro.dto.request;

import com.oviro.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank(message = "Le contenu du message est obligatoire")
    @Size(max = 500, message = "Le message ne peut pas dépasser 500 caractères")
    private String content;

    private MessageType type = MessageType.TEXT;
}
