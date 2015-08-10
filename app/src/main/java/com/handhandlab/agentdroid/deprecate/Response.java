package com.handhandlab.agentdroid.deprecate;

import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

import android.util.Log;

/**
 */

public class Response {
    private SocketChannel sc;

    public Response(SocketChannel sc) {
        this.sc = sc;
    }

    /**
     */
    public void send(byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.put(data, 0, data.length);
        buffer.flip();
        Log.d("haha", "write "+buffer.array().length);
        sc.write(buffer);
    }
}
