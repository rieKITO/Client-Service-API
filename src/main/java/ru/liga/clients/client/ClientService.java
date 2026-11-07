package ru.liga.clients.client;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.liga.clients.client.dto.ClientResponse;
import ru.liga.clients.client.dto.CreateClientRequest;
import ru.liga.clients.client.dto.UpdateClientRequest;
import ru.liga.clients.error.ClientNotFoundException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository repository;

    public ClientService(ClientRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ClientResponse create(CreateClientRequest request) {
        Client client = new Client(request.fullName(), request.gender());
        return ClientResponse.from(repository.saveAndFlush(client));
    }

    public List<ClientResponse> findAll() {
        return repository.findAll(Sort.by("id")).stream()
                .map(ClientResponse::from)
                .toList();
    }

    public ClientResponse findById(long id) {
        return ClientResponse.from(getClient(id));
    }

    @Transactional
    public ClientResponse update(long id, UpdateClientRequest request) {
        Client client = getClient(id);
        client.update(request.getFullName(), request.getGender(), request.getStatus());

        repository.flush();
        return ClientResponse.from(client);
    }

    @Transactional
    public void delete(long id) {
        repository.delete(getClient(id));
    }

    private Client getClient(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }

}
