package ru.liga.clients.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import ru.liga.clients.client.Gender;

public record CreateClientRequest(

        @NotBlank(message = "ФИО не должно быть пустым")
        @Schema(description = "ФИО клиента", example = "Иванов Иван Иванович")
        String fullName,

        @Schema(description = "Пол: M — мужской, F — женский. Можно не указывать", example = "M")
        Gender gender

) { }
