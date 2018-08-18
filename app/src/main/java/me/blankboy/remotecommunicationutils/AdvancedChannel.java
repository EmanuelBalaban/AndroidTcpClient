package me.blankboy.remotecommunicationutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

// Use this and thank me later, or you can thank me now in advance :P.
// This provides Connection extra fundamental methods and ensures there is no data loss.
public class AdvancedChannel implements AutoCloseable, ConnectionListener {
    // Think you lose this channel?
    private int UniqueID = -1;

    public int getUniqueID() {
        return UniqueID;
    }

    // Interface operations
    private List<ChannelListener> Listeners = new ArrayList<>();

    public void addListener(ChannelListener listener) {
        if (!Listeners.contains(listener)) Listeners.add(listener);
    }

    private void onExceptionOccurred(Exception ex) {
        for (ChannelListener l : Listeners)
            l.onExceptionOccurred(ex, this);
    }

    private void onDataReceived(DataPackage data, Chunk chunk) {
        for (ChannelListener l : Listeners)
            l.onDataReceived(data, chunk, this);
    }

    private void broadcastException(Exception ex) {
        Console.Log(ex.toString(), TypesOLog.ERROR);
        onExceptionOccurred(ex);
    }

    // This is useless.
    private Connection Connection;

    public Connection getConnection() {
        return Connection;
    }

    // Just basic console.
    private Logger Console;

    public Logger getConsole() {
        return Console;
    }

    // Create new advanced channel and Initialize it.
    public AdvancedChannel(Connection connection) {
        this.Connection = connection;
        InitializeChannel();
    }

    @Override
    public void close() throws Exception {
        if (Connection != null) Connection.close();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private void InitializeChannel() {
        if (Connection == null)
            throw new NullPointerException("Connection was null. Cannot initialize null object.");
        Connection.addListener(this);
        Console = Connection.getConsole();
        UniqueID = Connection.getUniqueID();
    }

    @Override
    public void onDataReceived(byte[] data, Date received, Connection sender) {
        try {
            if (DataPackage.IsValidFormat(data)) {
                DataPackage justForCheck = new DataPackage(data, CacheFolder, false);
                StreamCodes Code = StreamCodes.values()[justForCheck.StreamCode];

                if (Code == StreamCodes.Confirm) {
                    DataPackage dp = getOutgoingStream().filter(x -> x.getUniqueID() == justForCheck.getUniqueID()).findFirst().orElse(null);
                    if (dp != null) {
                        boolean result = dp.confirmChunk(data, DataPackage.DefaultMessageOffset, data.length - DataPackage.DefaultMessageOffset);
                        if (result)
                            Console.Log("Confirmed package with id: " + dp.getUniqueID(), TypesOLog.DEBUG);
                        else
                            Console.Log("Couldnt confirm package with id: " + dp.getUniqueID(), TypesOLog.DEBUG);

                    }
                } else {
                    DataPackage dp = getIncomingStream().filter(x -> x.getUniqueID() == justForCheck.getUniqueID() && x.getSize() == justForCheck.getSize()).findFirst().orElse(null);

                    if (dp == null) {
                        dp = new DataPackage(data, CacheFolder, false);
                        IncomingPackages.add(dp);
                        reportID(dp.getUniqueID());
                    }

                    Chunk chunk = dp.parseChunk(data);
                    dp.saveChunk(chunk, data, DataPackage.DefaultMessageOffset, false, false);

                    if (Code == StreamCodes.Append) {
                        // Faster will be to use normal method and chunk.GetHashData()
                        byte[] hash = chunk.getHashData();
                        DataPackage dpa = new DataPackage(dp.getUniqueID(), hash.length, (byte) 0, CacheFolder, false);
                        dpa.getOutputStream().write(hash, 0, hash.length);
                        dpa.StreamCode = StreamCodes.Confirm.getValue();
                        dpa.Priority = TypesOPriority.VeryHigh;
                        dpa.ConfirmationSystem = false;
                        Send(dpa);
                    }

                    if (dp.isFinished()) {
                        //IncomingPackages.Remove(dp);
                    }

                    onDataReceived(dp, chunk);
                }
            } else {
                // Drop connection
                Console.Log("Not valid package! Dropping connection!");
                Connection.disconnect();
            }
        } catch (Exception ex) {
            broadcastException(ex);
        }
    }

    @Override
    public void onStatusChanged(Connection.StatusType status, Connection sender) {
        if (status == me.blankboy.remotecommunicationutils.Connection.StatusType.DISCONNECTED)
            Console.Log("Connection lost!", TypesOLog.WARN);
    }

    @Override
    public void onExceptionOccurred(Exception ex, Connection sender) {
        onExceptionOccurred(ex);
    }

    // ID Allocation System
    private List<Integer> InServiceIDs = new ArrayList<>();

    // Wanna do things on your own?
    public int allocateNewID() {
        int id = InServiceIDs.size();
        Random rd = new Random();
        while (InServiceIDs.contains(id)) id = rd.nextInt();
        InServiceIDs.add(id);
        return id;
    }

    private void reportID(int UniqueID) {
        if (!InServiceIDs.contains(UniqueID)) InServiceIDs.add(UniqueID);
    }

    // Check if the underlying connection is still connected.
    public boolean isStillConnected() {
        if (Connection == null || Connection.getSocket() == null) return false;
        return Connection.isStillConnected();
    }

    // Don't want any package to be confirmed? Want faster transmission?
    public boolean ConfirmationSystem = true;

    // This is where temporary files are being stored.
    public String CacheFolder = null;//Path.GetTempPath();

    // Send a file! Actually it creates a new DataPackage with specified file stream.
    // code = 4, buffer = 4096 * 1000
    public void SendFile(String path, byte code, int buffer) throws IOException, FileNotFoundException {
        if (!new File(path).exists()) throw new FileNotFoundException();
        int UniqueID = allocateNewID();
        FileInputStream fs = new FileInputStream(path);
        DataPackage dp = new DataPackage(UniqueID, fs, code, ConfirmationSystem);
        dp.Buffer = buffer;
        Send(dp);
    }

    // Send a simple message! (or write message data to new DataPackage -> stream)
    // code = 0, uniqueID = -1
    public void SendMessage(String message, byte code, int uniqueID) {
        if (uniqueID == -1)
            uniqueID = allocateNewID();
        byte[] data = message.getBytes();
        DataPackage dp = new DataPackage(uniqueID, data.length, code, CacheFolder, ConfirmationSystem);
        try {
            dp.getOutputStream().write(data, 0, data.length);
        } catch (IOException ignored) {
        }
        Send(dp);
    }

    // Send DataPackage over the stream.
    public void Send(DataPackage dp) {
        if (dp == null) throw new IllegalArgumentException("DataPackage was null!");
        reportID(dp.getUniqueID());
        //if (!StreamSupport.stream(OutgoingQueue).anyMatch(x -> x.getUniqueID() == dp.getUniqueID()))
        if (OutgoingQueue.contains(dp)) OutgoingQueue.add(dp);
        QueueProcessor();
    }

    private void QueueProcessor() {
        if (QueueProcessorStatus == ProcessorState.Working) return;

        Console.Log("Queue processor has started working!", TypesOLog.DEBUG);
        QueueProcessorStatus = ProcessorState.Working;
        Thread t = new Thread(() -> {
            DataPackage last = null;
            while (isStillConnected() && getOutgoingStream().anyMatch(x -> x.getLastPosition() < x.getSize())) {
                System.gc();
                try {
                    if (last != null && last.getLastPosition() == last.getSize()) last = null;

                    DataPackage current = null;

                    if (QueueRule == TypesORules.Normal) {
                        current = getOutgoingStream().filter(x -> x.Priority == TypesOPriority.VeryHigh).findFirst().orElse(null);
                        if (current == null)
                            current = (last == null) ? getOutgoingStream().filter(x -> x.Priority == TypesOPriority.High).findFirst().orElse(null) : last;
                        if (current == null)
                            current = getOutgoingStream().filter(x -> x.Priority == TypesOPriority.Normal).findFirst().orElse(null);
                    } else {
                        current = getOutgoingStream().filter(x -> x.Priority == TypesOPriority.VeryHigh).reduce((a, b) -> b).orElse(null);
                        if (current == null)
                            current = (last == null) ? getOutgoingStream().filter(x -> x.Priority == TypesOPriority.High).reduce((a, b) -> b).orElse(null) : last;
                        if (current == null)
                            current = getOutgoingStream().filter(x -> x.Priority == TypesOPriority.Normal).reduce((a, b) -> b).orElse(null);
                    }

                    if (current != null) {
                        Console.Log("Processing package with id: " + current.getUniqueID(), TypesOLog.DEBUG);
                        Console.Log(current.toString(), TypesOLog.DEBUG);

                        int attemptCount = 0;

                        Chunk chunk = current.nextChunk();
                        byte[] buffer = current.getChunkData(chunk);

                        do {
                            System.gc();
                            attemptCount++;

                            Connection.Send(buffer, 0, buffer.length);

                            long now = System.currentTimeMillis();
                            while (current.ConfirmationSystem && chunk.Confirmation == TypesOConfirmation.NotConfirmed &&
                                    (System.currentTimeMillis() - now) / 10000 <= 5000)
                                Thread.sleep(1);
                        }
                        while (attemptCount < 3 && current.ConfirmationSystem && chunk.Confirmation == TypesOConfirmation.NotConfirmed);

                        if (current.ConfirmationSystem && chunk.Confirmation == TypesOConfirmation.NotConfirmed) {
                            Console.Log("Unable to process package '" + current.getUniqueID() + "'. " + String.valueOf(current.getSize() - (chunk.Position + chunk.Length)) + " bytes left!", TypesOLog.DEBUG);
                            current.close();
                            OutgoingQueue.remove(current);
                        } else {
                            //current.Position = current.Stream.Position;
                            if (current.isFinished()) {
                                Console.Log("Successfully processed package with id: " + current.getUniqueID(), TypesOLog.DEBUG);
                                current.getOutputStream().flush();
                                if (current.IsBuffered()) current.getOutputStream().close();
                                OutgoingQueue.remove(current);
                            } else if (current.Priority == TypesOPriority.Normal)
                                last = current;
                        }
                    } else OutgoingQueue.removeAll(Collections.singleton(null));
                } catch (Exception ex) {
                    broadcastException(ex);
                }
            }

            QueueProcessorStatus = ProcessorState.Stopped;
            Console.Log("Queue processor has finished working!", TypesOLog.DEBUG);
        });
        t.start();
    }

    public Stream<DataPackage> getIncomingStream() {
        return StreamSupport.stream(IncomingPackages);
    }

    public Stream<DataPackage> getOutgoingStream() {
        return StreamSupport.stream(OutgoingQueue);
    }

    // All in-receive packages on the line.
    public List<DataPackage> IncomingPackages = new ArrayList<>();

    // All out-going packages in one list. Position and priority counts!
    public List<DataPackage> OutgoingQueue = new ArrayList<>();

    // Outgoing queue processor. Indicates value 'Working' only when working.
    private ProcessorState QueueProcessorStatus = ProcessorState.Stopped;

    public ProcessorState getQueueProcessorStatus() {
        return QueueProcessorStatus;
    }

    // This is the rule on which outgoing queue is processed.
    public TypesORules QueueRule = TypesORules.Normal;
}
