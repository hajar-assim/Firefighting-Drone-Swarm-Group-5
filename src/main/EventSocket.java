package main;

import subsystems.*;
import java.net.*;
import java.io.*;

public class EventSocket {
    private DatagramSocket socket;

    public EventSocket(){
        try{
            socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(Event incident, InetAddress address, int port) throws IOException{
        // Serialize incident
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(incident);
        byte msg[] = byteArrayOutputStream.toByteArray();

        DatagramPacket packet = new DatagramPacket(msg, msg.length, address, port);

        socket.send(packet);
    }

    public Event receive() throws IOException, ClassNotFoundException {
        byte data[] = new byte[100];
        DatagramPacket packet = new DatagramPacket(data, data.length);

        socket.receive(packet);

        // Deserialize object
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        return (Event) objectInputStream.readObject();
    }
}
