package me.blankboy.tcpclient;

import java.util.Date;

public interface ConnectionListener{
    void onMessageReceived(Message message, Connection sender);
    void onDataReceived(byte[] data, Date time, Connection sender);
    void onException(Exception ex, Connection sender);
    void onLog(Log log, Connection sender);
    void onLoginResponse(boolean IsLoggedIn, Connection sender);
}