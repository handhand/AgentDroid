package com.handhandlab.agentdroid.deprecate;

import java.util.List;
import java.util.LinkedList;
import java.nio.channels.SelectionKey;
/**
 * <p>Title: 回应线程</p>
 * <p>Description: 用于向客户端发信</p>
 * @author starboy
 * @version 1.0
 */

public final class Writer extends Thread {
    private static List<SelectionKey> pool = new LinkedList<SelectionKey>();
    private static Notifier notifier = Notifier.getNotifier();

    public Writer() {
    }

    /**
     * SMS发线程主控服务方,负责调度整个处理过程
     */
    public void run() {
        while (true) {
            try {
                SelectionKey key;
                synchronized (pool) {
                    while (pool.isEmpty()) {
                        pool.wait();
                    }
                    key = (SelectionKey) pool.remove(0);
                }

                // 处理写事
                write(key);
            }
            catch (Exception e) {
                continue;
            }
        }
    }

    /**
     * 处理向客户发送数
     * @param key SelectionKey
     */
    public void write(SelectionKey key) {
        /*try {
            SocketChannel sc = (SocketChannel) key.channel();
            Response response = new Response(sc);
            WorkContext wc = (WorkContext)key.attachment();
            wc.resp = response;
            // 触发onWrite事件
            notifier.fireOnWrite(wc);

            // 关闭
            sc.finishConnect();
            sc.socket().close();
            sc.close();

            // 触发onClosed事件
            //notifier.fireOnClosed((WorkContext)key.attachment());
        }
        catch (Exception e) {
        	e.printStackTrace();
            notifier.fireOnError("Error occured in Writer: " + e.getMessage());
        }*/
    }

    /**
     * 处理客户请求,管理用户的联结池,并唤醒队列中的线程进行处�?
     */
    public static void processRequest(SelectionKey key) {
        synchronized (pool) {
            pool.add(pool.size(), key);
            pool.notifyAll();
        }
    }
}

