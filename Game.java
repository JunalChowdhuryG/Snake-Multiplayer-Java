import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Game implements Runnable {
    private Board board;
    private final List<Snake> snakes = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;
    private final List<PlayerHandler> players;
    private int currentLevel;
    private int gameSpeed;
    private static final int LEVEL_UP_SCORE_THRESHOLD = 50;

    public Game(List<PlayerHandler> players) {
        this.players = players;
        this.currentLevel = 1;
        this.gameSpeed = 250; // Slower start
        this.board = new Board(this.currentLevel);
    }

    public synchronized void addNewPlayer(PlayerHandler player) {
        List<Point> spawnPoints = board.getSafeSpawnPoints();
        if (spawnPoints.isEmpty()) {
            player.sendMessage("Sorry, the game is full or there's no space to spawn.");
            player.closeConnection();
            return;
        }

        // A quick check to avoid spawning on other snakes, though getSafeSpawnPoints should be enough
        // for static maps. This is more for robustness.
        List<Point> occupied = new ArrayList<>();
        for (Snake s : snakes) {
            occupied.addAll(s.getBody());
        }
        spawnPoints.removeAll(occupied);

        if (spawnPoints.isEmpty()) {
             player.sendMessage("Sorry, could not find a safe spawn point.");
             player.closeConnection();
             return;
        }

        Random rand = new Random();
        Point spawnPoint = spawnPoints.get(rand.nextInt(spawnPoints.size()));

        Snake snake = new Snake(spawnPoint.x, spawnPoint.y, player.getPlayerChar());
        player.setSnake(snake);
        snakes.add(snake);
    }

    public void removeSnake(Snake snake) {
        snakes.remove(snake);
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            tick();
            broadcastGameState();
            try {
                Thread.sleep(gameSpeed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    private void tick() {
        if (snakes.isEmpty()) return;

        // Move all snakes
        for (Snake snake : snakes) {
            snake.move();
        }

        List<Snake> snakesToRemove = new ArrayList<>();
        // Check for collisions and fruit
        for (Snake snake : snakes) {
            Point head = snake.getHead();

            // Find the player for this snake to send debug messages
            PlayerHandler player = null;
            for (PlayerHandler p : players) {
                if (p.getSnake() == snake) {
                    player = p;
                    break;
                }
            }

            // Check wall collision
            if (board.getCell(head.x, head.y) == '#') {
                if (player != null) player.sendMessage("DEBUG: Wall collision at (" + head.x + ", " + head.y + ")");
                snakesToRemove.add(snake);
                continue;
            }
            // Check self-collision
            if (snake.checkSelfCollision()) {
                if (player != null) {
                    StringBuilder bodyState = new StringBuilder("DEBUG: Self collision. Body: ");
                    for(Point p : snake.getBody()) {
                        bodyState.append("(").append(p.x).append(",").append(p.y).append(") ");
                    }
                    player.sendMessage(bodyState.toString());
                }
                snakesToRemove.add(snake);
                continue;
            }
            // Check collision with other snakes
            for (Snake otherSnake : snakes) {
                if (snake == otherSnake) continue;
                for (Point bodyPart : otherSnake.getBody()) {
                    if (head.equals(bodyPart)) {
                        if (player != null) player.sendMessage("DEBUG: Other snake collision");
                        snakesToRemove.add(snake);
                        break;
                    }
                }
                if (snakesToRemove.contains(snake)) break;
            }
            // Check for fruit
            char cellContent = board.getCell(head.x, head.y);
            if (cellContent >= '1' && cellContent <= '9') {
                int fruitValue = Character.getNumericValue(cellContent);
                snake.addScore(fruitValue);
                snake.grow(); // Grow by 1 segment
                board.setCell(head.x, head.y, ' ');
                board.placeFruit();
            }
        }

        for (Snake snake : snakesToRemove) {
            for (PlayerHandler player : players) {
                if (player.getSnake() == snake) {
                    player.sendMessage("GAME OVER");
                    player.closeConnection();
                    break;
                }
            }
            snakes.remove(snake);
        }

        checkLevelUp();
    }

    private void checkLevelUp() {
        int totalScore = 0;
        for (Snake snake : snakes) {
            totalScore += snake.getScore();
        }

        if (totalScore >= (currentLevel * LEVEL_UP_SCORE_THRESHOLD)) {
            nextLevel();
        }
    }

    private void nextLevel() {
        currentLevel++;
        if (currentLevel > Board.getMaxLevels()) {
            broadcastMessage("YOU WIN! All levels completed!");
            running = false;
            return;
        }

        broadcastMessage("LEVEL UP! Welcome to Level " + currentLevel);
        gameSpeed *= 0.8; // Increase speed by 20%
        board = new Board(currentLevel);

        // Reset all snakes to new positions
        List<Point> spawnPoints = board.getSafeSpawnPoints();
        Collections.shuffle(spawnPoints);

        for (int i = 0; i < snakes.size(); i++) {
            if (i < spawnPoints.size()) {
                Point p = spawnPoints.get(i);
                snakes.get(i).reset(p.x, p.y);
            } else {
                // Not enough spawn points, remove extra snakes (should not happen with good level design)
                removeSnake(snakes.get(i));
            }
        }
    }

    private void broadcastMessage(String message) {
        for (PlayerHandler player : players) {
            player.sendMessage(message);
        }
    }

    private void broadcastGameState() {
        String gameState = render();
        broadcastMessage(gameState);
    }

    private String render() {
        // ... (render method remains the same, but let's add level display)
        char[][] grid = board.getGrid();
        char[][] tempGrid = new char[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            tempGrid[i] = grid[i].clone();
        }

        for (Snake snake : snakes) {
            char bodyChar = snake.getBodyChar();
            for (Point p : snake.getBody()) {
                if (p.y >= 0 && p.y < board.getHeight() && p.x >= 0 && p.x < board.getWidth()) {
                    tempGrid[p.y][p.x] = bodyChar;
                }
            }
            Point head = snake.getHead();
             if (head.y >= 0 && head.y < board.getHeight() && head.x >= 0 && head.x < board.getWidth()) {
                tempGrid[head.y][head.x] = 'O';
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\033[H\033[2J");
        sb.append("--- Snake vs Snakes --- Level: ").append(currentLevel).append(" ---\n");
        for (int i = 0; i < board.getHeight(); i++) {
            sb.append(new String(tempGrid[i])).append("\n");
        }

        sb.append("--- Top 3 Players ---\n");
        List<Snake> sortedSnakes = new ArrayList<>(snakes);
        sortedSnakes.sort((s1, s2) -> Integer.compare(s2.getScore(), s1.getScore()));

        int rank = 1;
        int totalScore = 0;
        for (Snake snake : sortedSnakes) {
            if (rank <= 3) {
                sb.append(rank).append(". Player '").append(snake.getBodyChar()).append("': ").append(snake.getScore()).append("\n");
            }
            rank++;
            totalScore += snake.getScore();
        }
        sb.append("--------------------\n");
        sb.append("Level Up In: ").append(Math.max(0, (currentLevel * LEVEL_UP_SCORE_THRESHOLD) - totalScore)).append(" points\n");

        return sb.toString();
    }
}
