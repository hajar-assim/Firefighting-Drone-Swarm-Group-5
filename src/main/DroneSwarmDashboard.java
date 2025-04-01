package main;

import subsystems.fire_incident.Severity;
import subsystems.fire_incident.events.IncidentEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DroneSwarmDashboard extends JFrame {

    public static final int CELL_SIZE = 40; // this is the # divided by the coordinates in the original csv
    private static final int GRID_WIDTH = 25;
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
    private final Map<Integer, Integer> zoneRemainingWater = new HashMap<>();
    private final Map<Integer, Severity> zoneSeverities = new HashMap<>();


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

    // fire Severity → Color
    public enum FireSeverityColor {
        LOW(new Color(255, 255, 0, 80)),       // translucent yellow
        MODERATE(new Color(255, 165, 0, 80)),  // translucent orange
        HIGH(new Color(255, 0, 0, 80)),        // translucent red
        NONE(new Color(200, 200, 200, 60));    // gray

        public final Color color;

        FireSeverityColor(Color color) {
            this.color = color;
        }

        public static Color fromSeverity(Severity s) {
            return switch (s) {
                case LOW -> LOW.color;
                case MODERATE -> MODERATE.color;
                case HIGH -> HIGH.color;
                case NONE -> NONE.color;
                case null -> NONE.color;
            };
        }
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
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setResizable(true);

        JCheckBox showFaultedToggle = new JCheckBox("Show Faulted Drones");
        showFaultedToggle.setSelected(showFaultedDrones);
        showFaultedToggle.addActionListener(e -> {
            showFaultedDrones = showFaultedToggle.isSelected();
            repaint();
        });

        setLayout(new BorderLayout());

        LegendPanel legendPanel = new LegendPanel();
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));

        // divider
        JSeparator divider = new JSeparator(SwingConstants.HORIZONTAL);
        divider.setMaximumSize(new Dimension(180, 8));
        legendPanel.add(Box.createVerticalStrut(12));
        legendPanel.add(divider);
        legendPanel.add(Box.createVerticalStrut(12));

        // "Legend" title
        JLabel legendTitle = new JLabel("Legend");
        legendTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        legendTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        legendPanel.add(legendTitle);
        legendPanel.add(Box.createVerticalStrut(8));

        // === ZONE LEGEND ===
        JLabel zoneHeader = new JLabel("Zones");
        zoneHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        zoneHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        legendPanel.add(zoneHeader);
        legendPanel.add(Box.createVerticalStrut(4));

        legendPanel.add(createLegendItem(FireSeverityColor.LOW.color, "Z(n) Zone Label -> Low Severity"));
        legendPanel.add(createLegendItem(FireSeverityColor.MODERATE.color, "Z(n) Zone Label -> Moderate Severity"));
        legendPanel.add(createLegendItem(FireSeverityColor.HIGH.color, "Z(n) Zone Label -> High Severity"));
        legendPanel.add(createLegendItem(Color.RED, "Active fire"));
        legendPanel.add(createLegendItem(Color.GREEN, "Extinguished fire"));

        // === DRONE LEGEND ===
        JLabel droneHeader = new JLabel("Drones");
        droneHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        droneHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        legendPanel.add(droneHeader);
        legendPanel.add(Box.createVerticalStrut(4));

        legendPanel.add(createLegendItem(Color.BLUE, "D(n) Drone IDLE"));
        legendPanel.add(createLegendItem(Color.ORANGE, "D(n) Drone ON_ROUTE"));
        legendPanel.add(createLegendItem(Color.CYAN, "D(n) Drone DROPPING_AGENT"));
        legendPanel.add(createLegendItem(Color.BLACK, "D(n) Drone FAULTED"));


        // === BASE LEGEND ===
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
        private Point hoveredCell = null;

        public GridPanel() {
            setPreferredSize(new Dimension(GRID_WIDTH * CELL_SIZE + 2 * PADDING, GRID_HEIGHT * CELL_SIZE + 2 * PADDING));
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int x = (e.getX() - PADDING) / CELL_SIZE;
                    int y = (e.getY() - PADDING) / CELL_SIZE;
                    hoveredCell = new Point(x, y);
                    repaint();
                }
            });
        }

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

            for (Map.Entry<Integer, Rectangle> entry : zoneBounds.entrySet()) {
                int zoneID = entry.getKey();
                Rectangle r = entry.getValue();

                // Get severity (default to NONE)
                Severity s = zoneSeverities.getOrDefault(zoneID, Severity.NONE);

                // Set fill color based on severity
                Color fillColor = switch (s) {
                    case LOW      -> new Color(255, 255, 0, 80);   // translucent yellow
                    case MODERATE -> new Color(255, 165, 0, 80);   // translucent orange
                    case HIGH     -> new Color(255, 0, 0, 80);     // translucent red
                    case NONE     -> new Color(230, 230, 230, 60); // neutral gray
                };

                int px = r.x * CELL_SIZE + PADDING;
                int py = r.y * CELL_SIZE + PADDING;
                int width = r.width * CELL_SIZE;
                int height = r.height * CELL_SIZE;

                // fill and border
                g.setColor(fillColor);
                g.fillRect(px, py, width, height);

                g.setColor(Color.BLACK);
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

            if (hoveredCell != null) {
                for (Map.Entry<Integer, Rectangle> entry : zoneBounds.entrySet()) {
                    int zoneID = entry.getKey();
                    Rectangle zoneRect = entry.getValue();

                    if (zoneRect.contains(hoveredCell)) {
                        Integer remaining = zoneRemainingWater.get(zoneID);
                        if (remaining != null && remaining > 0) {
                            String text = "Water left: " + remaining + "L";

                            int px = hoveredCell.x * CELL_SIZE + PADDING;
                            int py = hoveredCell.y * CELL_SIZE + PADDING;

                            g.setColor(Color.BLACK);
                            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                            g.drawString(text, px + 5, py - 5);
                        }
                        break;
                    }
                }
            }

        }
    }

    /**
     * Creates a legend item for the dashboard.
     * @param color
     * @param label
     * @return
     */
    private JPanel createLegendItem(Color color, String label) {
        JPanel item = new JPanel();
        item.setLayout(new BoxLayout(item, BoxLayout.X_AXIS));
        item.setAlignmentX(Component.LEFT_ALIGNMENT); // key!
        item.setBackground(new Color(240, 240, 240));
        item.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4)); // padding
        item.setMaximumSize(new Dimension(280, 24)); // consistent width

        JLabel colorBox = new JLabel();
        colorBox.setOpaque(true);
        colorBox.setBackground(color);
        colorBox.setPreferredSize(new Dimension(15, 15));
        colorBox.setMinimumSize(new Dimension(15, 15));
        colorBox.setMaximumSize(new Dimension(15, 15));
        colorBox.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        JLabel text = new JLabel(label);
        text.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0)); // spacing after box

        item.add(colorBox);
        item.add(text);
        return item;
    }



    /**
     * Updates the water remaining in a zone.
     * @param zoneID
     * @param remainingLiters
     */
    public void updateZoneWater(int zoneID, int remainingLiters) {
        if (remainingLiters <= 0) {
            zoneRemainingWater.remove(zoneID); // fire's out
        } else {
            zoneRemainingWater.put(zoneID, remainingLiters);
        }
        repaint();
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
     * Updates the severity of a zone.
     * @param zoneID
     * @param severity
     */
    public void updateZoneSeverity(int zoneID, Severity severity) {
        if (severity == Severity.NONE) {
            zoneSeverities.remove(zoneID); // clear if fire is extinguished
        } else {
            zoneSeverities.put(zoneID, severity);
        }
        repaint();
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
            setPreferredSize(new Dimension(280, 300)); // width, height
            setBackground(Color.WHITE);
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
