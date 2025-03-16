package subsystems.drone.events;

import subsystems.Event;

import java.net.InetAddress;
import java.time.LocalDateTime;

public class DroneRegistrationEvent extends Event {
    private final int droneID;
    private final InetAddress address;
    private final int port;

    public DroneRegistrationEvent(int droneID, InetAddress address, int port) {
        super(LocalDateTime.now().toString());
        this.droneID = droneID;
        this.address = address;
        this.port = port;
    }

    public int getDroneID() { return droneID; }
    public InetAddress getAddress() { return address; }
    public int getPort() { return port; }

    @Override
    public String toString() {
        return String.format("DroneRegistrationEvent[droneID=%s, address=%s, port=%s]",
                droneID,
                address,
                port);
    }

    @Override
    public void fromString(String s) {
        // TODO
    }
}

