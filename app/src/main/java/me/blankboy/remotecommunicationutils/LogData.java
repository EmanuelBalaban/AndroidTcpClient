package me.blankboy.remotecommunicationutils;

import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogData implements Comparable<LogData> {
    // This message category type.
    public TypesOLog Type;

    // Message itself.
    public String Text;

    // Sent time.
    public Date Time;

    @Override
    public int compareTo(@NonNull LogData other) {
        if (other.Time == null) return 1;
        if (Time == null) return -1;
        if (other.Time == Time) return 0;
        return other.Time.before(Time) ? 1 : -1;
    }

    public static final String DefaultFormat = "{date} - {type} - {text}";
    public static final String DefaultDateFormat = "HH:mm:ss";

    @Override
    public String toString() {
        return toString(DefaultFormat.toString(), DefaultDateFormat.toString());
    }

    public String toString(String format, String dateFormat){
        SimpleDateFormat df =  new SimpleDateFormat(dateFormat);
        return format.replace("{date}", df.format(Time)).replace("{type}", (Type != TypesOLog.NULL ? Type.toString() : "")).replace("{text}", Text);
    }
}
