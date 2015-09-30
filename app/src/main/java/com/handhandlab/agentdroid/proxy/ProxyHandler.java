package com.handhandlab.agentdroid.proxy;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by Handhand on 2015/7/13.
 */
public interface ProxyHandler {
    public void onAccept(SelectionKey key,ProxyServer proxyServer);
    //called when selector has data to read
    //key is for getting channel and cancel, ProxyServer is for thread to reregister
    public void onRead(SelectionKey key ,ProxyServer proxyServer);
    //write data to channel, mostly called by AgentClient
    public void write(byte[] data);
    public void closeChannel();
    public void onResponse();
    public SocketChannel getChannel();
}
