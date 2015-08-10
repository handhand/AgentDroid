package com.handhandlab.agentdroid.proxy;

import android.content.Context;
import android.util.Log;

import com.handhandlab.agentdroid.goagent.AgentClient2;
import com.handhandlab.agentdroid.goagent.AgentClient3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * HTTPS Proxy Server
 * work with Iptables and Redsocks in Android
 *
 *
 * process:
 * 1, Iptables will redirect Https connection to Redsocks
 * 2, Redsocks then changes the connection to a CONNECT request, and redirect the request to our ServerSSL
 * NOTE: can NOT use Iptables to redirect the connection to our app directly, as we need to know where the client is connecting to first.
 */

public class ServerSSL implements Runnable {
    private Context mContext;

    //nio selector
    private static Selector selector;

    private ServerSocketChannel sschannel;

    //listening address
    private InetSocketAddress address;

    //listening port
    private int port;

    //hold the re-register requests
    private static List<SSLProxyHandler> registerList = new ArrayList<SSLProxyHandler>();

    //control the main loop
    boolean isRunning = true;

    /**
     * @param context Android context
     * @param port listening port
     * @throws IOException
     */
    public ServerSSL(Context context, int port) throws IOException {
        this.port = port;
        mContext = context;
        // setup non blocking io
        selector = Selector.open();
        sschannel = ServerSocketChannel.open();
        sschannel.configureBlocking(false);
        address = new InetSocketAddress(port);
        ServerSocket ss = sschannel.socket();
        ss.bind(address);
        sschannel.register(selector, SelectionKey.OP_ACCEPT);
        //TODO:This should move to outer place
        //start the thread pool to send requests to goagent server
        AgentClient3.start(8, "http://handhand.eu5.org/goa/agentdroid.php", "123456");
    }

    /**
     * Main loop
     */
    public void run() {

        while (isRunning) {
            try {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectedKeys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    // handle io events
                    if (key.isAcceptable()) {
                        // accept the new connection
                        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = ssc.accept();
                        clientChannel.configureBlocking(false);
                        Log.d("haha","get new accept");
                        // register read interests after accepting
                        //the handler object will be kept with the key
                        clientChannel.register(selector, SelectionKey.OP_READ, new SSLProxyHandler(mContext));
                    } else if (key.isReadable()) {
                        //handle read events
                        SSLProxyHandler handler = (SSLProxyHandler) key.attachment();
                        Log.d("haha server", "selector get new events READ! id:"+handler.handlerId);
                        //let handler handle the read events
                        handler.onRead(key, (SocketChannel) key.channel());
                    } else if ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                        //NOTE: Channels do NOT necessarily need to register "write interests" to be writable
                        Log.d("haha server", "selector get new events WRITE!");
                    }
                }
                reRegisterReads();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * if a channel cancels its key (i.e. selector won't get any events), use this method to re-register read interests.
     * The registering must(it seems) be done in the same thread of the selector;
     * Handlers are add to the list, and re-registered in the main loop
     *
     * @param channel channel needs registering
     * @param handler handler of channel events
     */
    public static void regesterRead(Channel channel,SSLProxyHandler handler){
        synchronized (registerList){
            Log.d("haha","request re-register:"+handler.handlerId);
            registerList.add(handler);
            registerList.notifyAll();
        }
        selector.wakeup();
    }

    /**
     * go through the pool and register the channels and handlers for reading
     */
    private void reRegisterReads(){
        synchronized (registerList){
            while(registerList.size()>0){
                SSLProxyHandler handler = registerList.remove(0);
                try{
                    Log.d("haha","do re-register:"+handler.handlerId);
                    handler.channel.register(selector,SelectionKey.OP_READ,handler);
                }catch (ClosedChannelException e){
                    e.printStackTrace();
                }
            }
        }
    }
}