package com.handhandlab.agentdroid.deprecate;

import android.content.Context;
import android.util.Log;

import com.handhandlab.agentdroid.deprecate.AgentClient2;
import com.handhandlab.agentdroid.proxy.HttpProxyHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>
 * Title: 主控服务线程
 * </p>
 *
 * @author starboy
 * @version 1.0
 */

public class Server2 implements Runnable {
    private Context mContext;
    private static Selector selector;
    private ServerSocketChannel sschannel;
    private InetSocketAddress address;
    private int port;

    /**
     * 创建主控服务线程
     */

    public Server2(Context context, int port) throws IOException {
        this.port = port;
        mContext = context;
        // 创建无阻塞网络
        selector = Selector.open();
        sschannel = ServerSocketChannel.open();
        sschannel.configureBlocking(false);
        address = new InetSocketAddress(port);
        ServerSocket ss = sschannel.socket();
        ss.bind(address);
        sschannel.register(selector, SelectionKey.OP_ACCEPT);
        AgentClient2.start(4, "http://handhand.eu5.org/goa/agentdroid.php", "123456");
    }

    public void run() {
        // 监听
        while (true) {
            try {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectedKeys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    // 处理IO事件
                    if (key.isAcceptable()) {
                        // Accept the new connection
                        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = ssc.accept();
                        clientChannel.configureBlocking(false);
                        // 注册读操作
                        clientChannel.register(selector, SelectionKey.OP_READ, new HttpProxyHandler(mContext));
                    } else if (key.isReadable()) {
                        HttpProxyHandler handler = (HttpProxyHandler) key.attachment();
                        Log.d("haha server", "selector get new events READ! id:"+handler.handlerId);
                        //handler.onRead(key, (SocketChannel) key.channel());
                    } else if ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                        Log.d("haha server", "selector get new events WRITE!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

}