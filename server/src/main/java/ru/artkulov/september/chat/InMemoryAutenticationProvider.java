package ru.artkulov.september.chat;

import java.util.ArrayList;
import java.util.List;

public class InMemoryAutenticationProvider implements AuthenticationProvider {
    private class User {
        private String login;
        private String password;
        private String nickname;
        private String role;

        public User(String login, String password, String nickname, String role) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
            this.role = role;
        }
    }

    private Server server;
    private List<User> users;

    public InMemoryAutenticationProvider(Server server) {
        this.server = server;
        this.users = new ArrayList<>();
        this.users.add(new User("admin", "admin", "admin", "ADMIN"));
        this.users.add(new User("login2", "pass2", "user2", "USER"));
        this.users.add(new User("login3", "pass3", "user3", "USER"));
    }

    @Override
    public void initialize() {
        System.out.println("Сервис аутентификации запущен");
    }

    private String getNicknameByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.nickname;
            }
        }
        return null;
    }

    private boolean isLoginAlreadyExist(String login) {
        for (User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNicknameAlreadyExist(String nickname) {
        for (User u : users) {
            if (u.nickname.equals(nickname)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) {
        String authNickname = getNicknameByLoginAndPassword(login, password);
        if (authNickname == null) {
            clientHandler.sendMessage("Некорректный логин/пароль");
            return false;
        }
        if (server.isNicknameBusy(authNickname)) {
            clientHandler.sendMessage("Указанная учетная запись уже занята");
            return false;
        }
        clientHandler.setNickname(authNickname);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authNickname);
        return true;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String nickname, String role) {
        if (login.trim().length() < 3 || password.trim().length() < 6 || nickname.trim().length() < 1 || role.trim().length() < 4) {
            clientHandler.sendMessage("Логин 3+ символа, Пароль 6+ символов, Никнейм 1+ символ");
            return false;
        }
        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят");
            return false;
        }
        if (isNicknameAlreadyExist(nickname)) {
            clientHandler.sendMessage("Указанный никнейм уже занят");
            return false;
        }
        users.add(new User(login, password, nickname, role));
        clientHandler.setNickname(nickname);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/regok " + nickname);
        return true;
    }

    @Override
    public String getRole(ClientHandler clientHandler) {
        for (User u : users) {
            if (u.nickname.equals(clientHandler.getNickname())) {
                return u.role;
            }
    }
        return null;
    }
}
