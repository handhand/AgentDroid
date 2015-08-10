package com.handhandlab.agentdroid.ui;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.handhandlab.agentdroid.goagent.AgentClient3;
import com.handhandlab.agentdroid.proxy.HttpHandlerFactory;
import com.handhandlab.agentdroid.proxy.ProxyServer;
import com.handhandlab.agentdroid.system.InitScript;

import java.io.IOException;

/**
 * Created by Handhand on 2015/7/17.
 */
public class ProxyService extends Service {

    public static final String EXTRA_MODE = "mode";

    public static final int MODE_HTTP = 0;
    public static final int MODE_HTTPS = 1;
    public static final int MODE_ALL = 2;

    ProxyServer proxyServer;
    Thread serverThread;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int mode = intent.getIntExtra(EXTRA_MODE,0);
        switch (mode){
            case MODE_HTTP:
                startProxy();
                break;
            case MODE_HTTPS:
                break;
            case MODE_ALL:
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        int mode = intent.getIntExtra(EXTRA_MODE,0);
        switch (mode){
            case MODE_HTTP:
                startProxy();
                break;
            case MODE_HTTPS:
                break;
            case MODE_ALL:
                break;
        }
        return new ProxyServiceBinder();
    }

    private void startProxy(){
        try{
            //AgentClient3.start(6,"http://handhand.eu5.org/goa/agentdroid.php", "123456");
            AgentClient3.start(6,"http://handhand.6te.net/agentdroid/index.php","123456");
            InitScript.startHTTPIntercept(getApplicationContext(),"www.eu5.com");
            proxyServer = new ProxyServer(getApplicationContext(),8888,new HttpHandlerFactory(getApplicationContext()));
            serverThread = new Thread(proxyServer);
            serverThread.start();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        proxyServer.stop();
        serverThread.interrupt();
        InitScript.stopIntercept();
        super.onDestroy();
    }

    public class ProxyServiceBinder extends Binder{
        public ProxyService getService(){
            return ProxyService.this;
        }
    }
}
