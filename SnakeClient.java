import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class SnakeClient {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8189;

        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1]);
                System.exit(1);
            }
        }

        try {
            // Set terminal to raw mode
            executeSttyCommand("-echo -cbreak");

            try (Socket socket = new Socket(host, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                System.out.println("Connected to Snake server. Use w,a,s,d to move. Type 'q' to quit.");

            // Thread to read from server and print to console
            Thread serverListener = new Thread(() -> {
                try {
                    String fromServer;
                    while ((fromServer = in.readLine()) != null) {
                        if (fromServer.equals("GAME OVER")) {
                            System.out.println("\n--- GAME OVER ---");
                            System.out.println("You crashed! Thanks for playing.");
                            System.exit(0); // Exit the client
                        }
                        System.out.println(fromServer);
                    }
                } catch (IOException e) {
                    // This can happen if the server closes the connection
                    System.out.println("\nConnection to server lost.");
                    System.exit(0);
                }
            });
            serverListener.start();

            // Main thread reads single chars from console and sends to server
            InputStreamReader consoleReader = new InputStreamReader(System.in);
            int fromUser;
            while (serverListener.isAlive() && (fromUser = consoleReader.read()) != -1) {
                char charFromUser = (char) fromUser;
                out.println(charFromUser);
                if (charFromUser == 'q') {
                    break;
                }
            }

            // Give the listener thread a moment to receive messages before exiting
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // If we break the loop, close the client
            System.exit(0);

            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + host);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + host);
            System.exit(1);
        } finally {
            // Restore terminal settings
            executeSttyCommand("echo cbreak");
        }
    }

    private static void executeSttyCommand(String args) {
        try {
            String[] cmd = {"/bin/sh", "-c", "stty " + args + " < /dev/tty"};
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            if (p.exitValue() != 0) {
                System.err.println("Warning: stty command failed. This may not work on your system.");
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Warning: Failed to execute stty command.");
        }
    }
}
