package me.blankboy.remotecommunicationutils;

// Just a list of use full stream processor codes.
enum StreamCodes {
    // Code for confirmation.
    Confirm((byte)-1),

    // Code for append with confirmation request.
    Append((byte)1),

    // Code for append without confirmation.
    QuickAppend((byte)2);

    private final byte id;
    StreamCodes(byte id) { this.id = id; }
    public byte getValue() { return id; }
}
