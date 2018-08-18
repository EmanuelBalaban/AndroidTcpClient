package me.blankboy.remotecommunicationutils;

import java.util.Date;

public interface ConnectionListener {
    void onDataReceived(byte[] data, Date received, Connection sender);
    void onStatusChanged(Connection.StatusType status, Connection sender);
    void onExceptionOccurred(Exception ex, Connection sender);
}
