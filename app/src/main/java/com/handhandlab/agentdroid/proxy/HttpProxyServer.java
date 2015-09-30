package com.handhandlab.agentdroid.proxy;

import android.content.Context;

import java.io.IOException;

/**
 * Created by Handhand on 2015/9/10.
 */
public class HttpProxyServer extends ProxyServer {

    public HttpProxyServer(Context context, int port) throws IOException {
        super(context, port);
    }

    @Override
    public ProxyHandler getProxyHandler() {
        return new HttpProxyHandler(mContext);
    }
}
