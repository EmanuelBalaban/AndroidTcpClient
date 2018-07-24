package me.blankboy.extensions;

import android.content.ContentResolver;

import java.util.ArrayList;
import java.util.List;

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
    }

    public void Disconnect(){
        if (Primary != null) Primary.Disconnect();
        if (Secondary != null) Secondary.Disconnect();
        Console.Log("Disconnected from server!");
    }

    public void SendMessage(String message) {
        if (Primary == null || !Primary.IsConnected()) return;//throw new Exception("Not connected!");
        LastMessage = message;
        Primary.Send(message.getBytes());
    }

    boolean isUrgentMessage(DataPackage dataPackage){
        return dataPackage.Data.length <= UrgentMessageSize;
    }

    @Override
    public void onDataReceived(DataPackage dataPackage, Connection sender) {
        if (isUrgentMessage(dataPackage) || sender == Primary){
            String message = new String(dataPackage.Data, 0, dataPackage.Data.length);

            boolean pool = false;
            if (message.equalsIgnoreCase("[GOODBYE]"))
            {
                pool = true;
                Disconnect();
            }
            else if (message.equalsIgnoreCase("[LOGIN_OK]"))
            {
                pool = true;
                PrimaryLoginState = LoginState.OK;
                Console.Log("Successfully logged in!");
            }
            else if (message.startsWith("[LOGIN_REJECT]"))
            {
                pool = true;
                PrimaryLoginState = LoginState.REJECTED;
                Console.Log("Login request was rejected with status '" + message.substring("[LOGIN_REJECT]".length()) + "'");
                Disconnect();
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

        }
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
