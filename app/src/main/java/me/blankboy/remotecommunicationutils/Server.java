package me.blankboy.remotecommunicationutils;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

// Basic server for capturin folks.
public class Server implements ConnectionListener {

    // Interface operations
    private List<ServerListener> Listeners = new ArrayList<>();
    public void addListener(ServerListener listener){
        if (!Listeners.contains(listener)) Listeners.add(listener);
    }
    private void onStatusChanged(StatusType status){
        for (ServerListener l: Listeners)
            l.onStatusChanged(status, this);
    }
    private void onExceptionOccured(Exception ex){
        for (ServerListener l: Listeners)
            l.onExceptionOccured(ex, this);
    }
    private void onClientConnected(Connection connection, Date time){
        for (ServerListener l: Listeners)
            l.onClientConnected(connection, time, this);
    }
    private void onClientDisconnected(Connection connection, Date time){
        for (ServerListener l: Listeners)
            l.onClientDisconnected(connection, time, this);
    }

    private void broadcastException(Exception ex){
        Console.Log(ex.toString(), TypesOLog.ERROR);
        onExceptionOccured(ex);
    }

    // JPC, just personal console.
    private Logger Console = new Logger();
    public Logger getConsole(){
        return Console;
    }

    // Status stuff
    private StatusType _LastStatus = StatusType.NULL;
    private StatusType _Status = StatusType.CLOSED;
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

    @Override
    public void onDataReceived(byte[] data, Date received, Connection sender) {

    }

    @Override
    public void onStatusChanged(Connection.StatusType status, Connection sender) {
        if (status == Connection.StatusType.DISCONNECTED)
            onClientDisconnected(sender, new Date(System.currentTimeMillis()));
    }

    @Override
    public void onExceptionOccurred(Exception ex, Connection sender) {

    }

    public enum StatusType{
        NULL,
        LISTENING,
        CLOSED,
    }

    // Saved hostname from creation of this server.
    private String _Hostname = "0.0.0.0";
    public String getHostname(){
        return _Hostname;
    }

    // Saved port from portcreation of this server.
    private int _Port = 13000;
    public int getPort(){
        return _Port;
    }

    private int GetUniqueID()
    {
        Random rad = new Random();
        int id = rad.nextInt();
        while (containsAnyID(id))
            id = rad.nextInt();
        return id;
    }

    private boolean containsAnyID(int id){
        for (Connection c: Clients)
            if (c.getUniqueID() == id) return true;
        return false;
    }

    // What do ya think thes s!?
    public List<Connection> Clients = new ArrayList<>();

    private ServerSocket SocketListener;

    // Wanna see if thes s working?
    private boolean _IsListening = true;
    public boolean IsListening(){
        return _IsListening;
    }

    // Create a new instance of server with hostname = 0.0.0.0 and specified port.
    public Server(int port) {
        _Hostname = "0.0.0.0";
        _Port = port;
    }

    // Create a new server with specified hostname and port. Also you have to action Start().
    public Server(String hostname, int port)
    {
        _Port = port;
        _Hostname = hostname;
    }

    // Ready? start listening asynchronous for new users!
    public void Start() throws Exception {
        if (SocketListener != null && _IsListening) throw new Exception("Server is already started!");
        try
        {
            if (SocketListener != null) SocketListener.close();
        }
        catch (Exception ignored)
        {
        }

        try
        {
            SocketListener = new ServerSocket();
            SocketAddress so = new InetSocketAddress(_Hostname, _Port);

            SocketListener.bind(so);

            AcceptClients();
        }
        catch (Exception ex)
        {
            broadcastException(ex);
        }
    }

    private void AcceptClients() throws Exception {
        if (_IsListening) throw new Exception("Already waiting for clients!");
        _IsListening = true;
        updateStatus(StatusType.LISTENING);
        while (_IsListening && SocketListener != null && SocketListener.isBound()){
            try{
                Socket client = SocketListener.accept();

                Connection connection = new Connection(GetUniqueID(), client, Console, true);
                connection.addListener(this);

                Clients.add(connection);

                onClientConnected(connection, new Date(System.currentTimeMillis()));
            }
            catch (Exception ex){
                broadcastException(ex);
            }
        }
        _IsListening = false;
        updateStatus(StatusType.CLOSED);
    }

    // Dont want any new folks, ya now what todo!
    public void Stop()
    {
        try
        {
            _IsListening = false;
            if (SocketListener != null) SocketListener.close();
        }
        catch (Exception ex)
        {
            broadcastException(ex);
        }
    }
}
