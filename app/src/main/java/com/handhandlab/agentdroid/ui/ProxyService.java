package com.handhandlab.agentdroid.ui;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.handhandlab.agentdroid.goagent.AgentClient3;
import com.handhandlab.agentdroid.proxy.HttpProxyServer;
import com.handhandlab.agentdroid.proxy.HttpsProxyServer;
import com.handhandlab.agentdroid.proxy.ProxyServer;
import com.handhandlab.agentdroid.system.InitScript;
import com.handhandlab.agentdroid.utils.DataUtils;

import java.io.IOException;

/**
 * Created by Handhand on 2015/7/17.
 */
public class ProxyService extends Service {

    public static final String EXTRA_MODE = "mode";

    public static final int MODE_HTTP = 0;
    public static final int MODE_HTTPS = 1;
    public static final int MODE_ALL = 2;

    int mId = 0xa23;

    ProxyServer mHttpProxy;
    ProxyServer mSSLProxy;
    Thread mHttpThread;
    Thread mSslThread;

    public static boolean isRunning;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int mode = intent.getIntExtra(EXTRA_MODE,0);
        showNotification();
        switch (mode){
            case MODE_HTTP:
                startHttpProxy();
                break;
            case MODE_HTTPS:
                startHttpsProxy();
                break;
            case MODE_ALL:
                startAllProxy();
                break;
        }
        isRunning = true;
        return Service.START_REDELIVER_INTENT;
        //return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if(isRunning)
            return new ProxyServiceBinder();
        else
            return null;
    }

    /**
     * start proxy thread
     */
    private void startHttpProxy(){
        try{
            String serverUrl = DataUtils.getUrl(this);
            //AgentClient3.start(6,"http://handhand.eu5.org/goa/agentdroid.php", "123456");
            AgentClient3.start(4,serverUrl,"123456");//default password of GoAgent, set in the php file
            InitScript.startHTTPIntercept(getApplicationContext(),serverUrl);
            mHttpProxy = new HttpProxyServer(getApplicationContext(),18888);
            mHttpThread = new Thread(mHttpProxy);
            mHttpThread.start();
        }

        catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    /**
     * start https proxy thread
     */
    private void startHttpsProxy(){
        try{
            String serverUrl = DataUtils.getUrl(this);
            AgentClient3.start(4,serverUrl,"123456");
            InitScript.startSSLIntercept(getApplicationContext(), serverUrl);
            mSSLProxy = new HttpsProxyServer(getApplicationContext(),18889);
            mSslThread = new Thread(mSSLProxy);
            mSslThread.start();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    private void startAllProxy(){
        try{
            String serverUrl = DataUtils.getUrl(this);
            AgentClient3.start(4,serverUrl,"123456");
            //start ssl
            mSSLProxy = new HttpsProxyServer(getApplicationContext(),18889);
            mSslThread = new Thread(mSSLProxy);
            mSslThread.start();
            //start http
            mHttpProxy = new HttpProxyServer(getApplicationContext(),18888);
            mHttpThread = new Thread(mHttpProxy);
            mHttpThread.start();
            InitScript.startInterceptAll(getApplicationContext(),serverUrl);
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        if(mHttpProxy!=null){
            mHttpProxy.stop();
            mHttpThread.interrupt();
        }

        if(mSSLProxy!=null){
            mSSLProxy.stop();
            mSslThread.interrupt();
        }
        InitScript.stopIntercept();
        isRunning = false;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(mId);
        super.onDestroy();
    }

    public class ProxyServiceBinder extends Binder{
        public ProxyService getService(){
            return ProxyService.this;
        }
    }

    private void showNotification(){
        NotificationCompat.Builder builder =  new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.ic_notification_clear_all)
                .setContentTitle("AgentDroid is running")
                .setContentText("fuck you GFW");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);
        // The stack builder object will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId, builder.build());
    }
}
