package me.blankboy.remotecommunicationutils;

import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import java8.util.stream.*;

// Yet just another data representation!
public class DataPackage implements AutoCloseable {
    // Stream stuff!
    private OutputStream _OutputStream = new ByteArrayOutputStream();

    public OutputStream getOutputStream() {
        return _OutputStream;
    }

    public InputStream getInputStream() {
        try {
            return _OutputStream instanceof ByteArrayOutputStream ?
                    new ByteArrayInputStream(((ByteArrayOutputStream) _OutputStream).toByteArray()) :
                    new FileInputStream(((FileOutputStream) _OutputStream).getFD());
        } catch (Exception ignored) {

        }
        return null;
    }

    public long getLength() {
        try {
            return _OutputStream instanceof ByteArrayOutputStream ?
                    ((ByteArrayOutputStream) _OutputStream).toByteArray().length :
                    ((FileOutputStream) _OutputStream).getChannel().size();
        } catch (Exception ignored) {

        }
        return 0;
    }

    @Override
    public void close() throws Exception {
        if (_OutputStream != null) _OutputStream.close();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public Stream<Chunk> getChunksStream() {
        return StreamSupport.stream(Chunks);
    }

    // If confirmation system is enabled checks if all confirmed chunks make Size, otherwise checks if all chunks.Length make Size.
    public boolean isFinished() {
        return Size == (ConfirmationSystem ? getChunksStream().filter(x -> x.Confirmation == TypesOConfirmation.Confirmed).mapToLong(x -> (long) x.Length).sum() : getChunksStream().mapToLong(x -> x.Length).sum());
    }

    // Percentage done of Stream.
    public double Percentage() {
        double result = 0;
        try {
            double sumOLengths = getLength();//Chunks.Sum(x => x.Length);
            result = (sumOLengths * 100) / Size;
        } catch (Exception ignored) {

        }
        return result;
    }

    // Calculated in dataType/second.
    public double averageSpeed(Data dataType) {
        int multiplier = dataType.getValue();
        int divider = 1;
        while (multiplier > 0) {
            divider *= 1024;
            multiplier--;
        }

        double speed = 0;
        try {
            long averageTicks = (long) getChunksStream().mapToLong(x -> x.Received - x.Sent).average().getAsDouble();
            long averageLength = (long) getChunksStream().mapToLong(x -> x.Length).average().getAsDouble();

            int time = (int) (averageTicks / 10000);
            long length = averageLength / divider;

            speed = length / time;
        } catch (Exception ignored) {

        }
        return speed;
    }

    // Calculated in ticks.
    public double averageLatency() {
        return Chunks.size() > 0 ? getChunksStream().mapToLong(x -> x.Received - x.Sent).average().getAsDouble() : -1;
    }

    // First time a chunk was sent.
    public long firstSent() {
        return Chunks.size() > 0 ? getChunksStream().mapToLong(x -> x.Sent).min().getAsLong() : -1;
    }

    // Last time a chunk was sent.
    public long lastSent() {
        return Chunks.size() > 0 ? getChunksStream().mapToLong(x -> x.Sent).max().getAsLong() : -1;
    }

    // First time a chunk was received.
    public long firstReceived() {
        return Chunks.size() > 0 ? getChunksStream().mapToLong(x -> x.Received).min().getAsLong() : -1;
    }

    // Last time a chunk was received.
    public long lastReceived() {
        return Chunks.size() > 0 ? getChunksStream().mapToLong(x -> x.Received).max().getAsLong() : -1;
    }

    byte[] getReadyData(byte[] data, byte StreamCode, byte Code) {
        if (data == null) throw new IllegalArgumentException("Passed data was null!");

        byte[] total = new byte[
                Extensions.LongSize + // Size
                        Extensions.IntSize +  // UniqueID
                        Extensions.ByteSize + // StreamCode
                        Extensions.ByteSize + // UserCode
                        Extensions.LongSize + // SentTime
                        data.length           // Data Length
                ];

        int offset = 0;
        byte[] buffer;

        // Size
        buffer = Extensions.getBytes((long) data.length);
        System.arraycopy(buffer, 0, total, offset, buffer.length);
        offset += buffer.length;

        // UniqueID
        buffer = Extensions.getBytes(UniqueID);
        System.arraycopy(buffer, 0, total, offset, buffer.length);
        offset += buffer.length;

        // StreamCode
        total[offset] = StreamCode;
        offset += Extensions.ByteSize;

        // UserCode
        total[offset] = Code;
        offset += Extensions.ByteSize;

        // Time
        buffer = Extensions.getBytes(System.currentTimeMillis());
        System.arraycopy(buffer, 0, total, offset, buffer.length);
        offset += buffer.length;

        // Data
        System.arraycopy(data, 0, total, offset, data.length);

        return total;
    }

    // Gives you chunk byte[] with Chunk.GetHash() and StreamCodes.Confirm, ready to be sent over Socket.
    byte[] getChunkConfirmation(Chunk chunk) {
        if (chunk == null) throw new IllegalArgumentException("Passed Chunk was null!");
        return getReadyData(chunk.getHashData(), StreamCodes.Confirm.getValue(), chunk.Code);
    }

    /// This should be used only in send purposes!
    /// Gives you chunk byte[] with header, ready to be sent over Socket.
    byte[] getChunkData(Chunk chunk) throws IOException {
        if (chunk == null) throw new IllegalArgumentException("Passed Chunk was null!");
        return getReadyData(chunk.readAllBytes(), StreamCode, chunk.Code);
    }

    // All processed chunks are saved here.
    public List<Chunk> Chunks = new ArrayList<Chunk>();

    // Parse data into chunk.
    Chunk parseChunk(byte[] data) throws Exception {
        if (!isValidFormat(data)) throw new DataFormatException("Invalid format!");

        int offset = 0;

        // Size
        long Size = Extensions.getLong(data, offset);//BitConverter.ToInt64(data, offset);
        offset += Extensions.LongSize;

        // UniqueID
        int UniqueID = Extensions.getInt(data, offset);//BitConverter.ToInt32(data, offset);
        offset += Extensions.IntSize;

        // StreamCode
        byte StreamCode = data[offset];
        offset += Extensions.ByteSize;

        // Code
        byte Code = data[offset];
        offset += Extensions.ByteSize;

        // Time
        long time = Extensions.getLong(data, offset);//BitConverter.ToInt64(data, offset);
        offset += Extensions.LongSize;

        // Message(Size - Offset)
        long pos = getLastPosition(false);
        Chunk chunk = new Chunk(this, Code, StreamCode, pos, Extensions.LimitToRange((int) (Size - pos), 0, data.length - offset), time, System.currentTimeMillis());
        chunk.Confirmation = TypesOConfirmation.Confirmed;
        //Log.e("ERROR", "Check this out! DataPackage 177.");
        return chunk;
    }

    // Save a chunk to Chunks array and its data to Stream.
    void saveChunk(Chunk chunk, byte[] data, int offsetInData, boolean calculatePosition, boolean calculateLength) throws Exception {
        if (chunk == null || data == null)
            throw new IllegalArgumentException("Cannot save null objects!");
        if (offsetInData < 0) throw new IllegalArgumentException("Offset cannot be negative.");

        if (calculatePosition)
            chunk.Position = getLastPosition(false); // getLength();
        if (calculateLength)
            chunk.Length = Extensions.LimitToRange((int) (Size - chunk.Position), 0, Buffer);


        //if (chunk.Length != data.Length - offsetInData) throw new InvalidDataException("Chunk length has to be the same with data length - offset.");
        if (Chunks.contains(chunk)) throw new Exception("Chunk was already saved!");

        Chunks.add(chunk);

        _OutputStream.write(data, offsetInData, chunk.Length);
        /*
        Stream.Position = chunk.Position;
        Stream.Write(data, offsetInData, chunk.Length);
        Stream.Flush();
        */
    }

    // Get next chunk to send!
    // Save = true, elevatedSearch = false
    Chunk nextChunk() {
        return nextChunk(true, false);
    }

    Chunk nextChunk(boolean save, boolean elevatedSearch) {
        long position = getLastPosition(elevatedSearch);
        Chunk chunk = new Chunk(this, Code, StreamCode, position, Extensions.LimitToRange((int) (Size - position), 0, Buffer), System.currentTimeMillis(), -1);
        if (save) Chunks.add(chunk);
        return chunk;
    }

    // Get last position from last Chunk.
    long getLastPosition() {
        return getLastPosition(false);
    }

    long getLastPosition(boolean extraSearch) {
        Chunk item = null;
        if (extraSearch) {
            long max = getChunksStream().mapToLong(x -> x.Position).max().getAsLong();
            item = getChunksStream().filter(x -> x.Position == max).findFirst().get();//Chunks.LastOrDefault(x = > x.Position == max);
        } else item = getChunksStream().reduce((a, b) -> b).orElse(null);

        long position = 0;

        if (item != null)
            position = item.Position + item.Length;

        return position;
    }

    // This is just for receive purposes!
    // CacheFolder = null, writeData = true
    public DataPackage(byte[] data, String CacheFolder, boolean writeData) throws DataFormatException {
        if (!isValidFormat(data)) throw new DataFormatException("Invalid format!");
        /*
        if (CacheFolder != null && Directory.Exists(CacheFolder))
            this.CacheFolder = CacheFolder;
        */
        int offset = 0;

        // Size
        this.Size = Extensions.getLong(data, offset);//BitConverter.ToInt64(data, offset);
        offset += Extensions.LongSize;

        // UniqueID
        this.UniqueID = Extensions.getInt(data, offset);//BitConverter.ToInt32(data, offset);
        offset += Extensions.IntSize;

        // StreamCode
        this.StreamCode = data[offset];
        offset += Extensions.ByteSize;

        // Code
        this.Code = data[offset];
        offset += Extensions.ByteSize;

        // Time
        long time = Extensions.getLong(data, offset);//BitConverter.ToInt64(data, offset);
        offset += Extensions.LongSize;

        // Message(Size - Offset)
        resetStream();
        if (writeData) {
            try {
                _OutputStream.write(data, offset, data.length - offset);
            } catch (IOException ignored) {

            } finally {
                Chunk chunk = new Chunk(this, Code, StreamCode, 0, data.length - offset, time, System.currentTimeMillis());
                Chunks.add(chunk);
            }
        }
    }

    // Create a new DataPackage and use FileStream as source for Stream. For sending purposes.
    public DataPackage(int UniqueID, FileInputStream Stream, byte Code, boolean ConfirmationSystem) throws IOException {
        this.UniqueID = UniqueID;
        this._OutputStream = new FileOutputStream(Stream.getFD());
        this.Size = getLength();
        this.Code = Code;
        this.ConfirmationSystem = ConfirmationSystem;
    }

    // Create a new DataPackage ready to be writen to and sent.
    public DataPackage(int UniqueID, long Size, byte Code, String CacheFolder, boolean ConfirmationSystem) {
        this.UniqueID = UniqueID;
        this.Size = Size;
        this.Code = Code;
        //if (!string.IsNullOrEmpty(CacheFolder) && Directory.Exists(CacheFolder)) this.CacheFolder = CacheFolder;
        this.ConfirmationSystem = ConfirmationSystem;
        resetStream();
    }

    // Expected size of this package.
    private long Size = 0;

    public long getSize() {
        return Size;
    }

    // Might or might not be unique.
    private int UniqueID = -1;

    public int getUniqueID() {
        return UniqueID;
    }

    // In case of buffering it is going to be saved here.
    private String Path = null;

    public String getPath() {
        return Path;
    }

    // Expected size is bigger than allowed MaximumUnBuffered. This indicates it is being saved to a temporary file.
    //private boolean _IsBuffered = false;

    public boolean isBuffered() {
        return Size > MaximumUnBuffered || _OutputStream instanceof FileOutputStream;
        //return _IsBuffered;
    }

    // This package stream code. Usualy StreamCodes.Append.
    byte StreamCode = StreamCodes.Append.getValue();

    // This is user very own code descriptor.
    private byte Code = 0;

    public byte getCode() {
        return Code;
    }

    public byte[] getHashData() throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        InputStream is = getInputStream();
        byte[] buffer = new byte[Buffer];
        int read;
        while ((read = is.read(buffer)) != -1) {
            md.update(buffer, 0, read);
        }
        return md.digest();
    }

    // Get MD5 hash of this package.
    // It calculates hash of current stream data.
    public String getHash() throws Exception {
        return Extensions.getReadableHexadecimal(getHashData());
    }

    // Don't want this package to be confirmed? Want faster transmission?
    public boolean ConfirmationSystem = true;

    // Confirm a chunk with its hash.
    // This method is automatic, so a false result means invalid hash, or nothing to confirm.
    boolean confirmChunk(byte[] data, int offset, int count) throws Exception {
        long received = System.currentTimeMillis();

        if (data == null) throw new IllegalArgumentException("Data array was null!");
        if (offset < 0 || count < 0)
            throw new IndexOutOfBoundsException("Offset and count cannot be negative!");
        if (offset + count > data.length)
            throw new IndexOutOfBoundsException("Offset + count cannot be bigger than data.Length!");

        boolean confirmed = false;

        if (!ConfirmationSystem) throw new Exception("Confirmation system is not enabled!");

        byte[] hash = new byte[count];
        System.arraycopy(data, offset, hash, 0, count);

        Chunk chunk = getChunksStream().filter(x -> x.Confirmation == TypesOConfirmation.NotConfirmed && x.getHashData().equals(hash)).findFirst().orElse(null);//Chunks.FirstOrDefault(x => x.Confirmation == TypesOConfirmation.NotConfirmed && x.GetHashData().SequenceEqual(hash));

        if (chunk != null) {
            chunk.Confirmation = TypesOConfirmation.Confirmed;
            chunk.Received = received;
            confirmed = true;
        }

        return confirmed;
    }

    // Using this the sending queue processor determinates the next message that will be sent.
    public TypesOPriority Priority = TypesOPriority.Normal;

    // If this package length is bigger, it will be buffered.
    public static final int MaximumUnBuffered = 1024 * 1024 * 4;

    // Default buffer for normal messages transmission.
    public static final int DefaultBuffer = 4096;

    // Default offset of message itself on encoding and decoding.
    public static final int DefaultMessageOffset = Extensions.LongSize * 2 + Extensions.IntSize * 2 + Extensions.ByteSize * 2;

    // Wanna set you own buffer size?!
    public int Buffer = DefaultBuffer;

    // This tells you if is valid package. Means if bigger or equal to DefaultMessageOffset.
    public static boolean isValidFormat(byte[] data) {
        return data.length >= DefaultMessageOffset;
    }

    String _CacheFolder = "";//System.IO.Path.GetTempPath();

    // This is where temporary files are being stored.
    // By default this equals Path.GetTempPath().
    public String getCacheFolder() {
        return _CacheFolder;
    }

    public void setCacheFolder(String value) {
        if (value == _CacheFolder) return;

        File dir = new File(value);

        if (dir.exists() && dir.isDirectory())
            _CacheFolder = value;
        else
            _CacheFolder = Extensions.getTempDirectory();

    }

    // Reset entire stream. Be careful with this! it is not for kids!
    public void resetStream() {
        try {
            File f = new File(Path);
            if (f.exists()) f.delete();
            Path = null;
            if (Size > MaximumUnBuffered) {
                File tmp = File.createTempFile("blankboy", ".tmp", new File(_CacheFolder));
                Path = tmp.getAbsolutePath();
                _OutputStream = new FileOutputStream(tmp);
            } else _OutputStream = new ByteArrayOutputStream();
        } catch (Exception ignored) {

        }
    }

    /// Get human readable version of this package.
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("{");
        sb.append("UniqueID = " + UniqueID);
        sb.append("Size = " + Size);
        sb.append("Buffer = " + Buffer);
        sb.append("Chunks = " + Chunks.size());

        sb.append("\nFirstSent = " + firstSent());
        sb.append("FirstReceived = " + firstReceived());
        sb.append("LastSent = " + lastSent());
        sb.append("LastReceived = " + lastReceived());

        sb.append("\nIsBuffered = " + isBuffered());
        if (isBuffered()) sb.append("TempPath = " + Path);
        sb.append("IsFinished = " + isFinished());
        if (!isFinished()) {
            sb.append("PercentageDone = " + Percentage());
            sb.append("Stream = " + getLength());
        } else {
            try {
                sb.append("Hash = " + getHash());
            } catch (Exception ignored) {

            }
        }

        sb.append("\nCode = " + Code);
        sb.append("StreamCode = " + StreamCode);

        sb.append("\nPriority = " + Priority);
        sb.append("ConfirmationSystem = " + ConfirmationSystem);

        sb.append("\n --- Average Values --- ");
        sb.append("Latency = " + averageLatency());
        sb.append("Speed = " + averageSpeed(Data.KiloBytes));
        sb.append("}");

        return sb.toString();

    }
}