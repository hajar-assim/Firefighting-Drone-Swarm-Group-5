package main.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BaseStationPanel extends JPanel {
    private final List<Integer> dronesAtBase = new ArrayList<>();
    private final JButton launchButton;

    public BaseStationPanel() {
        setLayout(new BorderLayout());

        // Drawing panel in center
        JPanel drawPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                g.drawString("BASE DRONES", 10, 20);

                int y = 40;
                for (int id : dronesAtBase) {
                    g.drawString("D(" + id + ")", 20, y + 15);
                    y += 30;
                }
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(150, 260);
            }
        };

        add(drawPanel, BorderLayout.CENTER);

        // Add drone button at bottom
        launchButton = new JButton("➕ Add Drone");
        launchButton.setFocusPainted(false);
        launchButton.setForeground(Color.BLACK);
        launchButton.setBackground(new Color(200, 200, 200)); // light gray
        launchButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        launchButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1, true),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        launchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        launchButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        launchButton.addActionListener(e -> onLaunchButtonClicked());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(launchButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void setDronesAtBase(List<Integer> droneIDs) {
        this.dronesAtBase.clear();
        this.dronesAtBase.addAll(droneIDs);
        repaint();
    }

    private void onLaunchButtonClicked() {
        try {
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + "/bin/java";
            String classpath = System.getProperty("java.class.path");
            String className = "subsystems.drone.DroneSubsystem";

            String command = String.format("\"%s\" -cp \"%s\" %s", javaBin, classpath, className);

            ProcessBuilder builder;

            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows: use cmd to launch a new terminal window
                builder = new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", command);
            } else if (os.contains("mac")) {
                // macOS: use AppleScript to launch Terminal
                builder = new ProcessBuilder("osascript", "-e",
                        "tell application \"Terminal\" to do script \"" + command.replace("\"", "\\\"") + "\"");
            } else {
                // Fallback or Linux: try xterm
                builder = new ProcessBuilder("xterm", "-hold", "-e", command);
            }

            builder.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
