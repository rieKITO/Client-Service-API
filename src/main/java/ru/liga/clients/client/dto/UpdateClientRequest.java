package ru.liga.clients.client.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import ru.liga.clients.client.ClientStatus;
import ru.liga.clients.client.Gender;

@Schema(description = "Обновляются только переданные поля. Явный null запрещён. Пустой объект ничего не меняет")
public class UpdateClientRequest {

    @Pattern(regexp = "(?sU).*\\S.*", message = "ФИО не должно быть пустым")
    @Schema(description = "Новое ФИО", example = "Иванов Иван Петрович")
    private String fullName;

    @Schema(description = "Новый пол", example = "M")
    private Gender gender;

    @Schema(description = "Новый статус", example = "INACTIVE")
    private ClientStatus status;

    public String getFullName() {
        return fullName;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Gender getGender() {
        return gender;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public ClientStatus getStatus() {
        return status;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setStatus(ClientStatus status) {
        this.status = status;
    }

}
