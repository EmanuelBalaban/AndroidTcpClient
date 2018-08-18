package me.blankboy.tcpclientv2;

import java.util.Date;

public class DataPackage
{
    public static final int Buffer = 1024;

    public Date ReceivedTime;
    public long UniqueID;

    public byte[] Data;
    public boolean isCached = false;
    public boolean isMore = false;
}
