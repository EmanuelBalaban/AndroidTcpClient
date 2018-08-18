package me.blankboy.androidtcpclient;

import android.app.ActivityManager;

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
        return new File(Extensions.getInternalFolderPath(AppMainDirectory));
    }

    public static File FilesDir;

    public static ActivityManager activityManager;

    public static Logger Console = new Logger();

    public static Channel Server;
}
