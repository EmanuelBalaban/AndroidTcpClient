package me.blankboy.remotecommunicationutils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Chunk {
    // This is every chunk code but its not implemented to use this one. So every chunk will be sent with DataPackage's code.
    public byte Code = 0;

    // This is just in case something goes wrong with user transmission.
    byte StreamCode = StreamCodes.Append.getValue();

    private DataPackage Owner;

    // Create new Chunk. This method is only for AdvancedChannel.
    public Chunk(DataPackage Owner, byte Code, byte StreamCode, long Position, int Length, long Sent, long Received) {
        this.Owner = Owner;
        this.Position = Position;
        this.Length = Length;
        this.StreamCode = StreamCode;
        this.Code = Code;
        this.Sent = Sent;
        this.Received = Received;
    }

    // Position in stream.
    public long Position = 0;

    // Length of chunk.
    public int Length = 0;

    // Sent time in ticks.
    public long Sent = -1;

    // Received time in ticks.
    public long Received = -1;

    // Measured in milliseconds
    public int Latency() {
        if (Received == -1 || Sent == -1) return -1;
        return (int) ((Received - Sent) / 10000);
    }

    // Read chunk data directly from Stream.
    public byte[] readAllBytes() throws IOException {
        byte[] data = new byte[Length];
        InputStream is = Owner.getInputStream();
        is.skip(Position);

        int read = is.read(data, 0, Length);
        if (read > 0) {
            if (read != Length) {
                byte[] result = new byte[read];
                System.arraycopy(data, 0, result, 0, read);
                return result;
            }
            return data;
        }
        return null;
    }

    // Get MD5 hash byte array of this chunk.
    public byte[] getHashData() {
        byte[] hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(readAllBytes());
            hash = md.digest();
        } catch (Exception ignored) {
        }
        return hash;
    }

    // Get readable MD5 hash of this data segment.
    public String getHash() {
        return Extensions.getReadableHexadecimal(getHashData());
    }

    // This is used to ensure there s no data loss. Still reading this?!
    public TypesOConfirmation Confirmation = TypesOConfirmation.NotConfirmed;

    // Get human readable version of this object.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{");
        sb.append("Position = " + Position);
        sb.append("Length = " + Length);
        sb.append("Hash = " + getHash());
        sb.append("\nStreamCode = " + StreamCode);
        sb.append("Code = " + Code);
        sb.append("\nSent = " + Sent);
        sb.append("Received = " + Received);
        sb.append("Latency = " + Latency() + "ms");
        sb.append("\nConfirmation = " + Confirmation.toString());
        sb.append("}");

        return sb.toString();
    }
}
