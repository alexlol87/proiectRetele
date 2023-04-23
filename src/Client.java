import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;

public class Client {
    private String id;
    private Timer screenSharingTimer;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JFrame frame;
    private JPanel panel;
    private JTextField usernameField;
    private JButton connectButton;
    private JButton seeScreenButton;
    private JButton refreshListButton;
    private JTextArea userListArea;
    private String whoSharing;
    private int counter = 0;
    private ImageDisplay display = null; //pentru screen sharing


    public Client(String id) {
        this.id = id;
    }

    private void createGUI() {
        frame = new JFrame("Screen Sharing App " + id);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(400, 300);

        panel = new JPanel();
        frame.add(panel);

        usernameField = new JTextField(20);
        panel.add(usernameField);

        connectButton = new JButton("Connect");
        panel.add(connectButton);

        seeScreenButton = new JButton("See Screen");
        panel.add(seeScreenButton);

        refreshListButton = new JButton("Refresh List");
        panel.add(refreshListButton);

        userListArea = new JTextArea();
        userListArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(userListArea);
        panel.add(scrollPane);

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                if (!username.isEmpty()) {
                    frame.setTitle("Fereastra lui " + username);
                    id = username;
                    out.println("CONNECT:" + username);
                }
            }
        });

        seeScreenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                if (!username.isEmpty()) {
                    whoSharing = username;
                    out.println("START_SCREEN_SHARING:" + username);
                }
            }
        });

        refreshListButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                if (!username.isEmpty()) {
                    out.println("USER_LIST:" + " ");
                }
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Perform a specific action before closing the window

                out.println("DISCONNECT:" + " ");
                if (screenSharingTimer.isRunning())
                    screenSharingTimer.stop();
                // Close the window
                e.getWindow().dispose();
            }
        });

        frame.setVisible(true);
    }

    //...
    private void processServerMessages() {
        String serverMessage;

        try {
            JFrame frame = new JFrame();


            while ((serverMessage = in.readLine()) != null) {
                String[] tokens = serverMessage.split(":", 2);
                String command = tokens[0];

                if ("USERLIST".equals(command)) {
                    String userList = tokens[1];
                    System.out.println("User list: " + userList);
                    userListArea.setText(userList);
                    panel.updateUI();
                } else if ("ERROR".equals(command)) {
                    String errorMessage = tokens[1];
                    JOptionPane.showMessageDialog(frame, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                } else if ("START_SCREEN_SHARING".equals(command)) {

                    screenSharingTimer.start();
                } else if ("STOP_SCREEN_SHARING".equals(command)) {
                    System.out.println(id + " STOP_SCREEN_SHARING");
                    screenSharingTimer.stop();
                } else if ("SCREEN_IMAGE".equals(command)) {
                    String screenImageBase64 = tokens[1];
                    if (display == null) {
                        display = new ImageDisplay();
                    }
                    display.updateImage(screenImageBase64);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            System.out.println("id = " + id);
            socket = new Socket("localhost", 8080);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            createGUI();
            screenSharingTimer = new Timer(33, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        System.out.println(counter++);
                        byte[] screenImageBytes = ScreenCapture.captureScreen();
                        // Send screenImageBytes to the server

                        out.println("SCREEN_IMAGE:" + Base64.getEncoder().encodeToString(screenImageBytes));
                    } catch (AWTException | IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
            System.out.println("Client started");
            // Process server messages
            processServerMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ImageDisplay {
        private JFrame frame;
        private JLabel imageLabel;

        public ImageDisplay() {
            frame = new JFrame("Image Display");
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            imageLabel = new JLabel();
            frame.getContentPane().add(imageLabel);
            frame.pack();
            frame.setVisible(true);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // Perform a specific action before closing the window
                    // aici trebuie sa trimit semnal sa opreasca partajarea ecranului
                    out.println("STOP_SCREEN_SHARING:" + whoSharing);
                    System.out.println(whoSharing);
                    display = null;
                    // Close the window
                    e.getWindow().dispose();
                }
            });
        }

        public void updateImage(String base64Image) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                BufferedImage image = ImageIO.read(bais);
                //resize image
                Image scaledImage = image.getScaledInstance(image.getWidth() / 2, image.getHeight() / 2, Image.SCALE_SMOOTH);
                bais.close();
                imageLabel.setIcon(new ImageIcon(scaledImage));
                frame.setSize(image.getWidth() / 2, image.getHeight() / 2);
                frame.repaint();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void main(String[] args) {
            ImageDisplay display = new ImageDisplay();
            String screenImageBase64 = args[0]; // Pass the base64 image as a command-line argument
            display.updateImage(screenImageBase64);
        }
    }
}

