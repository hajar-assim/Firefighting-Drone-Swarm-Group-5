package main;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DroneSwarmDashboard extends JFrame {

    public static final int CELL_SIZE = 40; // this is the # divided by the coordinates in the original csv
    private static final int GRID_WIDTH = 30;
    private static final int GRID_HEIGHT = 20;
    private static final int PADDING = 10; // pixels of space around the grid
    private final BaseStationPanel basePanel = new BaseStationPanel();

    // drones
    private final Map<Integer, DroneRender> droneStates = new HashMap<>();
    private boolean showFaultedDrones = true;

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

    // represents drone visual state for the GUI
    enum DroneState {
        IDLE(Color.BLUE),
        OUTBOUND(Color.ORANGE),
        RETURNING(Color.MAGENTA),
        EXTINGUISHING(Color.CYAN),
        FAULTED(Color.BLACK);

        final Color color;

        DroneState(Color c) {
            this.color = c;
        }

        public static DroneState fromDroneStateObject(subsystems.drone.states.DroneState state) {
            if (state == null) return null;

            return switch (state.getClass().getSimpleName()) {
                case "IdleState" -> IDLE;
                case "OnRouteState" -> OUTBOUND;
                case "DroppingAgentState" -> EXTINGUISHING;
                case "FaultedState" -> FAULTED;
                default -> null;
            };
        }
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

        JCheckBox showFaultedToggle = new JCheckBox("Show Faulted Drones");
        showFaultedToggle.setSelected(showFaultedDrones);
        showFaultedToggle.addActionListener(e -> {
            showFaultedDrones = showFaultedToggle.isSelected();
            repaint();
        });

        setLayout(new BorderLayout());

        LegendPanel legendPanel = new LegendPanel();
        GridPanel gridPanel = new GridPanel();
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(showFaultedToggle, BorderLayout.NORTH);
        rightPanel.add(legendPanel, BorderLayout.CENTER);


        setLayout(new BorderLayout());
        add(basePanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
        add(gridPanel, BorderLayout.CENTER);

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
                    int px = x * CELL_SIZE + PADDING;
                    int py = y * CELL_SIZE + PADDING;

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
            g2d.setColor(Color.BLACK); // black border

            for (Rectangle r : zoneBounds.values()) {
                int px = r.x * CELL_SIZE + PADDING;
                int py = r.y * CELL_SIZE + PADDING;
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

                int px = gridPos.x * CELL_SIZE + PADDING + 4;
                int py = gridPos.y * CELL_SIZE + PADDING + 14;

                g.drawString("Z(" + zoneID + ")", px, py);
            }


            // draw fire indicator
            for (Map.Entry<Integer, Rectangle> entry : zoneBounds.entrySet()) {
                int zoneID = entry.getKey();
                if (zoneID == 0) continue; // skip zone 0, our base
                Rectangle r = entry.getValue();

                FireStatus status = zoneFireStatus.getOrDefault(zoneID, FireStatus.NONE);
                if (status == FireStatus.NONE) continue;

                int centerX = r.x + r.width / 2;
                int centerY = r.y + r.height / 2;

                int px = centerX * CELL_SIZE + PADDING;
                int py = centerY * CELL_SIZE + PADDING;

                g.setColor(status == FireStatus.ACTIVE ? Color.RED : Color.GREEN);
                g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
            }

            // draw drones
            for (Map.Entry<Integer, DroneRender> entry : droneStates.entrySet()) {
                int droneID = entry.getKey();
                DroneRender drone = entry.getValue();

                if (drone.state == DroneState.FAULTED && !showFaultedDrones) {
                    continue; // skip drawing faulted drone
                }

                int px = drone.gridPos.x * CELL_SIZE + PADDING;
                int py = drone.gridPos.y * CELL_SIZE + PADDING;

                g.setColor(drone.state.color);

                // don't want to fully cover the cell with drone color, should show up as a dot
                int size = CELL_SIZE / 5;
                int offset = (CELL_SIZE - size) / 2;

                g.fillRect(px + offset, py + offset, size, size);

                // draw drone ID label etc: D(1)

                //  exclude Z(0) since we already have a panel for base drones
                if (entry.getValue().gridPos.x == 0 && entry.getValue().gridPos.y == 0) {
                    continue;
                }

                g.setColor(Color.BLACK);
                g.setFont(new Font("SansSerif", Font.ITALIC, 10));

                // draw the label underneath the drone
                int labelX = px + offset - 4; // slight nudge to center it
                int labelY = py + offset + size + 12; // below the square
                g.drawString("D(" + droneID + ")", labelX, labelY);

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
     */
    public void updateDronePosition(int droneID, Point gridPos, DroneState state) {
        droneStates.put(droneID, new DroneRender(gridPos, state));

        // update base station panel with list of drones at base (0,0) and IDLE
        List<Integer> atBase = droneStates.entrySet().stream()
                .filter(entry -> entry.getValue().state == DroneState.IDLE)
                .map(Map.Entry::getKey)
                .toList();

        basePanel.setDronesAtBase(atBase);

        repaint();
    }

    /**
     * Returns the preferred size of the dashboard.
     * @return
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(GRID_WIDTH * CELL_SIZE + PADDING * 2,
                GRID_HEIGHT * CELL_SIZE + PADDING * 2);
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

    /**
     * Main method to run the dashboard.
     * @param args
     */

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DroneSwarmDashboard::new);
    }


    /**
     * Custom JPanel to draw the legend.
     */

    private static class LegendPanel extends JPanel {
        public LegendPanel() {
            setPreferredSize(new Dimension(230, 300)); // width, height
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int y = 20;

            g.setFont(new Font("SansSerif", Font.BOLD, 14));

            // title
            g.setColor(Color.BLACK);
            g.drawString("Legend", 10, y);
            y += 30;

            drawLegendItem(g, CellType.ZONE.color, "Z(n) Zone Label", y);
            y += 25;

            drawLegendItem(g, CellType.ACTIVE_FIRE.color, "Active fire", y);
            y += 25;

            drawLegendItem(g, CellType.EXTINGUISHED_FIRE.color, "Extinguished fire", y);
            y += 25;

            drawLegendItem(g, DroneState.IDLE.color, "D(n) Drone IDLE", y);
            y += 25;

            drawLegendItem(g, DroneState.OUTBOUND.color, "D(n) Drone ON_ROUTE", y);
            y += 25;

            drawLegendItem(g, DroneState.EXTINGUISHING.color, "D(n) Drone DROPPING_AGENT", y);
            y += 25;

            drawLegendItem(g, DroneState.FAULTED.color, "D(n) Drone FAULTED", y);
        }

        private void drawLegendItem(Graphics g, Color color, String label, int y) {
            g.setColor(color);
            g.fillRect(10, y, 20, 20);

            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString(label, 40, y + 15);
        }
    }

    private static class BaseStationPanel extends JPanel {
        private List<Integer> dronesAtBase = new ArrayList<>();

        public void setDronesAtBase(List<Integer> droneIDs) {
            this.dronesAtBase = droneIDs;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(150, 300);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(Color.BLACK);
            g.drawString("Base Drones", 10, 20);

            int y = 50;
            for (int id : dronesAtBase) {
                g.setColor(DroneState.IDLE.color);
                g.fillRect(10, y, 20, 20);

                g.setColor(Color.BLACK);
                g.drawString("D(" + id + ")", 40, y + 15);
                y += 30;
            }
        }
    }

}
