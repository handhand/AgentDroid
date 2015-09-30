package com.handhandlab.agentdroid.deprecate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.util.Log;

/**
 * <p>
 * Title: 主控服务线程
 * </p>
 *
 * @author starboy
 * @version 1.0
 */

public class Server implements Runnable {
    // using for re-register key, it seems register MUST do in the same thread with selector
	private static List<ConnContext> readPool = new LinkedList<ConnContext>();
    private static List<ConnContext> writePool = new LinkedList<ConnContext>();
	private static Selector selector;
	private ServerSocketChannel sschannel;
	private InetSocketAddress address;
	protected Notifier notifier;
	private int port;

	/**
	 * 创建主控服务线程
	 */

	public Server(Context context,int port) throws IOException {
		this.port = port;
		// 获取事件触发
		notifier = Notifier.getNotifier();
		// 创建无阻塞网络
		selector = Selector.open();
		sschannel = ServerSocketChannel.open();
		sschannel.configureBlocking(false);
		address = new InetSocketAddress(port);
		ServerSocket ss = sschannel.socket();
		ss.bind(address);
		sschannel.register(selector, SelectionKey.OP_ACCEPT);
		EventListener listener = null;//new SimpleProxyHandler(context);
		Notifier.getNotifier().addListener(listener);
        AgentClient2.start(4,"http://handhand.eu5.org/goa/agentdroid.php","123456");
	}

	public void run() {
		Log.d("haha", "server start listening");
		// 监听
		while (true) {
			try {
				int num = selector.select();
				if (num > 0) {
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> it = selectedKeys.iterator();
					while (it.hasNext()) {
						SelectionKey key = it.next();
						it.remove();
						// 处理IO事件
						if (key.isAcceptable()) {
							Log.d("haha server", "selector get new events ACCEPT!");
							// Accept the new connection
							ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
							SocketChannel clientChannel = ssc.accept();
							clientChannel.configureBlocking(false);
							// 注册读操作
							clientChannel.register(selector, SelectionKey.OP_READ);
						} else if (key.isReadable()) {
							Log.d("haha server", "selector get new events READ!");
							handleRead(key);

							//Reader.processRequest(key); // 提交读服务线程读取客户端数据
							//key.cancel();
						} else if ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                            Log.d("haha server", "selector get new events WRITE!");
							handleWrite(key);
                            key.cancel();
							//Writer.processRequest(key); // 提交写服务线程向客户端发送回应数据
							//key.cancel();
						}
					}
				} else {
					addRegister(); // 在Selector中注册新的通道
				}
			} catch (Exception e) {
				notifier.fireOnError("Error occured in Server: "+ e.getMessage());
				e.printStackTrace();
			}
		}
	}

    /**
     * 处理读事件
     * 第一次连接时添加ConnContext
     * 数据会读入ConnContext的inData中，交由Handler处理
     * @param key
     */
	private void handleRead(SelectionKey key){
		 //获取连接相关信息（ConnContext）
        SocketChannel sc = (SocketChannel) key.channel();
        Object o = key.attachment();
        ConnContext connContext;
        //表示这是一个新连接
        if(o==null){
        	connContext = new ConnContext();
        	connContext.channel = sc;
            //将ConnContext保存，以后可从key中获取
            key.attach(connContext);
        }
        //这是一个已经建立的连接
        else{
        	connContext = (ConnContext)o;
        }
        //读取数据
        try {
        	if(connContext.readToData()==null)return;//close collection and return
            //Log.d("haha server","read:"+new String(connContext.inData));
			notifier.fireOnRead(connContext,key);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

    private void handleWrite(SelectionKey key) throws IOException{
        //获取连接相关信息（ConnContext）
        ConnContext cc = (ConnContext)key.attachment();
        int writeStart = 0;
        int writeEnd = 0;
        if(cc.channel.socket().isClosed())return;
        Log.d("haha "+cc.request.getHost(),"write to channel:"+cc.outData.length);
        while(true){
            cc.writeBuffer.clear();
            writeStart = writeEnd;
            int remaining = cc.outData.length - writeStart;
            if(remaining <= 0) break;
            if(remaining>cc.writeBuffer.capacity())
                writeEnd = writeStart + cc.writeBuffer.capacity();
            else
                writeEnd = writeStart + remaining;
            cc.writeBuffer.put(cc.outData, writeStart, writeEnd);
            cc.writeBuffer.flip();
            cc.channel.write(cc.writeBuffer);
        }
        cc.outData = null;
    }

	/**
	 * 添加新的通道注册
	 */
	private void addRegister() {
		synchronized (readPool) {
			while (!readPool.isEmpty()) {
				ConnContext connCtx = readPool.remove(0);
				SocketChannel schannel = connCtx.channel;
				try {
					schannel.register(selector, SelectionKey.OP_READ,connCtx);
				} catch (IOException e) {
                    e.printStackTrace();;
					try {
						schannel.finishConnect();
						schannel.close();
						schannel.socket().close();
					} catch (IOException e1) {
                        e1.printStackTrace();
					}
				}
			}
		}
        synchronized (writePool) {
            while (!writePool.isEmpty()) {
                ConnContext connCtx =writePool.remove(0);
                SocketChannel schannel = connCtx.channel;
                try {
                    schannel.register(selector, SelectionKey.OP_WRITE,connCtx);
                } catch (IOException e) {
                    e.printStackTrace();;
                    try {
                        schannel.finishConnect();
                        schannel.close();
                        schannel.socket().close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
	}

	/**
	 * 提交新的客户端写请求于主服务线程的回应池�?
	 */
	public static void registerWrite(ConnContext cc) {
		synchronized (writePool) {
			writePool.add(writePool.size(), cc);
			writePool.notifyAll();
		}
		selector.wakeup(); // 解除selector的阻塞状态，以便注册新的通道
	}

	public static void registerRead(ConnContext cc){
        synchronized (readPool) {
            readPool.add(readPool.size(), cc);
            readPool.notifyAll();
        }
        selector.wakeup(); // 解除selector的阻塞状态，以便注册新的通道
	}
}