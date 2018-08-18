package me.blankboy.remotecommunicationutils;

import java.util.Date;

public interface ServerListener {
    void onStatusChanged(Server.StatusType status, Server sender);
    void onExceptionOccured(Exception ex, Server sender);
    void onClientConnected(Connection connection, Date time, Server server);
    void onClientDisconnected(Connection connection, Date time, Server server);
}
