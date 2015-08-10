package com.handhandlab.agentdroid.deprecate;

import java.nio.channels.SelectionKey;


/**
 * <p>Title: 事件适配</p>
 * @author starboy
 * @version 1.0
 */

public abstract class EventAdapter implements EventListener {
    public EventAdapter() {
    }
    @Override
    public void onError(String error) {
    }
    @Override
    public void onAccept() throws Exception {
    }
    @Override
    public void onAccepted(Request request)  throws Exception {
    }
    @Override
    public void onRead(Request request)  throws Exception {
    }
    @Override
    public void onWrite(Request request, Response response)  throws Exception {
    }
    @Override
    public void onClosed(Request request)  throws Exception{
    }
	@Override
	public void onRead(ConnContext wc,SelectionKey key) throws Exception {}
	@Override
	public void onWrite(ConnContext wc) throws Exception {
	}
    
}
