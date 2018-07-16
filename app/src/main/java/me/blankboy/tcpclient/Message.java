package me.blankboy.tcpclient;

import java.util.Date;

public class Message{
    public Message(String Text, Date Time){
        this.Text = Text;
        this.Time = Time;
    }
    public String Text;
    public Date Time;
    @Override
    public String toString(){
        return Text;
    }
}