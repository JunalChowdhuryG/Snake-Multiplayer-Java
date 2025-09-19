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

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to Snake server. Use w,a,s,d and Enter to move. Type 'quit' to exit.");

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

            // Main thread reads from console and sends to server
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            String fromUser;
            while (serverListener.isAlive() && (fromUser = stdIn.readLine()) != null) {
                out.println(fromUser);
                if (fromUser.equalsIgnoreCase("quit")) {
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

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + host);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + host);
            System.exit(1);
        }
    }
}
