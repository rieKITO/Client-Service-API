package ru.liga.clients.error;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(long id) {
        super("Клиент с id " + id + " не найден");
    }

}
