package main.ui;

import javax.swing.*;
import java.awt.*;

public enum DroneStateEnum {

    IDLE("BASE_DRONE.png"),
    OUTBOUND("OUTBOUND_DRONE.png"),
    RETURNING("INBOUND_DRONE.png"),
    EXTINGUISHING("EXTINGUISHING_DRONE.png"),
    FAULTED("FAULTED_DRONE.png");

    final Image image;

    DroneStateEnum(String imageFileName) {
        this.image = new ImageIcon("src/main/ui/emojis/" + imageFileName).getImage()
                .getScaledInstance(GridPanel.CELL_SIZE, GridPanel.CELL_SIZE, Image.SCALE_SMOOTH);
    }

    public Image getImage() {
        return image;
    }

    public static DroneStateEnum fromDroneStateObject(subsystems.drone.states.DroneState state) {
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
