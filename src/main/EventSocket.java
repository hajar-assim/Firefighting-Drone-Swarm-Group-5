package main;

import subsystems.*;
import java.net.*;
import java.io.*;

/**
 * A class responsible for managing the communication of events using UDP sockets.
 * It allows sending and receiving serialized event objects over a DatagramSocket.
 */

public class

EventSocket {
    private DatagramSocket socket;


    /**
     * Constructs an EventSocket with a new DatagramSocket.
     *
     * @throws RuntimeException if there is an error creating the DatagramSocket.
     */
    public EventSocket(){
        try{
            socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructs an EventSocket bound to a specific port.
     *
     * @param port The port to bind the DatagramSocket to.
     * @throws RuntimeException if there is an error creating the DatagramSocket.
     */
    public EventSocket(int port){
        try{
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends an event over the DatagramSocket to a specified address and port.
     *
     * @param event The event to be sent.
     * @param address The address to send the event to.
     * @param port The port to send the event to.
     */
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

    /**
     * Receives an event over the DatagramSocket.
     *
     * @return The received event, or null if there was an error during reception.
     */
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

    /**
     * Retrieves the DatagramSocket associated with this EventSocket.
     *
     * @return The DatagramSocket used for communication.
     */
    public DatagramSocket getSocket() {
        return socket;
    }

    /**
     * Closes the DatagramSocket if it is open.
     */
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
