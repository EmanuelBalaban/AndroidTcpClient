package me.blankboy.remotecommunicationutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Logger {
    // interface stuff
    private List<LogReceiver> Listeners = new ArrayList<>();
    public void addListener(LogReceiver receiver){
        if (!Listeners.contains(receiver))
            Listeners.add(receiver);
    }
    private void broadcast(LogData log){
        for (LogReceiver l : Listeners)
            l.onLogReceived(log);
    }

    public boolean Debug = false;

    public String LineFormat = "{date} - {type} - {text}";
    public String DateFormat = "HH:mm:ss";

    public List<LogData> Logs = new ArrayList<>();

    public void Log(String message){
        Log(message, TypesOLog.INFO);
    }
    public void Log(String message, TypesOLog messType)
    {
        try
        {
            LogData mess = new LogData();

            mess.Text = message;
            mess.Type = messType;
            mess.Time = new Date(System.currentTimeMillis());

            Logs.add(mess);

            if (messType != TypesOLog.DEBUG || Debug){
                broadcast(mess);
            }
        }
        catch(Exception ex)
        {
            LogData error = new LogData();

            error.Text = ex.toString();
            error.Type = TypesOLog.ERROR;
            error.Time = new Date(System.currentTimeMillis());

            broadcast(error);
        }
    }

    @Override
    public String toString()
    {
        return GetConsole(LineFormat, DateFormat);
    }

    public String GetConsole(String lineFormat, String dateFormat){
        return GetConsole(lineFormat, dateFormat, false);
    }
    public String GetConsole(String lineFormat, String dateFormat, boolean saveFormats)
    {
        if (saveFormats)
        {
            LineFormat = lineFormat;
            DateFormat = dateFormat;
        }
        String result = "";
        Collections.sort(Logs);
        for (LogData l : Logs) result += l.toString(lineFormat, dateFormat) + "\n";
        return result;
    }
}
