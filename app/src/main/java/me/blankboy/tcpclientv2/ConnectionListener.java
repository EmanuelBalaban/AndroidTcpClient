package me.blankboy.tcpclientv2;

public interface ConnectionListener{
    void onDataReceived(DataPackage dataPackage, Connection sender);
    void onException(Exception ex, Connection sender);
    void onStatusChanged(StatusType newStatus, Connection sender);
}