package me.blankboy.extensions;

public interface ChannelListener {
    void onMessageReceived(Message message, Channel sender);
    void onException(Exception ex, Channel sender);
}
