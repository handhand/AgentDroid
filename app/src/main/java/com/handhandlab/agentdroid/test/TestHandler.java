package com.handhandlab.agentdroid.test;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by Handhand on 2015/7/1.
 */
public class TestHandler {
    ByteArrayOutputStream baos;
    ByteBuffer readBuffer;
    ByteBuffer writeBuffer;
    public TestHandler(){
        baos = new ByteArrayOutputStream();
        readBuffer = ByteBuffer.allocate(5);
        writeBuffer = ByteBuffer.allocate(5);
    }
    public void onRead(final SocketChannel channel) throws IOException{

        /*int n = channel.read(readBuffer);
        if(n>0){
            byte[] tmp = new byte[n];
            readBuffer.flip();
            readBuffer.get(tmp);
            Log.d("haha",new String(tmp));
            readBuffer.clear();

            Thread t = new Thread(){
                @Override
                public void run() {
                    Log.d("haha","write in other thread");
                    writeBuffer.put("hello".getBytes());
                    writeBuffer.flip();
                    try{
                        channel.write(writeBuffer);
                        writeBuffer.clear();
                    }catch(IOException ioe){
                        ioe.printStackTrace();
                    }
                }
            };
            t.start();
        }else if(n == -1){
            channel.close();
        }*/

        int n = 0;
        do{
            n = channel.read(readBuffer);

            if(n==-1){
                channel.close();
                return;
            }

            byte[] tmp = new byte[n];
            readBuffer.flip();
            readBuffer.get(tmp);
            Log.d("haha","server read partly:"+new String(tmp));
            baos.write(tmp);
            readBuffer.clear();

        }while (n>0);

       /* Thread t = new Thread(){
            @Override
            public void run() {
                Log.d("haha","write in other thread");
                writeBuffer.put("hello".getBytes());
                writeBuffer.flip();
                try{
                    channel.write(writeBuffer);
                    writeBuffer.clear();
                }catch(IOException ioe){
                    ioe.printStackTrace();
                }
            }
        };
        t.start();*/
        Thread d = new Thread(){
            @Override
            public void run() {
                try {
                    handleWrite(channel,"really really really really long reply.".getBytes());
                }catch (IOException ioe){
                    ioe.printStackTrace();
                }
            }
        };
        d.start();
    };

    private void handleWrite(SocketChannel channel,byte[] data) throws IOException{
        int writeStart = 0;
        int writeCount = 0;
        if(channel.socket().isClosed())return;
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
            channel.write(writeBuffer);
            writeBuffer.clear();
        }
    }
}
