package me.blankboy.remotecommunicationutils;

public interface ChannelListener {
    void onDataReceived(DataPackage data, Chunk chunk, AdvancedChannel sender);
    void onExceptionOccurred(Exception ex, AdvancedChannel sender);
}
