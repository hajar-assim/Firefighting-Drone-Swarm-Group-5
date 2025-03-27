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
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(event);
            objectOutputStream.flush(); // Ensure data is flushed
            byte[] msg = byteArrayOutputStream.toByteArray();

            DatagramPacket packet = new DatagramPacket(msg, msg.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Receives an event over the DatagramSocket.
     *
     * @return The received event, or null if there was an error during reception.
     */
    public Event receive() {
        byte[] data = new byte[4096];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        try {
            socket.receive(packet);
            int length = packet.getLength();
            if (length == 0) {
                System.err.println("[EventSocket] Received an empty packet.");
                return null;
            }
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data, 0, length);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Event event = (Event) objectInputStream.readObject();
            return event;
        } catch (EOFException e) {
            System.err.println("[EventSocket] EOFException during receive: " + e.getMessage());
            return null;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
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
