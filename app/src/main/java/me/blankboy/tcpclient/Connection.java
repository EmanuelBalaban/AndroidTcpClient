package me.blankboy.tcpclient;

import java.net.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;

public class Connection{
    /*
    @Override
    public boolean equals(Object other){
        if (other instanceof Connection){
            Connection otherCon = (Connection) other;
            try {
                if (otherCon.Socket.getLocalAddress().equals(Socket.getLocalAddress()))
                    return true;
            }
            finally {

            }
        }
        return false;
    }
    */
    private void Log(String message){
        Log log = new Log(message, LogType.INFO);
        for (ConnectionListener l : listeners)
            l.onLog(log, this);
    }
    private void Log(String message, LogType type){
        Log log = new Log(message, type);
        for (ConnectionListener l : listeners)
            l.onLog(log, this);
    }
    private void AnnounceException(Exception ex){
        Log(ex.toString(), LogType.ERROR);
        for (ConnectionListener l : listeners)
            l.onException(ex, this);
    }

    private List<ConnectionListener> listeners = new ArrayList<ConnectionListener>();
    public void addListener(ConnectionListener listener){
        listeners.add(listener);
    }

    public boolean IsConnected(){
        return Socket.isConnected();
    }

    public boolean IsDataConnection = false;
    public boolean IsWaitingForData = false;
    public boolean IsLoggedIn = false;

    public String UniqueIdentity(){
        return Hostname + ":" + Port;
    }
    public InetSocketAddress UniqueAddress;
    public Socket Socket;

    /*
    @Override
    protected void finalize() throws Throwable {
        try {
            Disconnect();
        } finally {
            super.finalize();
        }
    }
    */

    public Connection(){

    }
    public Connection(String Hostname, int Port){
        this.Hostname = Hostname;
        this.Port = Port;
    }
    public Connection(String Hostname, int Port, int Timeout){
        this.Hostname = Hostname;
        this.Port = Port;
        this.Timeout = Timeout;
    }
    public Connection(InetSocketAddress address){
        Hostname = address.getHostString();
        Port = address.getPort();
    }
    public Connection(InetSocketAddress address, int Timeout){
        Hostname = address.getHostString();
        Port = address.getPort();
        this.Timeout = Timeout;
    }
    public String Hostname = "";
    public int Port = -1;
    private int Timeout = 5000;
    public void Connect() throws NullPointerException {
        if (Hostname == "" || Port == -1) throw new NullPointerException("Hostname and port were not specified in constructor.");
        Connect(Hostname, Port, Timeout);
    }
    public void Connect(String Hostname, int Port, int Timeout){
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
                try{
                    Socket.connect(new InetSocketAddress(host, port));
                    //Socket.connect(new InetSocketAddress(host, port), timeout);
                }
                catch (Exception ex){
                    AnnounceException(ex);
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {

        } finally {
            if (Socket.isConnected()) {
                Log("Connected to " + UniqueAddress.getHostString() + ":" + UniqueAddress.getPort());
                IsWaitingForData = false;
                WaitForData();
            }
        }
    }
    public int MaximumUMSize = 1024;
    public void StopWaiting()
    {
        AwaitingCancellation = true;
        try
        {
            while (IsWaitingForData) ;
        }
        catch (Exception ex)
        {

        }
        AwaitingCancellation = false;
    }
    boolean AwaitingCancellation = false;
    public void WaitForData()
    {
        if (IsWaitingForData) return;
        IsWaitingForData = true;
        final Connection sender = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    byte[] result;
                    Date received;
                    do {
                        result = null;
                        received = new Date();
                        try
                        {
                            while (result == null)
                            {
                                if (AwaitingCancellation) break;
                                if (Socket.getInputStream().available() > 0)
                                {
                                    result = Read();
                                    received = new Date(System.currentTimeMillis());
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            AnnounceException(ex);
                        }
                        if (AwaitingCancellation) break;
                        if (result != null)
                        {
                            for (ConnectionListener l : listeners)
                                l.onDataReceived(result, received, sender);
                            if (!IsDataConnection || (MaximumUMSize > -1 && result.length <= MaximumUMSize)) ProcessMessage(result, received, IsDataConnection);
                        }
                    } while (Socket.isConnected());
                    if (!AwaitingCancellation)
                    {
                        Disconnect();
                    }
                }
                catch (Exception ex)
                {
                    AnnounceException(ex);
                } finally {
                    IsWaitingForData = false;
                }
            }
        }).start();
    }

    public boolean CriticalDebug = false;
    private void ProcessMessage(byte[] data, Date sent, boolean urgentMessage)
    {
        String message = new String(data, 0, data.length);
        boolean pool = false;
        if (message.equalsIgnoreCase("[GOODBYE]"))
        {
            pool = true;
            Disconnect();
        }
        else if (message.equalsIgnoreCase("[LOGIN_OK]"))
        {
            pool = true;
            IsLoggedIn = true;
            Log("Successfully logged in!");
            for (ConnectionListener l : listeners)
                l.onLoginResponse(IsLoggedIn, this);
        }
        else if (message.startsWith("[LOGIN_REJECT]"))
        {
            pool = true;
            IsLoggedIn = false;
            Log("Login request was rejected with status '" + message.substring("[LOGIN_REJECT]".length()) + "'");
            for (ConnectionListener l : listeners)
                l.onLoginResponse(IsLoggedIn, this);
            Disconnect();
        }
        else if (message.startsWith("[QUERY]"))
        {
            message = message.substring("[QUERY]".length());
            if (message == "IsDataConnection")
            {
                pool = true;
                SendMessage("[QUERY_RESULT]" + String.valueOf(IsDataConnection));
            }
        }
        if ((pool && CriticalDebug) || (!urgentMessage && !pool))
            for(ConnectionListener l : listeners)
                l.onMessageReceived(new Message(message, sent), this);
    }

    public String Username;
    public String Password;
    public void Login(String username, String password){
        Username = username;
        Password = password;
        String message = "[LOGIN_REQUEST]" + username + ":" + GetMD5OfString(password);
        SendMessage(message);
    }
    private String GetMD5OfString(String string){
        byte[] digest = GetMD5(string.getBytes());
        StringBuffer sb = new StringBuffer();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
    private byte[] GetMD5(byte[] data){
        byte[] result = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            result = md.digest();
        }
        catch (Exception ignored){

        }
        return result;
    }
    public void Disconnect(){
        try {
            if (Socket != null && Socket.isConnected()) {
                SendMessage("[GOODBYE]");
                Socket.shutdownInput();
                Socket.shutdownOutput();
            }
            Socket.close();
        } catch (Exception ignored){

        } finally {
            IsLoggedIn = false;
            Log("Disconnected from server.");
        }
    }
    public String ReadMessage()
    {
        try{
            byte[] data = Read();
            return new String(data, 0, data.length);
        }
        catch (Exception ignored){
            return "";
        }
    }
    public byte[] Read() {
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
    public String LastQUERY;
    public void SendMessage(String msg)
    {
        if (msg.startsWith("[QUERY]")) LastQUERY = msg;
        Send(msg.getBytes());
    }
    public void Send(byte[] data){
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

                }
            }
        });
        thread.start();
        System.gc();
    }
}