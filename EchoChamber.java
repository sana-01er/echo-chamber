import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class EchoChamber extends JPanel {

    // ==================== CONFIG ====================
    private static final int ROWS = 6;
    private static final int COLS = 6;
    private static final int TILE = 70;

    // Tile types
    private static final int EMPTY = 0;
    private static final int WALL = 1;
    private static final int CRYSTAL = 2;

    // ==================== DIRECTION ====================
    private enum Direction {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        final int dx, dy;
        Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }

        Direction turnRight() {
            return switch (this) {
                case UP -> RIGHT;
                case RIGHT -> DOWN;
                case DOWN -> LEFT;
                case LEFT -> UP;
            };
        }
    }

    // ==================== CRYSTAL COLOR ====================
    private enum GemColor { RED, BLUE, GREEN }

    // ==================== BOARD STATE ====================
    private final int[][] tiles = new int[ROWS][COLS];
    private final GemColor[][] crystals = new GemColor[ROWS][COLS];
    private final List<int[]> lastPath = new ArrayList<>();
    private Direction pendingDir = Direction.RIGHT;

    // ==================== CONSTRUCTOR ====================
    public EchoChamber() {
        setPreferredSize(new Dimension(COLS * TILE, ROWS * TILE));
        setBackground(new Color(26, 26, 46));
        buildLevel();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    pendingDir = pendingDir.turnRight();
                } else {
                    int c = e.getX() / TILE;
                    int r = e.getY() / TILE;
                    if (inBounds(r, c) && tiles[r][c] == EMPTY) {
                        simulateWave(r, c, pendingDir);
                    }
                }
                repaint();
            }
        });
    }

    private void buildLevel() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                tiles[r][c] = EMPTY;

        tiles[2][2] = WALL;
        tiles[2][3] = WALL;
        tiles[4][1] = WALL;

        placeCrystal(0, 0, GemColor.RED);
        placeCrystal(0, 5, GemColor.BLUE);
        placeCrystal(5, 0, GemColor.GREEN);
        placeCrystal(5, 5, GemColor.RED);
    }

    private void placeCrystal(int r, int c, GemColor color) {
        tiles[r][c] = CRYSTAL;
        crystals[r][c] = color;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS;
    }

    // ==================== WAVE SIMULATION ====================
    private void simulateWave(int startRow, int startCol, Direction startDir) {
        lastPath.clear();

        int r = startRow, c = startCol;
        Direction dir = startDir;
        int bounces = 0;

        while (bounces < 50) {
            int nr = r + dir.dy;
            int nc = c + dir.dx;

            if (!inBounds(nr, nc) || tiles[nr][nc] == WALL) {
                dir = dir.turnRight();
                bounces++;
                lastPath.add(new int[]{r, c});
                continue;
            }

            r = nr;
            c = nc;
            lastPath.add(new int[]{r, c});

            if (tiles[r][c] == CRYSTAL) {
                cycleColor(r, c);
                dir = dir.turnRight();
                bounces++;
            }

            if (r == startRow && c == startCol && bounces > 0) break;
        }
    }

    private void cycleColor(int r, int c) {
        crystals[r][c] = switch (crystals[r][c]) {
            case RED -> GemColor.BLUE;
            case BLUE -> GemColor.GREEN;
            case GREEN -> GemColor.RED;
        };
    }

    private boolean isSolved() {
        GemColor target = null;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (tiles[r][c] == CRYSTAL) {
                    if (target == null) target = crystals[r][c];
                    else if (crystals[r][c] != target) return false;
                }
            }
        }
        return true;
    }

    // ==================== RENDERING ====================
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw tiles
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int x = c * TILE;
                int y = r * TILE;

                if (tiles[r][c] == WALL) {
                    g.setColor(new Color(68, 68, 68));
                    g.fillRect(x, y, TILE, TILE);
                } else if (tiles[r][c] == CRYSTAL) {
                    g.setColor(switch (crystals[r][c]) {
                        case RED -> new Color(220, 20, 60);
                        case BLUE -> new Color(30, 144, 255);
                        case GREEN -> new Color(50, 205, 50);
                    });
                    g.fillOval(x + 10, y + 10, TILE - 20, TILE - 20);
                } else {
                    g.setColor(new Color(44, 44, 74));
                    g.drawRect(x, y, TILE, TILE);
                }
            }
        }

        // Draw wave path
        if (!lastPath.isEmpty()) {
            g.setColor(new Color(255, 204, 0, 190));
            g.setStroke(new BasicStroke(3f));
            int[] first = lastPath.get(0);
            int prevX = first[1] * TILE + TILE / 2;
            int prevY = first[0] * TILE + TILE / 2;
            for (int i = 1; i < lastPath.size(); i++) {
                int[] p = lastPath.get(i);
                int x = p[1] * TILE + TILE / 2;
                int y = p[0] * TILE + TILE / 2;
                g.drawLine(prevX, prevY, x, y);
                prevX = x;
                prevY = y;
            }
        }

        // Aim indicator + instructions
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Aim: " + pendingDir + "   (Right-click to rotate)", 10, 22);

        if (isSolved()) {
            g.setColor(new Color(255, 255, 100));
            g.setFont(new Font("SansSerif", Font.BOLD, 28));
            g.drawString("SOLVED!", 10, ROWS * TILE - 20);
        }
    }

    // ==================== ENTRY POINT ====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Echo Chamber");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new EchoChamber());
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
