package main.ui;

import java.awt.*;

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