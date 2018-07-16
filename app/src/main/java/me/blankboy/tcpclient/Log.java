package me.blankboy.tcpclient;

import java.util.Date;

public class Log{
    public Log(String Text, LogType Type, Date Time){
        this.Text = Text;
        this.Type = Type;
        this.Time = Time;
    }
    public Log(String Text, LogType Type){
        this.Text = Text;
        this.Type = Type;
        this.Time = new Date(System.currentTimeMillis());
    }
    public String Text;
    public Date Time;
    public LogType Type;
    @Override
    public String toString()
    {
        String format = "{date} - {type} - {text}";
        String dateFormat = "HH:mm:ss";
        // You have to convert from milliseconds to secs and mins and hours
        // return format.replace("{date}", Time.toString(dateFormat)).replace("{type}", Type.toString()).replace("{text}", Text);
        return Text;
    }
}
