package main.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Custom JPanel to draw the legend.
 */
public class LegendPanel extends JPanel {
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
