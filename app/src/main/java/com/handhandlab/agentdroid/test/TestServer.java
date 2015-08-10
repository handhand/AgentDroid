package com.handhandlab.agentdroid.test;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by Handhand on 2015/7/1.
 */
public class TestServer implements Runnable {

    int port = 8080;
    TestHandler handler;

    public TestServer(int port,TestHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    @Override
    public void run() {
        Selector selector;
        try{
            selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            ServerSocket ss = serverChannel.socket();
            ss.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            SelectionKey serverKey = serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        }catch(IOException ioe){
            ioe.printStackTrace();
            return;
        }


        while (true) {
            try {
                selector.select();
            }catch (IOException ioe){
                ioe.printStackTrace();
                return;
            }

            for (Iterator<SelectionKey> itor = selector.selectedKeys().iterator(); itor.hasNext(); ) {
                SelectionKey key = (SelectionKey) itor.next();
                itor.remove();
                try {
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
                        clientKey.attach(new TestHandler());
                    } else if (key.isReadable()) {
                        Log.d("haha","readable!");
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        TestHandler handler = (TestHandler) key.attachment();
                        handler.onRead(socketChannel);
                    } else if (key.isWritable()) {
                        Log.d("haha","writable!");
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        client.write(buffer);
                        if (buffer.remaining() == 0) {  // write finished, switch to OP_READ
                            buffer.clear();
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    }
                } catch (IOException e) {
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        }
    }
}
