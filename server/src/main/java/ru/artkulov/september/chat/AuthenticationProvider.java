package ru.artkulov.september.chat;

public interface AuthenticationProvider {
    void initialize();

    boolean authenticate(ClientHandler clientHandler, String login, String password);

    boolean registration(ClientHandler clientHandler, String login, String password, String username, String role);

    String getRole(ClientHandler clientHandler);
}
