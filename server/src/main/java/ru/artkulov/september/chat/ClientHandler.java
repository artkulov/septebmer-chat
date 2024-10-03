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
    private static int userCount = 0;

    public String getNickname() {
        return nickname;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        userCount++;
        nickname = "user" + userCount;
        new Thread(() -> {
            try {
                System.out.println("Подключился новый клиент");
                out.writeUTF("Доступные команды: " +
                        "\nРегистрация /reg <ник> " +
                        "\nЛичное сообщение /w <ник> <сообщение>" +
                        "\nВыход из чата /exit");
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.startsWith("/w")) {
                            String[] parts = message.split(" ", 3);
                            if (parts.length == 3) {
                                sendPrivateMessage(parts[1], parts[2]);
                            } else {
                                out.writeUTF("Неверный формат команды. Используйте: /w <ник> <сообщение>");
                            }
                        }
                        if (message.equals("/exit")) {
                            sendMessage("/exitok");
                            break;
                        }
                        if (message.startsWith("/reg ")) {
                            String[] cmd = message.split(" ");
                            if (cmd.length == 2) {
                                this.nickname = cmd[1];
                            } else {
                                sendMessage("Неверный формат команды /reg. Используйте: /reg <ник>");
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
}
