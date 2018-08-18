package me.blankboy.remotecommunicationutils;

// Types of priority for sending queue.
public enum TypesOPriority {

    // This means slow processing, based on its location in OutgoingQueue.
    Normal(-1),

    // This means it will be the next package in queue processor!
    High(0),

    // This means fast delivery, immediately after current buffer is processed!
    VeryHigh(1);

    private final int id;
    TypesOPriority(int id) { this.id = id; }
    public int getValue() { return id; }
}
