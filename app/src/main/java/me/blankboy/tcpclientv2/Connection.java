package me.blankboy.tcpclientv2;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import me.blankboy.androidtcpclient.Variables;
import me.blankboy.extensions.Extensions;

public class Connection {
    public Logger Console = new Logger();

    private Socket Socket;

    public Socket getSocket() {
        return Socket;
    }
    public boolean IsConnected(){
        if (Socket != null && Socket.isConnected()) return true;
        return false;
    }

    private InetSocketAddress UniqueAddress;

    public InetSocketAddress getRemoteAddress() {
        return UniqueAddress;
    }

    private boolean _IsWaitingForData = false;

    public boolean IsWaitingForData() {
        return _IsWaitingForData;
    }

    private List<ConnectionListener> Listeners = new ArrayList<>();

    public void addListener(ConnectionListener listener) {
        if (!Listeners.contains(listener))
            Listeners.add(listener);
    }

    private void broadcastPackage(DataPackage dataPackage) {
        for (ConnectionListener l : Listeners)
            l.onDataReceived(dataPackage, this);
    }

    private void broadcastException(Exception ex) {
        for (ConnectionListener l : Listeners)
            l.onException(ex, this);
    }

    private void broadcastStatus(StatusType status) {
        for (ConnectionListener l : Listeners)
            l.onStatusChanged(status, this);
    }

    StatusType _LastStatus = StatusType.NULL;
    StatusType _Status = StatusType.DISCONNECTED;

    public StatusType getStatus() {
        return _Status;
    }

    void UpdateStatus(StatusType newStatus) {
        _LastStatus = _Status;
        if (_Status == newStatus) return;
        _Status = newStatus;
        broadcastStatus(_Status);
    }

    void DowngradeStatus() throws Exception {
        if (_LastStatus == StatusType.NULL)
            throw new Exception("Unable to downgrade to Status.NULL!");
        UpdateStatus(_LastStatus);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            Disconnect();
        } finally {
            super.finalize();
        }
    }

    public Connection() {

    }

    public Connection(String Hostname, int Port) {
        this.Hostname = Hostname;
        this.Port = Port;
    }

    public Connection(String Hostname, int Port, int Timeout) {
        this.Hostname = Hostname;
        this.Port = Port;
        this.Timeout = Timeout;
    }

    public Connection(InetSocketAddress address) {
        Hostname = address.getHostString();
        Port = address.getPort();
    }

    public Connection(InetSocketAddress address, int Timeout) {
        Hostname = address.getHostString();
        Port = address.getPort();
        this.Timeout = Timeout;
    }

    public String Hostname = "";
    public int Port = -1;
    private int Timeout = 5000;

    public void Connect() throws NullPointerException {
        if (Hostname == "" || Port == -1)
            throw new NullPointerException("Hostname and port were not specified in call.");
        Connect(Hostname, Port, Timeout);
    }

    public void Connect(String Hostname, int Port, int Timeout) {
        this.Hostname = Hostname;
        this.Port = Port;
        this.Timeout = Timeout;

        final String host = Hostname;
        final int port = Port;
        final int timeout = Timeout;

        Socket = new Socket();
        UniqueAddress = new InetSocketAddress(Hostname, Port);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Socket.connect(new InetSocketAddress(host, port));
                    Socket.connect(new InetSocketAddress(host, port), timeout);
                } catch (Exception ex) {
                    broadcastException(ex);
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {

        } finally {
            if (Socket.isConnected()) {
                //Console.Log("Connected to " + UniqueAddress.getHostString() + ":" + UniqueAddress.getPort());
                _IsWaitingForData = false;
                UpdateStatus(StatusType.CONNECTED);
            }
            else UpdateStatus(StatusType.DISCONNECTED);
        }
    }
    public void StopWaiting() {
        AwaitingCancellation = true;
        try {
            while (_IsWaitingForData) ;
        } catch (Exception ex) {

        }
        AwaitingCancellation = false;
        UpdateStatus(StatusType.CONNECTED);
    }
    boolean AwaitingCancellation = false;

    public void Disconnect()
    {
        try
        {
            _IsWaitingForData = false;
            AwaitingCancellation = true;
            Socket.close();
        }
        catch (Exception ex)
        {
            broadcastException(ex);
        }
        finally
        {
            UpdateStatus(StatusType.DISCONNECTED);
        }
    }

    public void StartWaiting(){
        if (_IsWaitingForData) return;
        _IsWaitingForData = true;
        UpdateStatus(StatusType.LISTENING);
        final Connection sender = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    do {
                        DataPackage dataPackage = null;
                        UpdateStatus(StatusType.LISTENING);
                        System.gc();
                        try
                        {
                            while (true)//dataPackage == null)
                            {
                                UpdateStatus(StatusType.LISTENING);
                                if (AwaitingCancellation) break;
                                if (Socket.getInputStream().available() > 0)
                                {
                                    UpdateStatus(StatusType.RECEIVING);
                                    Read();
                                    //dataPackage.Data = Read();
                                    //dataPackage.ReceivedTime = new Date(System.currentTimeMillis());
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            broadcastException(ex);
                        }
                        if (AwaitingCancellation) break;
                        if (dataPackage != null)
                        {
                            System.gc();
                            broadcastPackage(dataPackage);
                        }
                    } while (Socket.isConnected());
                    if (!AwaitingCancellation)
                    {
                        Disconnect();
                    }
                }
                catch (Exception ex)
                {
                    broadcastException(ex);
                } finally {
                    _IsWaitingForData = false;
                    if (Socket.isConnected()) UpdateStatus(StatusType.CONNECTED);
                    if (!AwaitingCancellation) StartWaiting();
                }
            }
        }).start();
    }

    public HashMap<Long, byte[]> SavedPackages = new HashMap<>();

    void ProcessPackage(DataPackage dp){
        byte[] result = dp.Data;
        if (SavedPackages.containsKey(dp.UniqueID)){
            byte[] saved = SavedPackages.get(dp.UniqueID);
            result = new byte[saved.length + dp.Data.length];
            System.arraycopy(saved, 0, result, 0, saved.length);
            System.arraycopy(dp.Data, 0, result, saved.length, dp.Data.length);
        }
        if (dp.isMore)
            SavedPackages.put(dp.UniqueID, result);
        else{
            SavedPackages.remove(dp.UniqueID);
            dp.Data = result;
            dp.isMore = false;
            dp.isCached = false;
            dp.ReceivedTime = new Date(System.currentTimeMillis());
            broadcastPackage(dp);
        }
    }

    int ReadMessageSize(InputStream bf){
        int messagesize = 0;
        try {
            BufferedInputStream in = new BufferedInputStream(bf);

            byte[] sizeinfo = new byte[4];

            int currentread, totalread = 0;
            currentread = totalread = in.read(sizeinfo);

            while (totalread < sizeinfo.length && currentread > 0) {
                currentread = in.read(sizeinfo, totalread, sizeinfo.length - totalread);
                totalread += currentread;
            }

            messagesize |= sizeinfo[0];
            messagesize |= (((int) sizeinfo[1]) << 8);
            messagesize |= (((int) sizeinfo[2]) << 16);
            messagesize |= (((int) sizeinfo[3]) << 24);
        }
        catch (Exception ex){

        }
        finally {
            return messagesize;
        }
    }

    void Read() throws IOException {
        DataPackage result = new DataPackage();
        result.UniqueID = System.currentTimeMillis();
        result.ReceivedTime = new Date(result.UniqueID);

        InputStream in = Socket.getInputStream();
        BufferedInputStream bf = new BufferedInputStream(in);

        int messagesize = ReadMessageSize(in);
        if (messagesize <= 0) return;

        byte[] buffer = new byte[Extensions.LimitToRange(messagesize, 0, DataPackage.Buffer)];
        int bytesRead, totalread = 0;

        // here i could use same size info method
        while (totalread < messagesize && (bytesRead = bf.read(buffer)) > 0) {
            result.isMore = true;
            if (result.Data != null) ProcessPackage(result);
            result.Data = Arrays.copyOf(buffer, bytesRead);

            totalread += buffer.length;//bytesRead;
            buffer = new byte[Extensions.LimitToRange(messagesize - totalread, 0, DataPackage.Buffer)];
        }
        result.isMore = false;
        if (result.Data != null) ProcessPackage(result);

        /*
        byte[] sizeinfo = new byte[4];
        int messagesize = 0;
        int currentread, totalread = 0;
        currentread = totalread = in.read(sizeinfo);

        while (totalread < sizeinfo.length && currentread > 0) {
            currentread = in.read(sizeinfo, totalread, sizeinfo.length - totalread);
            totalread += currentread;
        }

        messagesize |= sizeinfo[0];
        messagesize |= (((int) sizeinfo[1]) << 8);
        messagesize |= (((int) sizeinfo[2]) << 16);
        messagesize |= (((int) sizeinfo[3]) << 24);

        if (messagesize <= 0) return null;

        if (messagesize >= Runtime.getRuntime().maxMemory()) {
            File file = new File(Variables.FilesDir, System.currentTimeMillis() + ".cache");
            OutputStream output = new FileOutputStream(file);
            BufferedOutputStream bf = new BufferedOutputStream(output);

            int bytesRead;

            InputStream in = Socket.getInputStream();

            if (sizeinfo != null)
                bf.write(sizeinfo, 0, totalread);

            byte[] buffer = new byte[2048];
            while ((bytesRead = in.read(buffer)) != -1) {
                bf.write(buffer, 0, bytesRead);
                totalread += bytesRead;
            }

            bf.close();
            output.close();
            in.reset();

            result.isCached = true;
            Console.Log("Cached file!!!!!!!!!!!!!!!!!!");
            //result.Data = file.getPath().getBytes();
        }
        if (messagesize < Runtime.getRuntime().maxMemory()) {

            byte[] data = new byte[messagesize];

            totalread = 0;
            currentread = totalread = Socket.getInputStream().read(data, totalread, data.length - totalread);
            System.gc();

            while (totalread < messagesize && currentread > 0) {
                currentread = Socket.getInputStream().read(data, totalread, data.length - totalread);
                System.gc();
                totalread += currentread;
            }

            result.Data = Arrays.copyOf(data, totalread);
        }
        */

        // result.ReceivedTime = new Date(System.currentTimeMillis());
        // return result;
    }
    public void Send(byte[] data){
        if (!_IsWaitingForData) StartWaiting();
        System.gc();
        final StatusType Status = _Status;
        UpdateStatus(StatusType.SENDING);
        final byte[] FinalData = data;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] finalData = FinalData;
                StatusType status = Status;
                try {
                    long current = System.currentTimeMillis() / 1000;
                    while (Socket.getInputStream().available() > 0) {
                        if (current + 5 < System.currentTimeMillis() / 1000)
                            return;
                    }

                    byte[] sizeinfo = new byte[4];

                    sizeinfo[0] = (byte)finalData.length;
                    sizeinfo[1] = (byte)(finalData.length >> 8);
                    sizeinfo[2] = (byte)(finalData.length >> 16);
                    sizeinfo[3] = (byte)(finalData.length >> 24);

                    Socket.getOutputStream().write(sizeinfo);

                    int offset = 0;
                    while (offset < finalData.length){
                        byte[] buffer = new byte[Extensions.LimitToRange(finalData.length - offset, 0, DataPackage.Buffer)];
                        System.arraycopy(finalData, 0, buffer, 0, buffer.length);
                        Socket.getOutputStream().write(buffer);
                        offset += buffer.length;
                    }
                } catch (Exception ignored){

                } finally {
                    System.gc();
                    UpdateStatus(status);
                }
            }
        });
        thread.start();
    }
}