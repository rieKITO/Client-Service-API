package ru.liga.clients.client.dto;

import ru.liga.clients.client.Client;
import ru.liga.clients.client.ClientStatus;
import ru.liga.clients.client.Gender;

import java.time.Instant;

public record ClientResponse(
        Long id,
        String fullName,
        Gender gender,
        ClientStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getFullName(),
                client.getGender(),
                client.getStatus(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }

}
