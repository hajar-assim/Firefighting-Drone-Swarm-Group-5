package subsystems.drone.events;

import subsystems.Event;
import subsystems.drone.DroneInfo;

import java.net.InetAddress;
import java.time.LocalDateTime;

public class DroneRegistrationEvent extends Event {
    private DroneInfo droneInfo;
    private final InetAddress address;
    private final int port;

    public DroneRegistrationEvent(DroneInfo droneInfo, InetAddress address, int port) {
        super(LocalDateTime.now().toString());
        this.droneInfo = droneInfo;
        this.address = address;
        this.port = port;
    }
    public DroneInfo getDroneInfo() { return droneInfo; }
    public InetAddress getAddress() { return address; }
    public int getPort() { return port; }

    @Override
    public String toString() {
        return String.format("DroneRegistrationEvent[drone Info=%s, address=%s, port=%s]",
                droneInfo,
                address,
                port);
    }

    @Override
    public void fromString(String s) {
        // TODO
    }
}

