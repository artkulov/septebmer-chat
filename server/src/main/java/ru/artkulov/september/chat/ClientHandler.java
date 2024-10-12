package ru.artkulov.september.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private String role; // Роль по умолчанию

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                System.out.println("Подключился новый клиент");
                sendMessage("Доступные команды: " +
                        "\nАутентификация /auth <логин> <пароль> " +
                        "\nРегистрация /reg <логин> <пароль> <ник> " +
                        "\nОтключить пользователя (только для администратора) /kick <ник> " +
                        "\nЛичное сообщение /w <ник> <сообщение>" +
                        "\nВыход из чата /exit");
                sendMessage("Перед работой необходимо выполнить аутентификацию или регистрацию");
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/auth ")) {
                        String[] elements = message.split(" ");
                        if (elements.length != 3) {
                            sendMessage("Неверный формат команды /auth");
                            continue;
                        }
                        if (server.getAuthenticationProvider().authenticate(this, elements[1], elements[2])) {
                            this.role = server.getAuthenticationProvider().getRole(this);
                            break;
                        }
                        continue;
                    }
                    if (message.startsWith("/reg ")) {
                        String[] elements = message.split(" ");
                        if (elements.length != 4) {
                            sendMessage("Неверный формат команды /reg");
                            continue;
                        }
                        if (server.getAuthenticationProvider().registration(this, elements[1], elements[2], elements[3], elements[4])) {
                            break;
                        }
                        continue;
                    }
                }
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.startsWith("/w ")) {
                            String[] parts = message.split(" ", 3);
                            if (parts.length == 3) {
                                sendPrivateMessage(parts[1], parts[2]);
                            } else {
                                out.writeUTF("Неверный формат команды. Используйте: /w <ник> <сообщение>");
                            }
                        }
                        if (message.equals("/exit")) {
                            sendMessage("/exitok ");
                            break;
                        }
                        if (message.startsWith("/kick ") && role.equals("ADMIN")) {
                            String[] parts = message.split(" ");
                            if (parts.length == 2) {
                                kickUser(parts[1]);
                            } else {
                                sendMessage("Неверный формат команды(Используйте: /kick <ник>) или нет доступа к команде.");
                                continue;
                            }
                        }
                        continue;
                    }
                    server.broadcastMessage(nickname + ": " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    private void sendPrivateMessage(String recipientNickname, String message) throws IOException {
        for (ClientHandler client : server.clients) {
            if (client.nickname.equals(recipientNickname)) {
                client.out.writeUTF("Личное сообщение от " + nickname + ": " + message);
                return;
            }
        }
        out.writeUTF("Пользователь с ником " + recipientNickname + " не найден.");
    }


    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void kickUser(String userToKick) throws IOException {
        for (ClientHandler c : server.clients) {
            if (c.getNickname().equals(userToKick)) {
                c.sendMessage("Вы были отключены администратором.");
                c.disconnect();
                server.broadcastMessage(userToKick + " был отключен администратором.");
                return;
            }
        }
        out.writeUTF("Пользователь с ником " + userToKick + " не найден.");
    }

}
