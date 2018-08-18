package me.blankboy.extensions;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

public class FileTransfer
{
    public FileTransfer(String FileName, String Signature, int Lenght)
    {
        this.FileName = FileName;
        this.Signature = Signature;
        this.Length = Lenght;
    }
    public FileTransfer(String UniqueIdentity) throws Exception
    {
        if (!UniqueIdentity.contains(":") || UniqueIdentity.split(":").length != 3) throw new Exception("Invalid identity!");
        String[] list = UniqueIdentity.split(":");
        this.FileName = list[0];
        this.Signature = list[1];
        this.Length = Integer.parseInt(list[2]);
    }

    public Uri LocalUri = null;
    public String FileName;
    public String Signature;
    public int Length;
    public TransferState State = TransferState.REGISTER;

    public String UniqueIdentity() {
        return FileName + ":" + Signature + ":" + Length;
    }

    public boolean Equals(byte[] data) {
        return (Signature.equalsIgnoreCase(Extensions.getMD5Hash(data)) && data.length == Length);
    }
    public boolean Equals(InputStream is, int length) {
        try {
            return (Signature.equalsIgnoreCase(Extensions.getMD5Hash(is, 2048)) && length == Length);
        } catch (IOException e) {
            return false;
        }
    }
}