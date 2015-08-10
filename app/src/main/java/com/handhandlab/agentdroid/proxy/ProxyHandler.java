package com.handhandlab.agentdroid.proxy;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by Handhand on 2015/7/13.
 */
public interface ProxyHandler {
    public void onRead(SelectionKey key, final SocketChannel channel);
    public void write(byte[] data);
    public void closeChannel();
    public void onResponse();
}
