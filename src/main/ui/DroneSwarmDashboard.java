package main.ui;

import subsystems.fire_incident.Severity;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static main.ui.GridPanel.*;

public class DroneSwarmDashboard extends JFrame {

    private final BaseStationPanel basePanel = new BaseStationPanel();

    // drones
    public static final Map<Integer, DroneRender> droneStates = new HashMap<>();
    public static boolean showFaultedDrones = true;

    // zones
    public static final Map<Point, CellType> zoneMap = new HashMap<>();
    public static final Map<Integer, Point> zoneLabels = new HashMap<>();
    public static final Map<Integer, Rectangle> zoneBounds = new HashMap<>();
    public static final Map<Integer, FireStatus> zoneFireStatus = new HashMap<>();
    public static final Map<Integer, Integer> zoneRemainingWater = new HashMap<>();
    public static final Map<Integer, Severity> zoneSeverities = new HashMap<>();


    // represents fire status
    public enum FireStatus {
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
        JLabel legendTitle = new JLabel("LEGEND");
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

        legendPanel.add(createLegendItem(FireSeverityColor.LOW.color,null, "Z(n) Zone Label -> Low Severity"));
        legendPanel.add(createLegendItem(FireSeverityColor.MODERATE.color, null, "Z(n) Zone Label -> Moderate Severity"));
        legendPanel.add(createLegendItem(FireSeverityColor.HIGH.color, null, "Z(n) Zone Label -> High Severity"));
        legendPanel.add(createLegendItem(null, FIRE_IMAGE, "Active fire"));
        legendPanel.add(createLegendItem(null, EXTINGUISHED_IMAGE, "Extinguished fire"));

        // === DRONE LEGEND ===
        JLabel droneHeader = new JLabel("Drones");
        droneHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        droneHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        legendPanel.add(droneHeader);
        legendPanel.add(Box.createVerticalStrut(4));

        legendPanel.add(createLegendItem(null, DroneStateEnum.IDLE.getImage(), "D(n) Drone IDLE"));
        legendPanel.add(createLegendItem(null, DroneStateEnum.OUTBOUND.getImage(), "D(n) Drone ON_ROUTE"));
        legendPanel.add(createLegendItem(null, DroneStateEnum.EXTINGUISHING.getImage(), "D(n) Drone DROPPING_AGENT"));
        legendPanel.add(createLegendItem(null, DroneStateEnum.FAULTED.getImage(), "D(n) Drone FAULTED"));


        // === BASE LEGEND ===
        GridPanel gridPanel = new GridPanel();
        JScrollPane scrollPane = new JScrollPane(
                gridPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(showFaultedToggle, BorderLayout.NORTH);
        rightPanel.add(legendPanel, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(basePanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
        add(scrollPane, BorderLayout.CENTER);

        pack();
        setVisible(true);
    }


    /**
     * Creates a legend item for the dashboard.
     * @param color
     * @param image
     * @param label
     * @return
     */
    private JPanel createLegendItem(Color color, Image image, String label) {
        JPanel item = new JPanel();
        item.setLayout(new BoxLayout(item, BoxLayout.X_AXIS));
        item.setAlignmentX(Component.LEFT_ALIGNMENT); // key!
        item.setBackground(new Color(240, 240, 240));
        item.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4)); // padding
        item.setMaximumSize(new Dimension(280, 24)); // consistent width

        JLabel colorBox = new JLabel();
        colorBox.setOpaque(true);
        if (color != null){
            colorBox.setBackground(color);
        }else{
            Image scaled = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            colorBox.setIcon(new ImageIcon(scaled));
        }
        colorBox.setPreferredSize(new Dimension(20, 20));
        colorBox.setMinimumSize(new Dimension(20, 20));
        colorBox.setMaximumSize(new Dimension(20, 20));

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
    public void updateDronePosition(int droneID, Point gridPos, DroneStateEnum state) {
        droneStates.put(droneID, new DroneRender(gridPos, state));

        // update base station panel with list of drones at base (0,0) and IDLE
        List<Integer> atBase = droneStates.entrySet().stream()
                .filter(entry -> entry.getValue().state == DroneStateEnum.IDLE)
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

}
