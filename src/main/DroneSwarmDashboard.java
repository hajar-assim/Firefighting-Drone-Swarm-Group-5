package main;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DroneSwarmDashboard extends JFrame {

    public static final int CELL_SIZE = 40;
    private static final int GRID_WIDTH = 20;
    private static final int GRID_HEIGHT = 15;

    private final Map<Point, CellType> zoneMap = new HashMap<>();
    private final Map<Integer, Point> dronePositions = new HashMap<>();
    private final Map<Integer, DroneState> droneStates = new HashMap<>();
    private final Map<Integer, Point> zoneLabels = new HashMap<>();


    public DroneSwarmDashboard() {
        setTitle("Firefighting Drone Swarm Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GridPanel panel = new GridPanel();
        add(panel, BorderLayout.CENTER);

        pack();
        setVisible(true);
    }


    private class GridPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // draw grid
            for (int x = 0; x < GRID_WIDTH; x++) {
                for (int y = 0; y < GRID_HEIGHT; y++) {
                    int px = x * CELL_SIZE;
                    int py = y * CELL_SIZE;

                    Point cell = new Point(x, y);
                    CellType type = zoneMap.getOrDefault(cell, CellType.EMPTY);

                    g.setColor(type.color);
                    g.fillRect(px, py, CELL_SIZE, CELL_SIZE);

                    g.setColor(Color.GRAY);
                    g.drawRect(px, py, CELL_SIZE, CELL_SIZE);
                }
            }

            // add zone labels etc: Z(1)
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));

            for (Map.Entry<Integer, Point> entry : zoneLabels.entrySet()) {
                int zoneID = entry.getKey();
                Point gridCell = entry.getValue();

                int x = gridCell.x * CELL_SIZE + 4;
                int y = gridCell.y * CELL_SIZE + 14;

                g.drawString("Z(" + zoneID + ")", x, y);
            }
        }
    }

    /**
     * Marks a zone on the grid.
     * @param zoneID
     * @param start
     * @param end
     */
    public void markZone(int zoneID, Point start, Point end) {
        int startX = Math.min(start.x, end.x);
        int endX = Math.max(start.x, end.x);
        int startY = Math.min(start.y, end.y);
        int endY = Math.max(start.y, end.y);

        zoneLabels.put(zoneID, new Point(startX, startY));

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                zoneMap.put(new Point(x, y), CellType.ZONE);
            }
        }

        repaint();
    }

    // represents types of zones
    enum CellType {
        EMPTY(Color.WHITE),
        ZONE(Color.LIGHT_GRAY),
        ACTIVE_FIRE(Color.RED),
        EXTINGUISHED_FIRE(Color.GREEN);

        final Color color;
        CellType(Color c) { this.color = c; }
    }

    // represents drone visual state
    enum DroneState {
        IDLE(Color.BLUE),
        OUTBOUND(Color.ORANGE),
        RETURNING(Color.MAGENTA),
        EXTINGUISHING(Color.CYAN),
        FAULTED(Color.BLACK);

        final Color color;
        DroneState(Color c) { this.color = c; }
    }

    // method to move drones (call this to update positions)
    public void updateDronePosition(int droneId, Point newPos, DroneState newState) {
        dronePositions.put(droneId, newPos);
        droneStates.put(droneId, newState);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(DroneSwarmDashboard::new);
    }
}
