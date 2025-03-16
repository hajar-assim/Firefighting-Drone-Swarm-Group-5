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

    public EventSocket(int port){
        try{
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(Event event, InetAddress address, int port) {
        // Serialize event
        try{
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(event);
            byte msg[] = byteArrayOutputStream.toByteArray();

            DatagramPacket packet = new DatagramPacket(msg, msg.length, address, port);

            socket.send(packet);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public Event receive() {
        byte data[] = new byte[1000];
        DatagramPacket packet = new DatagramPacket(data, data.length);

        Event event = null;

        try{
            socket.receive(packet);

            // Deserialize object
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            event = (Event) objectInputStream.readObject();
        } catch (Exception e){
            e.printStackTrace();
        }

        return event;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    // used in Unit Tests for FireIncidentSubsystemTest cleanup
    public void close() {
        socket.close();
    }
}
