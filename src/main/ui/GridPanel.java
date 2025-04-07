package main.ui;

import subsystems.fire_incident.Severity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Map;

import static main.ui.DroneSwarmDashboard.*;

/**
 * Custom JPanel to draw the grid and zones.
 */
public class GridPanel extends JPanel {
    private Point hoveredCell = null;
    public static final int CELL_SIZE = 40; // this is the # divided by the coordinates in the original csv
    public static final int GRID_WIDTH = 40;
    public static final int GRID_HEIGHT = 40;
    public static final int PADDING = 10; // pixels of space around the grid
    public static final Image FIRE_IMAGE = new ImageIcon("src/main/ui/emojis/ACTIVE_FIRE.png").getImage()
            .getScaledInstance(CELL_SIZE, CELL_SIZE, Image.SCALE_SMOOTH);

    public static final Image EXTINGUISHED_IMAGE = new ImageIcon("src/main/ui/emojis/EXTINGUISHED_FIRE.png").getImage()
            .getScaledInstance(CELL_SIZE, CELL_SIZE, Image.SCALE_SMOOTH);

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

        if (FIRE_IMAGE == null || EXTINGUISHED_IMAGE == null){
            System.out.println("NO IMAGE");
            System.exit(1);
        }
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
            Color fillColor = FireSeverityColor.fromSeverity(s);

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

            DroneSwarmDashboard.FireStatus status = zoneFireStatus.getOrDefault(zoneID, DroneSwarmDashboard.FireStatus.NONE);
            if (status == DroneSwarmDashboard.FireStatus.NONE) continue;

            int centerX = r.x + r.width / 2;
            int centerY = r.y + r.height / 2;

            int px = centerX * CELL_SIZE + PADDING;
            int py = centerY * CELL_SIZE + PADDING;

            // Choose image based on fire status
            Image imageToDraw = (status == DroneSwarmDashboard.FireStatus.ACTIVE) ? FIRE_IMAGE : EXTINGUISHED_IMAGE;

            // Draw image
            g.drawImage(imageToDraw, px, py, CELL_SIZE, CELL_SIZE, null);
        }

        // draw drones
        for (Map.Entry<Integer, DroneRender> entry : droneStates.entrySet()) {
            int droneID = entry.getKey();
            DroneRender drone = entry.getValue();

            if (drone.state == DroneStateEnum.FAULTED && !showFaultedDrones) {
                continue; // skip drawing faulted drone
            }

            int px = drone.gridPos.x * CELL_SIZE + PADDING;
            int py = drone.gridPos.y * CELL_SIZE + PADDING;

            g.drawImage(drone.state.image, px, py, CELL_SIZE -15, CELL_SIZE -15, null);

            // don't want to fully cover the cell with drone color, should show up as a dot
            int size = CELL_SIZE / 5;
            int offset = (CELL_SIZE - size) / 2;

            // draw drone ID label etc: D(1)

            //  exclude Z(0) since we already have a panel for base drones
            if (entry.getValue().gridPos.x == 0 && entry.getValue().gridPos.y == 0) {
                continue;
            }

            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.ITALIC, 10));

            // draw the label underneath the drone
            int labelX = px + offset - 13; // slight nudge to center it
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