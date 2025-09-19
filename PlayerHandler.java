import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class PlayerHandler implements Runnable {
    private Socket socket;
    private Game game;
    private Snake snake;
    private PrintWriter out;
    private List<PlayerHandler> players;
    private volatile boolean running = true;
    private final char playerChar;

    public PlayerHandler(Socket socket, Game game, char playerChar, List<PlayerHandler> players) {
        this.socket = socket;
        this.game = game;
        this.playerChar = playerChar;
        this.players = players;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String inputLine;
            while (running && (inputLine = in.readLine()) != null) {
                if (snake == null) continue; // Don't process commands if snake isn't ready

                switch (inputLine.trim().toLowerCase()) {
                    case "w":
                        snake.setDirection(Snake.Direction.UP);
                        break;
                    case "s":
                        snake.setDirection(Snake.Direction.DOWN);
                        break;
                    case "a":
                        snake.setDirection(Snake.Direction.LEFT);
                        break;
                    case "d":
                        snake.setDirection(Snake.Direction.RIGHT);
                        break;
                    case "quit":
                        running = false;
                        break;
                }
            }
        } catch (IOException e) {
            // This is expected when a client disconnects
        } finally {
            closeConnection();
        }
    }

    public void sendMessage(String message) {
        if (out != null && !out.checkError()) {
            out.println(message);
        }
    }

    public void closeConnection() {
        this.running = false;
        if (snake != null) {
            game.removeSnake(snake);
        }
        players.remove(this);
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Player " + playerChar + " connection closed.");
    }

    public Snake getSnake() {
        return snake;
    }

    public void setSnake(Snake snake) {
        this.snake = snake;
    }

    public char getPlayerChar() {
        return playerChar;
    }
}
