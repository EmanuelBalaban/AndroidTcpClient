package me.blankboy.tcpclientv2;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Connection {
    private Logger Console = new Logger();

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
                    DataPackage dataPackage = null;
                    do {
                        UpdateStatus(StatusType.LISTENING);
                        System.gc();
                        try
                        {
                            while (dataPackage == null)
                            {
                                UpdateStatus(StatusType.LISTENING);
                                if (AwaitingCancellation) break;
                                if (Socket.getInputStream().available() > 0)
                                {
                                    UpdateStatus(StatusType.RECEIVING);
                                    dataPackage = new DataPackage();
                                    dataPackage.Data = Read();
                                    dataPackage.ReceivedTime = new Date(System.currentTimeMillis());
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
                }
            }
        }).start();
    }
    byte[] Read() {
        System.gc();

        try {
            byte[] sizeinfo = new byte[4];

            int totalread, currentread;


            currentread = totalread = Socket.getInputStream().read(sizeinfo);

            while (totalread < sizeinfo.length && currentread > 0) {
                currentread = Socket.getInputStream().read(sizeinfo, totalread, sizeinfo.length - totalread);


                totalread += currentread;
            }

            int messagesize = 0;

            messagesize |= sizeinfo[0];
            messagesize |= (((int) sizeinfo[1]) << 8);
            messagesize |= (((int) sizeinfo[2]) << 16);
            messagesize |= (((int) sizeinfo[3]) << 24);

            byte[] data = new byte[messagesize];

            totalread = 0;
            currentread = totalread = Socket.getInputStream().read(data, totalread, data.length - totalread);

            while (totalread < messagesize && currentread > 0) {
                currentread = Socket.getInputStream().read(data, totalread, data.length - totalread);
                totalread += currentread;
            }

            return Arrays.copyOf(data, totalread);
            //return new String(data, 0, totalread);
        } catch (Exception ex) {
            return null;
        }
    }
    public void Send(byte[] data){
        System.gc();
        final StatusType status = _Status;
        UpdateStatus(StatusType.SENDING);
        final byte[] finalData = data;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
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
                    Socket.getOutputStream().write(finalData);
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