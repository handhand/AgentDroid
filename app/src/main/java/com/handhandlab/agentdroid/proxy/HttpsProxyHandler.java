package com.handhandlab.agentdroid.proxy;

import android.content.Context;
import android.util.Log;

import com.handhandlab.agentdroid.goagent.AgentClient3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Handler to do HTTPS proxy
 * One channel per handler
 *
 * AgentDroid won't actually know which host the client is connecting to when the ssl connection is initiating,
 * it uses Subject Alternative Names in the certificate to work around this.
 * Users should preset all the secure sites' domain she might likely to visit, agentdroid will set all these domain in
 * the certificate's subject alternative name field;
 */
public class HttpsProxyHandler implements ProxyHandler {
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

    public HttpsProxyHandler(Context context) {
        //init buffer
        readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.context = context;
        handlerId = id++;
    }

    @Override
    public void onAccept(SelectionKey key, ProxyServer proxyServer) {

    }

    /**
     * @param key
     * @param proxyServer
     */
    @Override
    public void onRead(SelectionKey key, final ProxyServer proxyServer) {

        /***** SSLEngineHelper object is not null, meaning the ssl tunnel has been established ******/
        if (ssl != null) {
            try {
                String decodedData = ssl.sslRead(key, channel);//let sslEngine do the reading and decrypting
                if (decodedData != null) {
                    //decode data to an http request
                    HandyHttpRequest request = new HandyHttpRequest(decodedData);

                    //go agent!
                    AgentClient3.goAgent(new AgentClient3.AgentTask(request, this));

                } else {//read nothing, close the channel
                    Log.d("haha", "client close channel:" + handlerId);
                    key.cancel();
                    channel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        Log.d("haha","---a ssl connection init----");

        /******  ssl is NOT initiated yet ******/
        this.channel = (SocketChannel) key.channel();

        //unregister read interests, since handshakes are done in other thread and do not need selector
        key.cancel();

        //do handshake in other thread, we do not want the handshake's long running task to block the main io loop
        Runnable initSSL = new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("haha", "init ssl:" + handlerId);

                    /**
                     * NOTE: the following process is actually a Man In The Middle attack,
                     * as client think it's handshaking with the destination server,
                     * but in fact it's handshaking with us
                     */
                    //init ssl engine, "fake" certificate will be generated.
                    //ideally we should pass the actual host name get from CONNECT request
                    // but RedSocks only provides us with ip address, so we use predefined SANs to get around this problem.
                    ssl = new SSLEngineHelper(context, "http://www.google.com");
                    Log.d("haha", "handshake:" + handlerId);
                    //when client receive the 200 response, the handshake shall begin
                    ssl.doHandShake(channel);
                    //after handshake, re-register to do request handling in main io loop
                    proxyServer.regesterRead(channel, HttpsProxyHandler.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread t = new Thread(initSSL);
        t.start();
        return;

    }

    /**
     * write data to channel
     * because we have SSLEngine here, we should use it to encode our data,
     * writing is actually done by SSLEngine
     *
     * @param data
     */
    @Override
    public void write(byte[] data) {
        //use ssl
        if (ssl != null) {
            try {
                ssl.write(channel, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        //no ssl, write plain text;
        if (channel.socket().isClosed()) {
            Log.d("haha", "socket is CLOSED when try to write! id:" + handlerId);
            return;
        }
        int writeStart = 0;//mark write position of the byte array
        int writeCount = 0;
        while (true) {//data might be bigger than our write buffer
            writeStart = writeStart + writeCount;
            int remaining = data.length - writeStart;
            if (remaining <= 0) break;
            if (remaining > writeBuffer.capacity())
                writeCount = writeBuffer.capacity();
            else
                writeCount = remaining;
            writeBuffer.put(data, writeStart, writeCount);
            writeBuffer.flip();
            try {
                channel.write(writeBuffer);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                break;//if write throws exception, chances are it can't be written any more
            } finally {
                writeBuffer.clear();
            }
        }
    }

    @Override
    public void closeChannel() {
        try {
            //ssl.doSSLClose(channel);
            channel.socket().close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public void onResponse() {
        //do nothing
    }

    @Override
    public SocketChannel getChannel() {
        return channel;
    }
}
