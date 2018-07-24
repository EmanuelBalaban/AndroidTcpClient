package me.blankboy.extensions;

import android.net.Uri;

public class FileTransfer
{
    public FileTransfer(String FileName, String Signature, int Lenght)
    {
        this.FileName = FileName;
        this.Signature = Signature;
        this.Lenght = Lenght;
    }
    public FileTransfer(String UniqueIdentity) throws Exception
    {
        if (!UniqueIdentity.contains(":") || UniqueIdentity.split(":").length == 3) throw new Exception("Invalid identity!");
        String[] list = UniqueIdentity.split(":");
        this.FileName = list[0];
        this.Signature = list[1];
        this.Lenght = Integer.parseInt(list[2]);
    }

    public Uri LocalUri = null;
    public String FileName;
    public String Signature;
    public int Lenght;
    public TransferState State = TransferState.REGISTER;

    public String UniqueIdentity() {
        return FileName + ":" + Signature + ":" + Lenght;
    }

    public boolean Equals(byte[] data) {
        return (Signature.equalsIgnoreCase(Extensions.getMD5Hash(data)) && data.length == Lenght);
    }
}