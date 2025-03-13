package main;

import subsystems.*;
import java.net.*;
import java.io.*;

public class EventSocket {
    private DatagramSocket receiveSocket;
    private DatagramSocket sendSocket;

    public EventSocket(){
        try{
            receiveSocket = new DatagramSocket();
            sendSocket = new DatagramSocket();
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

        sendSocket.send(packet);
    }

    public Event receive() throws IOException, ClassNotFoundException {
        byte data[] = new byte[100];
        DatagramPacket packet = new DatagramPacket(data, data.length);

        receiveSocket.receive(packet);

        // Deserialize object
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        return (Event) objectInputStream.readObject();
    }
}
