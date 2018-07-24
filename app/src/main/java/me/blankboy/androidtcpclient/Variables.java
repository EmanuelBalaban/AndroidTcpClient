package me.blankboy.androidtcpclient;

import java.io.File;

import me.blankboy.extensions.*;
import me.blankboy.tcpclientv2.*;

public class Variables {
    public static String AppMainDirectory = "ARC Transfers";
    public static void CheckAppMainDirectory(){
        Extensions.createInternalFolder(AppMainDirectory);
    }
    public static File GetAppMainDirectory(){
        CheckAppMainDirectory();
        return new File(AppMainDirectory);
    }

    public static Logger Console = new Logger();

    public static Channel Server;
}
