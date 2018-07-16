package me.blankboy.androidtcpclient;

import me.blankboy.tcpclient.*;

public class Variables {
    public static String AppMainDirectory = "ARC Transfers";

    public static Connection PrimaryServer; // And this is for communications.
    public static Connection SecondaryServer; // This is for data transfers.
    public static boolean isSet = false;
    public static void InitializeSecondaryServer(String hostname, int port, String username, String password){
        if (isSet) return;
        SecondaryServer = new Connection(hostname, port);
        SecondaryServer.Connect();
        SecondaryServer.Login(username, password);
        isSet = true;
    }
    public static boolean IsServerLoggedIn(){
        if (IsServerConnected() && PrimaryServer.IsLoggedIn) return true;
        return false;
    }
    public static boolean IsServerConnected(){
        if (PrimaryServer == null || !Variables.PrimaryServer.IsConnected()) return false;
        return true;
    }
}
