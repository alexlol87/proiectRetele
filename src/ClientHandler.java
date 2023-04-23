import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {
    private Socket socket;
    private String username;
    private int counter = 0;


    public ClientHandler(Socket socket) {
        System.out.println("Contstructorul ClientHandler");
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String clientMessage;
            String username = null;
            String screenSharingList = "";
            System.out.println("screen sharing list before while " + screenSharingList);

            String[] tokens = null;
            String command = null;

            while ((clientMessage = in.readLine()) != null) {
                tokens = clientMessage.split(":");
                command = tokens[0];
                if ("CONNECT".equals(command)) {
                    String proposedUsername = tokens[1];

                    if (!Server.usernameExists(proposedUsername)) {
                        username = proposedUsername;
                        Server.addClient(username, this);
                        if (this.username != null) {
                            Server.removeClient(this.username);
                        }
                        this.username = username;
                        out.println("USERLIST:" + Server.getUserList());
                    } else {
                        out.println("ERROR:Username already exists");
                    }
                } else if ("START_SCREEN_SHARING".equals(command)) {
                    if (Server.usernameExists(tokens[1])) {
                        Server.getScreenSharing().putIfAbsent(tokens[1], new ArrayList<>());
                        Server.getScreenSharing().get(tokens[1]).add(username);

                        System.out.println("I-am zis sa dea share la " + Server.getScreenSharing().get(tokens[1]));

                        Server.getClient(tokens[1]).send("START_SCREEN_SHARING:" + " ");
                    } else {
                        out.println("ERROR:User does not exist");
                    }
                } else if ("STOP_SCREEN_SHARING".equals(command)) {
                    if (Server.usernameExists(tokens[1])) {
                        Server.getScreenSharing().get(tokens[1]).remove(username);
                        System.out.println(username + " a parasit share-ul cu " + tokens[1]);
                        if (Server.getScreenSharing().get(tokens[1]).isEmpty())
                            Server.getClient(tokens[1]).send("STOP_SCREEN_SHARING:" + " ");
                    } else {
                        out.println("ERROR:User does not exist");
                    }
                } else if ("USER_LIST".equals(command)) {
                    out.println("USERLIST:" + Server.getUserList());
                } else if ("SCREEN_IMAGE".equals(command)) {
                    String[] finalTokens = tokens;
//                        if (!Server.getScreenSharing().get(username).isEmpty()) {
//                            Server.getScreenSharing().get(username).stream().forEach(user -> {
//                                System.out.println("I-am zis sa dea share la " + user + " " + counter++);
//                                Server.getClient(user).send("SCREEN_IMAGE:" + finalTokens[1]);
//                            });
//                        }
                    if (!Server.getScreenSharing().get(username).isEmpty()) {
                        CopyOnWriteArrayList<String> usersToReceiveScreen = new CopyOnWriteArrayList<>(Server.getScreenSharing().get(username));

                        Iterator<String> iterator = usersToReceiveScreen.iterator();
                        while (iterator.hasNext()) {
                            String user = iterator.next();
                            System.out.println("I-am zis sa dea share la " + user + " " + counter++);
                            Server.getClient(user).send("SCREEN_IMAGE:" + finalTokens[1]);
                        }
                    }

                } else if ("DISCONNECT".equals(command)) {
                    Server.removeClient(username);
                    socket.close();
                    break;
                }
            }

        } catch (Exception e) {
            synchronized (Server.class) {
                Server.getScreenSharing().remove(username);
                Server.getScreenSharing().forEach((key, value) -> {
                    if (value.contains(username)) {
                        value.remove(username);
                        if (value.isEmpty()) {
                            Server.getClient(key).send("STOP_SCREEN_SHARING:" + username);
                        }
                    }
                });
                Server.removeClient(username);
            }

            e.printStackTrace();
        }
    }

    public void send(String message) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
