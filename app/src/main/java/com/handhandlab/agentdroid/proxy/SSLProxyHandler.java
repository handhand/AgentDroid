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
 * Handler to do HTTPS proxy
 * One channel per handler
 */
public class SSLProxyHandler implements ProxyHandler{
    //handler id for debug purpose
    static int id = 0;
    public int handlerId;

    //buffer size for read and write
    private static final int BUFFER_SIZE = 1024 * 10;

    Context context;

    //read buffer per channel
    ByteBuffer readBuffer;

    //write buffer per channel
    ByteBuffer writeBuffer;

    //corresponding channel
    SocketChannel channel;

    //object that do all the ssl stuff.
    //it will be initialized when receiving a http CONNECT request.
    SSLEngineHelper ssl;

    public SSLProxyHandler(Context context) {
        //init buffer
        readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.context = context;
        handlerId = id++;
    }

    /**
     * handle read events
     * @param key
     * @param channel
     */
    @Override
    public void onRead(SelectionKey key, final SocketChannel channel){

        /***** SSLEngineHelper object is not null, meaning the ssl tunnel has been established ******/
        if(ssl!=null){
            try{
                String decodedData =  ssl.sslRead(key,channel);//let sslEngine do the reading and decrypting
                if(decodedData!=null){
                    //decode data to an http request
                    HandHttpRequest request = new HandHttpRequest(decodedData);

                    //go agent!
                    AgentClient3.goAgent(new AgentClient3.AgentTask(request,this));

                }else {//read nothing, close the channel
                    Log.d("haha","client close channel:"+handlerId);
                    key.cancel();
                    channel.close();
                }
            }catch(IOException e){
                e.printStackTrace();
            }
            return;
        }

        /******  ssl is NOT initiated yet ******/
        this.channel = channel;

        //read plain text, it SHOULD be a CONNECT request
        byte[] inData = read(key, channel);

        if(inData==null){
            //client closes the channel
            return;
        }

        //decode to http request
        HandHttpRequest request = new HandHttpRequest(new String(inData));

        if(request.method.toLowerCase().equals("connect")){
            Log.d("haha","SSL CONNECT REQUEST:"+handlerId);

            //unregister read interests, since handshakes are done in other thread and do not need selector
            key.cancel();
            //do handshake in other thread, we do not want the handshake's long running task to block the main io loop
            Runnable initSSL = new Runnable(){
                @Override
                public void run() {
                    try {
                        Log.d("haha","init ssl:"+handlerId);
                        //response a plain text 200 to start a tunnel setup, according to the protocol
                        write(HTTP.RESPONSE_200.getBytes());
                        /**
                         * NOTE: the following process is actually a Man In The Middle attack,
                         * as client think it's handshaking with the destination server,
                         * but in fact it's handshaking with us
                         */
                        //init ssl engine, "fake" certificate will be generated.
                        //ideally we should pass the actual host name get from CONNECT request
                        // but RedSocks only provides us with ip address, so we use predefined SANs to get around this problem.
                        ssl = new SSLEngineHelper(context,"http://www.google.com");
                        Log.d("haha","handshake:"+handlerId);
                        //when client receive the 200 response, the handshake shall begin
                        ssl.doHandShake(channel);
                        //after handshake, re-register to do request handling in main io loop
                        ServerSSL.regesterRead(channel,SSLProxyHandler.this);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            Thread t = new Thread(initSSL);
            t.start();
            return;
        }
    }

    /**
     * read data from channel
     * use this method to read the first CONNECT request
     * @param key
     * @param channel
     * @return bytes from the channel; NULL if the read event is client closing the channel
     */
    private byte[] read(SelectionKey key, SocketChannel channel) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int r;//bytes read
            while (true) {//read until we've got nothing to read
                readBuffer.clear();
                r = channel.read(readBuffer);
                if (r == 0) break;//nothing to read, but channel is still open
                if (r == -1) {
                    //-1 means client closes the connection
                    Log.d("haha read", "client close connection! id:"+handlerId);
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
    public void write(byte[] data){
        //use ssl
        if(ssl!=null){
            try{
                ssl.write(channel,data);
            }catch (IOException e){
                e.printStackTrace();
            }
            return;
        }
        //no ssl, write plain text;
        if(channel.socket().isClosed()){
            Log.d("haha","socket is CLOSED when try to write! id:"+handlerId);
            return;
        }
        int writeStart = 0;//mark write position of the byte array
        int writeCount = 0;
        while(true){//data might be bigger than our write buffer
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
            //ssl.doSSLClose(channel);
            channel.socket().close();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    @Override
    public void onResponse() {
        //do nothing
    }
}
