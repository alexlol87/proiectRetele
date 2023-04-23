import com.sun.source.tree.LiteralTree;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static HashMap<String, ClientHandler> clients = new HashMap<>();
    private static HashMap<String, List<String>> screenSharing = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");
                ClientHandler handler = new ClientHandler(socket);
                Thread handlerThread = new Thread(handler);
                handlerThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void shareScreen(String username) {
        // ...
    }

    public static void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        sendUserListToAll();
    }

    public static void removeClient(String username) {
        clients.remove(username);
        sendUserListToAll();
    }

    public static boolean usernameExists(String username) {
        return clients.containsKey(username);
    }

    public static String getUserList() {
        return String.join(", ", clients.keySet());
    }

    public static HashMap<String, ClientHandler> getClients() {
        return clients;
    }

    public static ClientHandler getClient(String username) {
        return clients.get(username);
    }

    public static HashMap<String, List<String>> getScreenSharing() {
        return screenSharing;
    }

    private static void sendUserListToAll() {
        String userList = getUserList();
        for (ClientHandler handler : clients.values()) {
            handler.send("USERLIST:" + userList);
        }
    }
}
