package me.blankboy.remotecommunicationutils;

// Data types for AverageSpeed calculator.
public enum Data {
    Bytes(0),
    KiloBytes(1),
    MegaBytes(2),
    GigaBytes(3);

    private final int id;
    Data(int id) { this.id = id; }
    public int getValue() { return id; }
}
