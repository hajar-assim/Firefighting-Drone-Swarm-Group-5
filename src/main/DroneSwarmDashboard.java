package main;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DroneSwarmDashboard extends JFrame {

    public static final int CELL_SIZE = 40;
    private static final int GRID_WIDTH = 20;
    private static final int GRID_HEIGHT = 15;

    // drones
    private final Map<Integer, DroneRender> droneStates = new HashMap<>();


    // zones
    private final Map<Point, CellType> zoneMap = new HashMap<>();
    private final Map<Integer, Point> zoneLabels = new HashMap<>();
    private final Map<Integer, Rectangle> zoneBounds = new HashMap<>();
    private final Map<Integer, FireStatus> zoneFireStatus = new HashMap<>();

    /**
     * ENUMS
     */

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

    // represents fire status
    enum FireStatus {
        NONE, ACTIVE, EXTINGUISHED
    }



    /**
     * Creates a new instance of the DroneSwarmDashboard.
     */
    public DroneSwarmDashboard() {
        setTitle("Firefighting Drone Swarm Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GridPanel panel = new GridPanel();
        add(panel, BorderLayout.CENTER);

        pack();
        setVisible(true);
    }

    /**
     * Custom JPanel to draw the grid and zones.
     */
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

            // grey out the zones
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(2)); // 2px wide border
            g2d.setColor(Color.DARK_GRAY);

            for (Rectangle r : zoneBounds.values()) {
                int px = r.x * CELL_SIZE;
                int py = r.y * CELL_SIZE;
                int width = r.width * CELL_SIZE;
                int height = r.height * CELL_SIZE;

                g.drawRect(px, py, width, height);
            }

            // add zone labels etc: Z(1)
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(Color.BLACK);

            for (Map.Entry<Integer, Point> entry : zoneLabels.entrySet()) {
                int zoneID = entry.getKey();
                Point gridPos = entry.getValue(); // top-left cell of zone

                int px = gridPos.x * CELL_SIZE + 4;
                int py = gridPos.y * CELL_SIZE + 14;

                g.drawString("Z(" + zoneID + ")", px, py);
            }


            // draw fire indicator
            for (Map.Entry<Integer, Rectangle> entry : zoneBounds.entrySet()) {
                int zoneID = entry.getKey();
                Rectangle r = entry.getValue();

                FireStatus status = zoneFireStatus.getOrDefault(zoneID, FireStatus.NONE);
                if (status == FireStatus.NONE) continue;

                int centerX = r.x + r.width / 2;
                int centerY = r.y + r.height / 2;

                int px = centerX * CELL_SIZE;
                int py = centerY * CELL_SIZE;

                g.setColor(status == FireStatus.ACTIVE ? Color.RED : Color.GREEN);
                g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
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
        zoneBounds.put(zoneID, new Rectangle(startX, startY, endX - startX + 1, endY - startY + 1));

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                zoneMap.put(new Point(x, y), CellType.ZONE);
            }
        }

        repaint();
    }

    /**
     * Draws a drone on the grid.
     * @param droneId
     * @param newPos
     * @param newState
     */
    public void updateDronePosition(int droneId, Point newPos, DroneState newState) {
        dronePositions.put(droneId, newPos);
        droneStates.put(droneId, newState);
        repaint();
    }

    /**
     * Returns the preferred size of the dashboard.
     * @return
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);
    }

    /**
     * Sets the fire status of a zone.
     * @param zoneID
     * @param status
     */
    public void setZoneFireStatus(int zoneID, FireStatus status) {
        zoneFireStatus.put(zoneID, status);
        repaint();
    }

    /**
     * Represents the state of a drone.
     */
    private static class DroneRender {
        Point gridPos;
        DroneState state;

        public DroneRender(Point pos, DroneState state) {
            this.gridPos = pos;
            this.state = state;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DroneSwarmDashboard::new);
    }

}
