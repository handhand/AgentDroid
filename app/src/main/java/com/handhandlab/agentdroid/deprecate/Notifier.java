package com.handhandlab.agentdroid.deprecate;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;

/**
 * <p>Title: 事件触发</p>
 * @version 1.0
 */
public class Notifier {
    private static ArrayList<EventListener> listeners = null;
    private static Notifier instance = null;

    private Notifier() {
        listeners = new ArrayList<EventListener>();
    }

    /**
     * 获取事件触发
     * @return 返回事件触发
     */
    public static synchronized Notifier getNotifier() {
        if (instance == null) {
            instance = new Notifier();
            return instance;
        }
        else return instance;
    }

    /**
     * 添加事件监听
     */
    public void addListener(EventListener l) {
        synchronized (listeners) {
            if (!listeners.contains(l))
                listeners.add(l);
        }
    }

    public void fireOnAccept() throws Exception {
        for (int i = listeners.size() - 1; i >= 0; i--)
            ( (EventListener) listeners.get(i)).onAccept();
    }

    public void fireOnAccepted(Request request) throws Exception {
        for (int i = listeners.size() - 1; i >= 0; i--)
            ( (EventListener) listeners.get(i)).onAccepted(request);
    }

    void fireOnRead(Request request) throws Exception {
        for (int i = listeners.size() - 1; i >= 0; i--)
            ( (EventListener) listeners.get(i)).onRead(request);

    }
    
    void fireOnRead(ConnContext wc,SelectionKey key) throws Exception {
        for (int i = listeners.size() - 1; i >= 0; i--)
            ( (EventListener) listeners.get(i)).onRead(wc,key);

    }

    void fireOnWrite(Request request, Response response)  throws Exception  {
        for (int i = listeners.size() - 1; i >= 0; i--)
            ( (EventListener) listeners.get(i)).onWrite(request, response);

    }
    
    void fireOnWrite(ConnContext wc)  throws Exception  {
        for (int i = listeners.size() - 1; i >= 0; i--)
            ( (EventListener) listeners.get(i)).onWrite(wc);

    }

    public void fireOnClosed(Request request) throws Exception {
        for (int i = listeners.size() - 1; i >= 0; i--)
            ( (EventListener) listeners.get(i)).onClosed(request);
    }

    public void fireOnError(String error) {
        for (int i = listeners.size() - 1; i >= 0; i--)
            ( (EventListener) listeners.get(i)).onError(error);
    }
}
