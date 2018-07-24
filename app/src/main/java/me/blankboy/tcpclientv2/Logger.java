package me.blankboy.tcpclientv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Logger{
    public boolean Debug = false;

    public String LineFormat = "{date} - {type} - {text}";
    public String DateFormat = "HH:mm:ss";

    public List<Log> Logs = new ArrayList<>();
    private List<LogReceiver> Listeners = new ArrayList<>();

    public void addListener(LogReceiver receiver){
        if (!Listeners.contains(receiver))
            Listeners.add(receiver);
    }
    private void broadcast(Log log){
        for (LogReceiver lr : Listeners)
            lr.onLogReceived(log);
    }

    public void Log(String message){
        Log(message, LogType.INFO);
    }
    public void Log(String message, LogType messType)
    {
        try
        {
            Log mess = new Log();

            mess.Text = message;
            mess.Type = messType;
            mess.Time = new Date(System.currentTimeMillis());

            Logs.add(mess);

            if (messType != LogType.DEBUG || Debug){
                broadcast(mess);
            }
        }
        catch(Exception ex)
        {
            Log error = new Log();

            error.Text = ex.toString();
            error.Type = LogType.ERROR;
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
        for (Log l : Logs) result += l.toString(lineFormat, dateFormat) + "\n";
        return result;
    }
}
