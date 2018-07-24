package me.blankboy.extensions;

public class COMMANDS {
    Channel Channel;

    public COMMANDS(Channel Channel){
        this.Channel = Channel;
    }

    public void Shutdown(){
        Channel.SendMessage("[COMMAND]SHUTDOWN");
    }

    public void SetMonitor(MonitorState state){
        Channel.SendMessage("[COMMAND]MONITOR_" + String.valueOf(state).toUpperCase());
    }
}
