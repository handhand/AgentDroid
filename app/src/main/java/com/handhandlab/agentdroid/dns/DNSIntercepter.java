package com.handhandlab.agentdroid.dns;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Handhand on 2015/9/11.
 */
public class DNSIntercepter implements Runnable{

    @Override
    public void run(){
        try
        {
            DatagramSocket server=new DatagramSocket(18890);
            while(true)
            {
                byte[] sendbyte=new byte[1024];
                byte[] receivebyte=new byte[1024];
                DatagramPacket receiver=new DatagramPacket(receivebyte,receivebyte.length);
                server.receive(receiver);
                Log.d("haha", "destin:" +receiver.getLength());

                DatagramSocket serverSocket = new DatagramSocket();
                DatagramPacket request = new DatagramPacket(receiver.getData(),receiver.getLength(),InetAddress.getByName("114.114.114.114"),53);
                serverSocket.send(request);
                DatagramPacket response=new DatagramPacket(sendbyte,sendbyte.length);
                serverSocket.receive(response);
                Log.d("haha","return:"+new String(response.getData(),0,response.getLength()));
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
