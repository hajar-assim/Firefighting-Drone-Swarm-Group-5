public class Box {
    // Boolean to hold box state
    private boolean boxEmpty = true;
    // Message to be passed to the box
    private EventMessage message;

    /**
     * Place message in the box for it to be read
     * @param message
     * @throws InterruptedException
     */
    public synchronized void put(EventMessage message) throws InterruptedException {
        // Wait while the box is empty
        while (!boxEmpty) {
            wait();
        }
        // Put message in the box
        this.message = message;

        // Box is not empty anymore so flip bool
        boxEmpty = false;

        // Notify other threads that the box has a message
        notifyAll();
    }

    /**
     * Method to get a message from the box
     * @return The message placed in the box
     * @throws InterruptedException
     */
    public synchronized EventMessage get() throws InterruptedException {
        // Wait while box is empty
        while (boxEmpty) {
            wait();
        }

        // Variable to retrieve and return message from the box
        EventMessage tmpMsg = message;

        // Set message as null again since this box no longer has a message
        message = null;

        // Flip boolean to true since box is now empty
        boxEmpty = true;

        // Notify waiting threads that box is now empty
        notifyAll();

        return tmpMsg;
    }
}
