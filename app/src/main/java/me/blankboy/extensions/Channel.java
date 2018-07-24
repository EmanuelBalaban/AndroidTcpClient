package me.blankboy.extensions;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.blankboy.androidtcpclient.Variables;
import me.blankboy.tcpclientv2.*;

public class Channel implements ConnectionListener {
    public static final int UrgentMessageSize = 1024;

    public COMMANDS COMMANDS = new COMMANDS(this);

    private ContentResolver contentResolver;

    public Logger Console = new Logger();
    public boolean CriticalDebug = false;
    public String LastMessage;

    private FileTransfer searchFileTransfer(String UniqueIdentity, TransferState State, List<FileTransfer> list){
        for (FileTransfer item : list)
            if (item.UniqueIdentity() == UniqueIdentity && item.State.equals(State))
                return item;
        return null;
    }
    private FileTransfer searchFileTransfer(byte[] data, TransferState State, List<FileTransfer> list){
        for (FileTransfer item : list)
            if (item.Equals(data) && item.State.equals(State))
                return item;
        return null;
    }

    public List<FileTransfer> IncomingFiles = new ArrayList<>();
    public List<FileTransfer> OutgoingFiles = new ArrayList<>();

    private List<ChannelListener> Listeners = new ArrayList<>();
    public void addListener(ChannelListener listener) {
        if (!Listeners.contains(listener))
            Listeners.add(listener);
    }

    private void broadcastMessage(Message message) {
        for (ChannelListener l : Listeners)
            l.onMessageReceived(message, this);
    }

    public Connection Primary;
    public Connection Secondary;

    public LoginState PrimaryLoginState = LoginState.NULL;
    public LoginState SecondaryLoginState = LoginState.NULL;

    public Channel(String hostname, int port, ContentResolver contentResolver){
        this.contentResolver = contentResolver;
        Primary = new Connection(hostname, port);
        Primary.Connect();
        Primary.addListener(this);
        Primary.StartWaiting();
        SendMessage("[QUERY]DATA_SERVER");
    }

    public void Disconnect(){
        if (Primary != null) Primary.Disconnect();
        if (Secondary != null) Secondary.Disconnect();
        Console.Log("Disconnected from server!");
        System.gc();
    }

    public void SendMessage(String message) {
        if (Primary == null || !Primary.IsConnected()) return;//throw new Exception("Not connected!");
        LastMessage = message;
        Primary.Send(message.getBytes());
        System.gc();
    }

    public void SendFile(Uri fileUri, Context context) throws Exception {
        if (SecondaryLoginState != LoginState.OK) throw new Exception("Not authenticated to data server!");
        System.gc();

        String filename = Extensions.getFileName(context, fileUri);
        byte[] data = Extensions.readAllBytes(contentResolver, fileUri);

        FileTransfer ft = new FileTransfer(filename, Extensions.getMD5Hash(data), data.length);
        ft.LocalUri = fileUri;
        ft.State = TransferState.REGISTER;
        OutgoingFiles.add(ft);

        Console.Log("Registering file '" + ft.FileName + "' to server...");
        String registrationRequest = "[COMMAND]REGISTER:" + ft.UniqueIdentity();
        Secondary.Send(registrationRequest.getBytes());
        System.gc();
    }

    boolean isUrgentMessage(DataPackage dataPackage){
        return dataPackage.Data.length <= UrgentMessageSize;
    }

    public String Username;
    public String Password;
    public void Login(String username, String password, boolean encryptPassword){
        Username = username;
        Password = password;
        String message = "[LOGIN_REQUEST]" + username + ":" + (encryptPassword ? Extensions.getMD5Hash(password.getBytes()) : password);
        SendMessage(message);
    }

    @Override
    public void onDataReceived(DataPackage dataPackage, Connection sender) {
        System.gc();
        if (isUrgentMessage(dataPackage) || sender == Primary){
            String message = new String(dataPackage.Data, 0, dataPackage.Data.length);

            boolean pool = false;
            if (message.equalsIgnoreCase("[GOODBYE]"))
            {
                pool = true;
                Disconnect();
            }
            else if (message.equalsIgnoreCase("[AUTHENTICATE]")){
                if (sender == Secondary){
                    pool = true;
                    Secondary.Send(("[LOGIN_REQUEST]" + Extensions.getIPAddress(true) + String.valueOf(Primary.getSocket().getLocalPort())).getBytes());
                } else {
                    pool = true;
                    broadcastMessage(new Message(message, new Date(System.currentTimeMillis())));
                    return;
                }
            }
            else if (message.equalsIgnoreCase("[LOGIN_OK]"))
            {
                pool = true;
                if (sender == Primary) {
                    PrimaryLoginState = LoginState.OK;
                    Console.Log("Successfully logged in!");
                } else{
                    SecondaryLoginState = LoginState.OK;
                    Console.Log("\nAuthenticated to data channel!");
                }
            }
            else if (message.startsWith("[LOGIN_REJECT]"))
            {
                pool = true;
                if (sender == Primary) {
                    PrimaryLoginState = LoginState.REJECTED;
                    Console.Log("Login request was rejected with status '" + message.substring("[LOGIN_REJECT]".length()) + "'");
                    Disconnect();
                } else{
                    SecondaryLoginState = LoginState.REJECTED;
                }
            }
            else if (message.startsWith("[QUERY]"))
            {
                String query = message.substring("[QUERY]".length());
                if (query.equalsIgnoreCase("IsDataConnection"))
                {
                    pool = true;
                    SendMessage("[QUERY_RESULT]" + String.valueOf(sender == Secondary));
                }
            }
            else if (message.startsWith("[QUERY_RESULT]")){
                String result = message.substring("[QUERY_RESULT]".length());
                if (LastMessage.equalsIgnoreCase("[QUERY]DATA_SERVER"))
                {
                    pool = true;
                    if (result.contains(":")) {
                        String[] ar = result.split(":");
                        if (ar.length >= 2) {
                            Console.Log("\nReceived data server connection info!");

                            Secondary = new Connection(ar[0], Integer.valueOf(ar[1]));
                            Secondary.Connect();
                            Secondary.addListener(this);
                            Secondary.StartWaiting();

                            String login = "[LOGIN_REQUEST]" + Extensions.getIPAddress(true) + ":" + String.valueOf(Primary.getSocket().getLocalPort());
                            Secondary.Send(login.getBytes());
                        }
                    }
                }
            }
            else if ((PrimaryLoginState == LoginState.OK && sender == Primary) || (SecondaryLoginState == LoginState.OK && sender == Secondary))
            {
                if (message.startsWith("[COMMAND]"))
                {
                    String command = message.substring("[COMMAND]".length());
                    if (command.startsWith("CONFIRM:"))
                    {
                        pool = true;
                        String uniqueIdentity = command.substring("CONFIRM:".length());
                        FileTransfer ft = searchFileTransfer(uniqueIdentity, TransferState.REGISTER, OutgoingFiles);
                        if (ft != null)
                        {
                            ft.State = TransferState.SENDING;
                            Console.Log("Sending file '" + ft.FileName + "' ...");
                            sender.Send(Extensions.readAllBytes(contentResolver, ft.LocalUri));
                        }
                    }
                    else if (command.startsWith("COMPLETE:"))
                    {
                        pool = true;
                        String uniqueIdentity = command.substring("COMPLETE:".length());
                        FileTransfer ft = searchFileTransfer(uniqueIdentity, TransferState.SENDING, OutgoingFiles);
                        if (ft != null)
                        {
                            ft.State = TransferState.COMPLETE;
                            Console.Log("File '" + ft.FileName + "' was successfully sent!");
                        }
                    }
                    else if (command.startsWith("REGISTER:"))
                    {
                        pool = true;
                        String uniqueIdentity = command.substring("REGISTER:".length());

                        FileTransfer incoming = searchFileTransfer(uniqueIdentity, TransferState.REGISTER, IncomingFiles);
                        if (incoming == null)
                        {
                            try
                            {
                                incoming = new FileTransfer(uniqueIdentity);
                                incoming.State = TransferState.REGISTER;
                                IncomingFiles.add(incoming);
                            }
                            catch(Exception ex)
                            {
                                Console.Log(ex.toString(), LogType.ERROR);
                                incoming = null;
                            }
                        }
                        if (incoming != null)
                        {
                            Console.Log("Incoming file: " + incoming.FileName);
                            SendMessage("[COMMAND]CONFIRM:" + incoming.UniqueIdentity());
                        }
                    }
                }

                if ((pool && CriticalDebug) || (!isUrgentMessage(dataPackage) && !pool))
                    broadcastMessage(new Message(message, dataPackage.ReceivedTime));
            }
        } else if (SecondaryLoginState == LoginState.OK){
            FileTransfer incoming = searchFileTransfer(dataPackage.Data, TransferState.REGISTER, IncomingFiles);
            if (incoming != null)
            {
                String filename = incoming.FileName;

                File file = new File(Variables.GetAppMainDirectory(), filename);
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    Console.Log(e.toString(), LogType.ERROR);
                }

                try{
                    FileOutputStream outputStream = new FileOutputStream(file);
                    outputStream.write(dataPackage.Data);
                    outputStream.close();

                    Console.Log("Successfully downloaded file '" + filename + "'");
                    sender.Send(("[COMMAND]COMPLETE:" + incoming.UniqueIdentity()).getBytes());
                    incoming.State = TransferState.COMPLETE;
                } catch (Exception ex){
                    sender.Send(("[COMMAND]ERROR:" + incoming.UniqueIdentity()).getBytes());
                }
            }
        }
        System.gc();
    }
    @Override
    public void onException(Exception ex, Connection sender) {
        Console.Log(ex.toString(), LogType.ERROR);
    }
    @Override
    public void onStatusChanged(StatusType newStatus, Connection sender) {
        if (sender == Primary && newStatus == StatusType.DISCONNECTED){
            Disconnect();
        }
    }
}
