package me.blankboy.remotecommunicationutils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Connection implements AutoCloseable {
    @Override
    public void close() throws Exception {
        stopWaiting(true);
        disconnect();
        try {
            finalize();
        } catch (Throwable ignored) {

        }
    }

    // Interface operations
    private List<ConnectionListener> Listeners = new ArrayList<>();
    public void addListener(ConnectionListener listener){
        if (!Listeners.contains(listener)) Listeners.add(listener);
    }
    private void onStatusChanged(StatusType status){
        for (ConnectionListener l: Listeners)
            l.onStatusChanged(status, this);
    }
    private void onExceptionOccurred(Exception ex){
        for (ConnectionListener l: Listeners)
            l.onExceptionOccurred(ex, this);
    }
    private void onDataReceived(byte[] data, Date received){
        for (ConnectionListener l: Listeners)
            l.onDataReceived(data, received, this);
    }

    private void broadcastException(Exception ex){
        Console.Log(ex.toString(), TypesOLog.ERROR);
        onExceptionOccurred(ex);
    }

    // Personal console. Be careful, it contains logs.
    private Logger Console = new Logger();
    public Logger getConsole(){
        return Console;
    }

    /// Underlying Socket that makes everything possible.
    private Socket Socket;
    public Socket getSocket(){
        return Socket;
    }

    // Its what it says it is
    private int UniqueID = -1;
    public int getUniqueID(){
        return UniqueID;
    }

    // Believe this is remote IPEndPoint.
    private InetSocketAddress UniqueAddress;
    public InetSocketAddress getUniqueAddress() {
        return UniqueAddress;
    }
    public String getUniqueIdentity(){
        return UniqueAddress.toString();
    }

    // This tells you if this instance of connection is listening to messages.
    private boolean _IsWaitingForData = false;
    public boolean IsWaitingForData(){
        return _IsWaitingForData;
    }

    // Status stuff
    private StatusType _LastStatus = StatusType.NULL;
    private StatusType _Status = StatusType.NULL;
    private void updateStatus(StatusType newStatus){
        if (_Status == newStatus) return;

        _LastStatus = _Status;
        _Status = newStatus;

        onStatusChanged(newStatus);
    }
    private void downgradeStatus() throws UnsupportedOperationException {
        if (_LastStatus == StatusType.NULL) throw new UnsupportedOperationException("Unable to downgrade to Status.NULL!");
        StatusType current = _Status;
        _Status = _LastStatus;
        _LastStatus = current;
    }
    public StatusType getStatus() {
        return _Status;
    }
    public enum StatusType {
        NULL,
        CONNECTED,
        LISTENING,
        RECEIVING,
        SENDING,
        DISCONNECTED,
    }

    // Create new connection and start listening, of course if you want to.
    public Connection(int UniqueID, Socket Socket, Logger Console, boolean StartWaiting) throws Exception {
        this.UniqueID = UniqueID;
        this.Socket = Socket;
        this.Console = Console;
        UniqueAddress = (InetSocketAddress) Socket.getRemoteSocketAddress();
        if (Socket.isConnected()) updateStatus(StatusType.CONNECTED);
        if (StartWaiting) startWaiting(true);
    }

    // Check if still connected.
    public boolean isStillConnected() {
        if (Socket == null) return false;
        try {
            boolean part1 = UniqueAddress.getAddress().isReachable(1000);
            boolean part2 = (Socket.getInputStream().available() == 0);
            if (part1 && part2)
                return false;
            else
                return true;
        }
        catch (Exception ignored){

        }
        return  false;
    }

    // Disconnect and close underlying Socket.
    public void disconnect() {
        try {
            stopWaiting(true);
            Socket.close();
        } catch (Exception ignored) {
            broadcastException(ignored);
        } finally {
            updateStatus(StatusType.DISCONNECTED);
        }
    }

    // Start listening for messages asynchronously! Maximum message size is long.MAXVALUE = 9,223,372,036,854,775,807 Bytes, approximately 9223.372036854777 PetaBytes.
    public void startWaiting(boolean forced) throws Exception {
        if (isStillConnected()) throw new Exception("Socket is not connected!");
        if (forced) stopWaiting(forced);
        if (_IsWaitingForData) throw new Exception("Socket is already waiting for data!");
        try {
            ListenForMessages();
        } catch (Exception ex) {
            broadcastException(ex);
            _IsWaitingForData = false;
            updateStatus(StatusType.CONNECTED);
        }
    }

    private boolean AwaitingCancellation = false;
    private void ListenForMessages() throws Exception {
        AwaitingCancellation = false;
        if (_IsWaitingForData) throw new Exception("Already listening for messages!");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    _IsWaitingForData = true;
                    InputStream stream = Socket.getInputStream();

                    while (isStillConnected() && !AwaitingCancellation) {
                        updateStatus(StatusType.LISTENING);
                        try {
                            byte[] size = new byte[Extensions.IntSize];

                            Extensions.ReadStream(stream, size, size.length);

                            updateStatus(StatusType.RECEIVING);

                            int messageSize = Extensions.getInt(size, 0);

                            byte[] messageRaw = new byte[messageSize];

                            Extensions.ReadStream(stream, messageRaw, messageRaw.length);

                            onDataReceived(messageRaw, new Date(System.currentTimeMillis()));
                        } catch (IOException ex) {
                            disconnect();
                            broadcastException(ex);
                        } catch (Exception ex) {
                            broadcastException(ex);
                        }
                    }
                } catch (Exception ex) {
                    broadcastException(ex);
                } finally {
                    _IsWaitingForData = false;
                    updateStatus(StatusType.CONNECTED);
                }
            }
        });
        t.start();
    }

    // Stop waiting for any new messages! Notice that you can continue receiving from where you left with StartWaiting()
    public void stopWaiting(boolean forced) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AwaitingCancellation = true;
                    while (_IsWaitingForData) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            //e.printStackTrace();
                        }
                    }
                } catch (Exception ignored) {
                    broadcastException(ignored);
                } finally {
                    AwaitingCancellation = false;
                }
            }
        });
        t.start();
    }

    // Asynchronously send a byte[] starting at specified offset and with specified length, preceded by 4 bytes, its size.
    public void Send(byte[] data, int offset, int count)
    {
        if (data == null || offset < 0 || count < 0) throw new IndexOutOfBoundsException("Array is null or offset/length are negative.");
        if (count > data.length) throw new IndexOutOfBoundsException("Specified length is bigger than arrays length.");
        if (offset + count > data.length) throw new IndexOutOfBoundsException("Specified offset + length should be less than array's length.");
        if (offset > data.length) throw new IndexOutOfBoundsException("Specified offset is bigger than available length.");

        updateStatus(StatusType.SENDING);

        final byte[] finalData = data;
        final int finalOffset = offset;
        final int finalCount = count;

        Thread t = new Thread(() -> {
            byte[] threadData = finalData;
            int threadOffset = finalOffset;
            int threadCount = finalCount;

            try {
                byte[] total = new byte[Extensions.IntSize + threadCount];

                System.arraycopy(Extensions.getBytes(threadCount), 0, total, 0, Extensions.IntSize);
                System.arraycopy(threadData, threadOffset, total, Extensions.IntSize, threadCount);

                Console.Log("Sending message with length: " + total.length, TypesOLog.DEBUG);

                Socket.getOutputStream().write(total);
            } catch (Exception ex) {
                broadcastException(ex);
                if (_Status == StatusType.SENDING) downgradeStatus();
            }
        });
    }
}
