package com.handhandlab.agentdroid.proxy;

import android.content.Context;
import android.util.Log;

import com.handhandlab.agentdroid.goagent.AgentClient2;
import com.handhandlab.agentdroid.goagent.AgentClient3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 *
 */
public class HttpProxyHandler implements ProxyHandler{
    static int id = 0;
    public int handlerId;
    private static final int BUFFER_SIZE = 1024 * 10;
    Context context;
    ByteBuffer readBuffer;
    ByteBuffer writeBuffer;
    SocketChannel channel;
    //tmp
    HandHttpRequest request;

    public HttpProxyHandler(Context context) {
        readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.context = context;
        handlerId = id++;
    }

    @Override
    public void onRead(SelectionKey key, SocketChannel channel){

        this.channel = channel;

        byte[] inData = read(key, channel);

        if(inData==null){
            //channel close
            return;
        }

        HandHttpRequest request = new HandHttpRequest(new String(inData));
        this.request = request;

        AgentClient3.goAgent(new AgentClient3.AgentTask(request, this));

    }

    private byte[] read(SelectionKey key, SocketChannel channel) {
        int r = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            while (true) {
                readBuffer.clear();
                r = channel.read(readBuffer);
                if (r == 0) break;//nothing to read, but channel is still open
                if (r == -1) {
                    //-1 means counterpart close the connection
                    Log.d("haha read", "TEST TEST:client close connection! id:"+handlerId);
                    channel.close();
                    key.cancel();
                    return null;
                }
                readBuffer.flip();
                byte[] bytes = new byte[readBuffer.remaining()];
                readBuffer.get(bytes);
                baos.write(bytes);
            }
            return baos.toByteArray();
        }catch (IOException ioe){
            ioe.printStackTrace();
            key.cancel();
        }
        return null;
    }

    /**
     * synchronized because multithreading would mess up the bytebuffer, wouldn't it?
     * @param data
     */
    @Override
    public synchronized void write(byte[] data){
        //Log.d("haha","write to client id:"+handlerId);
        int writeStart = 0;
        int writeCount = 0;
        if(channel.socket().isClosed()){
            Log.d("haha","socket is CLOSED when try to write! id:"+handlerId);
            return;
        }
        while(true){
            writeStart = writeStart + writeCount;
            int remaining = data.length - writeStart;
            if(remaining <= 0) break;
            if(remaining>writeBuffer.capacity())
                writeCount = writeBuffer.capacity();
            else
                writeCount = remaining;
            writeBuffer.put(data, writeStart, writeCount);
            writeBuffer.flip();
            try{
                channel.write(writeBuffer);
            }catch(IOException ioe){
                ioe.printStackTrace();
                break;//if write throws exception, chances are it can't be written any more
            }finally {
                writeBuffer.clear();
            }
        }
    }

    @Override
    public void closeChannel(){
        try{
            channel.socket().close();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    /**
     * TEST: close client channel after deliver response
     */
    @Override
    public void onResponse() {
        closeChannel();
    }
}
