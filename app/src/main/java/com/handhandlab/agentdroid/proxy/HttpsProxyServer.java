package com.handhandlab.agentdroid.proxy;

import android.content.Context;

import java.io.IOException;

/**
 * Created by Handhand on 2015/9/10.
 */
public class HttpsProxyServer extends ProxyServer{

    public HttpsProxyServer(Context context, int port)throws IOException{
        super(context, port);
    }

    @Override
    public ProxyHandler getProxyHandler() {
        return new HttpsProxyHandler(mContext);
    }
}
