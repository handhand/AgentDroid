package com.handhandlab.agentdroid.proxy;

import android.content.Context;

/**
 * Created by Handhand on 2015/7/17.
 */
public class HttpHandlerFactory implements HandlerFactory {

    Context context;

    public HttpHandlerFactory(Context context){
        this.context = context;
    }

    @Override
    public ProxyHandler getHandler() {
        return new HttpProxyHandler(context);
    }
}
