package me.blankboy.tcpclientv2;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log implements Comparable<Log> {
    public LogType Type;
    public String Text;
    public Date Time;

    public static final String Format = "{date} - {type} - {text}";
    public static final String DateFormat = "HH:mm:ss";

    @Override
    public int compareTo(Log other) {
        if (other.Time == null) return 1;
        if (Time == null) return -1;
        if (other.Time == Time) return 0;
        return other.Time.before(Time) ? 1 : -1;
    }

    @Override
    public String toString() {
        return toString(Format.toString(), DateFormat.toString());
    }

    public String toString(String format, String dateFormat){
        SimpleDateFormat df =  new SimpleDateFormat(dateFormat);
        return format.replace("{date}", df.format(Time)).replace("{type}", (Type != LogType.NULL ? Type.toString() : "")).replace("{text}", Text);
    }
}