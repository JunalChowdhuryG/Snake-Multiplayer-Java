
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SnakeServer {

    public static void main(String[] args) {
        int port = 8189;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Usage: java SnakeServer <port>");
                return;
            }
        }

        System.out.println("Starting Snake Server on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // A thread-safe list to hold handlers for all connected players
            List<PlayerHandler> players = new CopyOnWriteArrayList<>();

            // The main game instance
            Game game = new Game(players);
            Thread gameThread = new Thread(game);
            gameThread.start();

            int playerCounter = 0;
            // Expanded character set for more players
            char[] playerChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

            while (true) {
                Socket clientSocket = serverSocket.accept();

                System.out.println("New player connected: " + clientSocket.getInetAddress());
                // Cycle through the character set for new players
                char playerChar = playerChars[playerCounter % playerChars.length];

                PlayerHandler playerHandler = new PlayerHandler(clientSocket, game, playerChar, players);
                players.add(playerHandler);
                game.addNewPlayer(playerHandler);

                Thread playerThread = new Thread(playerHandler);
                playerThread.start();

                playerCounter++;
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}