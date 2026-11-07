package ru.liga.clients.client;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.liga.clients.client.dto.ClientResponse;
import ru.liga.clients.client.dto.CreateClientRequest;
import ru.liga.clients.client.dto.UpdateClientRequest;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Клиенты", description = "Учёт клиентов компании")
@ApiResponse(
        responseCode = "400",
        description = "Некорректный запрос",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
        )
)
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Создать клиента", description = "Новый клиент всегда имеет статус ACTIVE")
    @ApiResponse(responseCode = "201", description = "Клиент создан")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request) {
        ClientResponse client = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(client.id()).toUri();
        return ResponseEntity.created(location).body(client);
    }

    @GetMapping
    @Operation(summary = "Получить всех клиентов", description = "Список отсортирован по id по возрастанию")
    @ApiResponse(responseCode = "200", description = "Список клиентов, в том числе пустой")
    public List<ClientResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить клиента по id")
    @ApiResponse(responseCode = "200", description = "Клиент найден")
    @ApiResponse(
            responseCode = "404",
            description = "Клиент не найден",
            content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class
                    )
            )
    )
    public ClientResponse findById(@PathVariable long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить клиента", description = "Пропущенные поля сохраняются. Явный null запрещён")
    @ApiResponse(responseCode = "200", description = "Актуальное состояние клиента")
    @ApiResponse(
            responseCode = "404",
            description = "Клиент не найден",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ClientResponse update(@PathVariable long id, @Valid @RequestBody UpdateClientRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить клиента", description = "Запись физически удаляется из базы данных")
    @ApiResponse(responseCode = "204", description = "Клиент удалён", content = @Content)
    @ApiResponse(
            responseCode = "404",
            description = "Клиент не найден",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
