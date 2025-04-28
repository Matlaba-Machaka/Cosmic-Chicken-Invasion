package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Random;

import static game.CosmicChickenInvasion.GamePanel.INVINCIBILITY_DURATION;

public class CosmicChickenInvasion extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private GamePanel gamePanel;

    public CosmicChickenInvasion() {
        setTitle("Cosmic Chicken Invasion");
        setSize(WIDTH, HEIGHT); // Fixed size
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        add(gamePanel);
        setVisible(true);

        // Make sure focus is properly set AFTER visible
        SwingUtilities.invokeLater(() -> {
            gamePanel.requestFocusInWindow(); // this must come AFTER setVisible
            gamePanel.initializeGame();
        });
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(CosmicChickenInvasion::new);
    }

    class GamePanel extends JPanel {
        private Player player;
        private ArrayList<Chicken> chickens;
        private ArrayList<Pitchfork> pitchforks;
        private Random rand;
        private boolean running;
        private boolean gameOver;
        private int score;
        private int lives;
        private long lastHitTime;
        static final int INVINCIBILITY_DURATION = 1500; // ms
        private int maxChickens = 5;


        public GamePanel() {
            setFocusable(true);
            rand = new Random();
            chickens = new ArrayList<>();
            pitchforks = new ArrayList<>();
            System.out.println("GamePanel constructor: WIDTH=" + WIDTH + ", HEIGHT=" + HEIGHT);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    handleKeyPressed(e);
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    handleKeyReleased(e);
                }
            });

            new Thread(this::gameLoop).start();
        }

        public void initializeGame() {
            resetGame(); // no async thread needed anymore
        }


        private void resetGame() {
            player = new Player(getWidth() / 2, getHeight() - 100);
            lastHitTime = 0;
            maxChickens = 5;
            chickens.clear();
            pitchforks.clear();
            score = 0;
            lives = 3;
            running = false;
            gameOver = false;
            spawnChickens();
        }


        private void spawnChickens() {
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int boundX = panelWidth - 20;
            int boundY = panelHeight / 2;

            if (boundX <= 0 || boundY <= 0) {
                System.out.println("Warning: Invalid panel size. Skipping spawn.");
                return;
            }

            while (chickens.size() < 5) {
                int x = rand.nextInt(boundX);
                int y = rand.nextInt(boundY);
                chickens.add(new Chicken(x, y, rand));
            }
        }
        private void gameLoop() {
            while (true) {
                if (running) {
                    update();
                }
                repaint();
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        private void update() {
            player.update();

            for (Chicken c : new ArrayList<>(chickens)) {
                c.update(score);
                if (c.getBounds().intersects(player.getBounds())) {
                    long now = System.currentTimeMillis();
                    if (now - lastHitTime > INVINCIBILITY_DURATION) {
                        chickens.remove(c);
                        lives--;
                        lastHitTime = now;

                        if (lives <= 0) {
                            running = false;
                            gameOver = true;
                        }

                        spawnChickens();
                        break;
                    }
                }
            }

            for (Pitchfork p : new ArrayList<>(pitchforks)) {
                p.update();
                if (p.y < 0) {
                    pitchforks.remove(p);
                } else {
                    for (Chicken c : new ArrayList<>(chickens)) {
                        if (p.getBounds().intersects(c.getBounds())) {
                            chickens.remove(c);
                            pitchforks.remove(p);
                            score += 10;
                            if (score % 50 == 0 && maxChickens < 15) {
                                maxChickens++;
                            }
                            spawnChickens();
                            break;
                        }
                    }
                }
            }
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            FontMetrics fm = g2d.getFontMetrics();

            if (!running && !gameOver) {
                g2d.setColor(Color.YELLOW);
                String title = "COSMIC CHICKEN INVASION";
                g2d.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, getHeight() / 2 - 20);

                String prompt = "Press SPACE to Start";
                g2d.drawString(prompt, (getWidth() - fm.stringWidth(prompt)) / 2, getHeight() / 2 + 20);

                g2d.setColor(Color.WHITE);
                String controls = "Controls: LEFT/RIGHT to move, SPACE to shoot";
                g2d.drawString(controls, (getWidth() - fm.stringWidth(controls)) / 2, getHeight() / 2 + 50);
            } else if (gameOver) {
                g2d.setColor(Color.RED);
                g2d.drawString("GAME OVER - Score: " + score, (getWidth() - fm.stringWidth("GAME OVER - Score: " + score)) / 2, getHeight() / 2 - 20);
                g2d.drawString("Press SPACE to Restart", (getWidth() - fm.stringWidth("Press SPACE to Restart")) / 2, getHeight() / 2 + 20);
            } else {
                player.draw(g2d);
                for (Chicken c : chickens) c.draw(g2d);
                for (Pitchfork p : pitchforks) p.draw(g2d);
                g2d.setColor(Color.YELLOW);
                g2d.drawString("Score: " + score, 10, 20);
                g2d.drawString("Lives: " + lives, WIDTH - 80, 20);
            }
        }

        private void handleKeyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE && !running && !gameOver) {
                running = true;
            } else if (e.getKeyCode() == KeyEvent.VK_SPACE && gameOver) {
                resetGame();
                running = true;
            }
            player.handleInput(e, true);
        }

        private void handleKeyReleased(KeyEvent e) {
            player.handleInput(e, false);
        }
    }

    class Player {
        private int x, y;
        private boolean left, right, shooting;
        private long lastShotTime;
        private static final int SPEED = 5;
        private static final int SHOT_DELAY = 300;

        public Player(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void handleInput(KeyEvent e, boolean pressed) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) left = pressed;
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) right = pressed;
            if (e.getKeyCode() == KeyEvent.VK_SPACE) shooting = pressed;
        }

        public void update() {
            if (left && x > 0) x -= SPEED;
            if (right && x < WIDTH - 50) x += SPEED;
            if (shooting && System.currentTimeMillis() - lastShotTime > SHOT_DELAY) {
                CosmicChickenInvasion.this.gamePanel.pitchforks.add(new Pitchfork(x + 20, y - 20));
                lastShotTime = System.currentTimeMillis();
            }
        }

        public void draw(Graphics2D g) {
            long now = System.currentTimeMillis();
            boolean flashing = (now - gamePanel.lastHitTime) < INVINCIBILITY_DURATION;

            if (!flashing || (now / 100 % 2 == 0)) {
                g.setColor(Color.GREEN);
                g.fillRect(x, y, 50, 30);
                g.setColor(Color.GRAY);
                g.fillRect(x + 20, y - 20, 10, 20);
            }
        }


        public Rectangle getBounds() {
            return new Rectangle(x, y, 50, 30);
        }
    }

    class Chicken {
        private int x, y, dx, dy;
        private boolean teleporting, divebombing;
        private Random rand;

        public Chicken(int x, int y, Random rand) {
            this.x = x;
            this.y = y;
            this.rand = rand;
            this.divebombing = false;
            dx = (rand.nextBoolean() ? 1 : -1) * 3;
            dy = 2;
        }

        public void update(int score) {
            // Speed up with score

            dy = 2 + score / 100;

            // Occasionally divebomb
            if (!divebombing && rand.nextInt(300) == 0) {
                dy += 6;
                divebombing = true;
            }

            x += dx;
            y += dy;

            // Teleporting
            if (rand.nextInt(100) < 5) {
                x = rand.nextInt(780);
                y = rand.nextInt(300);
                teleporting = true;
            }

            if (x < 0 || x > 780) dx = -dx;
            if (y > 600) {
                y = 0;
                divebombing = false;
            }
        }

        public void draw(Graphics2D g) {
            // Body
            g.setColor(teleporting ? Color.MAGENTA : new Color(255, 255, 200)); // pale yellow chicken body
            g.fillOval(x, y, 24, 24);

            // Head bump
            g.setColor(Color.RED);
            g.fillOval(x + 6, y - 6, 12, 12); // red crest

            // Eyes
            g.setColor(Color.BLACK);
            g.fillOval(x + 6, y + 6, 4, 4);
            g.fillOval(x + 14, y + 6, 4, 4);

            // Beak
            g.setColor(Color.ORANGE);
            g.fillPolygon(
                    new int[]{x + 11, x + 13, x + 12},
                    new int[]{y + 14, y + 14, y + 20},
                    3
            );

            // Feet
            g.setColor(Color.ORANGE);
            g.drawLine(x + 8, y + 24, x + 8, y + 28);
            g.drawLine(x + 16, y + 24, x + 16, y + 28);

            teleporting = false;
        }


        public Rectangle getBounds() {
            return new Rectangle(x, y, 24, 28);
        }
    }

    class Pitchfork {
        private int x, y;
        private static final int SPEED = 7;

        public Pitchfork(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void update() {
            y -= SPEED;
        }

        public void draw(Graphics2D g) {
            g.setColor(Color.GRAY);
            g.fillRect(x, y, 5, 20);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, 5, 20);
        }
    }
}
