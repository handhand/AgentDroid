package com.handhandlab.agentdroid.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

import com.handhandlab.agentdroid.R;
import com.handhandlab.agentdroid.cert.CertHelper;
import com.handhandlab.agentdroid.proxy.Server2;
import com.handhandlab.agentdroid.proxy.ServerSSL;
import com.handhandlab.agentdroid.system.InitAsyncTask;
import com.handhandlab.agentdroid.system.InitScript;
import com.handhandlab.agentdroid.system.Shell;
import com.handhandlab.agentdroid.test.TestHandler;
import com.handhandlab.agentdroid.test.TestServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class MainActivity extends ActionBarActivity implements ServiceConnection,View.OnClickListener{

    ProxyService proxyService;
    Button btnStartService;
    Button btnSSLProxy;
    Button btnHTTPProxy;
    Button btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.test_server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TestServer nioServer = new TestServer(8080, new TestHandler());
                Thread t = new Thread(nioServer);
                t.start();

            }
        });

        /**
         * start http proxy server
         */
        findViewById(R.id.test_http_server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Server2 nioServer = new Server2(getApplicationContext(),8899);
                    Thread t = new Thread(nioServer);
                    t.start();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                //与服务端建立连接
                /*Thread d = new Thread(){
                    @Override
                    public void run() {
                        try{
                            String host = "127.0.0.1";  //要连接的服务端IP地址
                            int port = 8080;   //要连接的d服务端对应的监听端口
                            client = new Socket(host, port);
                            String s = "hahathisisreally";
                            Writer writer = new OutputStreamWriter(client.getOutputStream());
                            writer.write(s);
                            writer.flush();
                            Thread.sleep(1000);
                            String ss = "reallyreallylong";
                            writer.write(ss);
                            writer.flush();//写完后要记得flush
                            Log.d("haha","client write finished");
                            InputStream is = client.getInputStream();
                            byte[] buffer = new byte[100];
                            int read = is.read(buffer);
                            Log.d("haha","client:"+new String(buffer,0,read));
                            client.close();
                            StringBuilder sb = new StringBuilder();
                        }catch (IOException ioe){
                            ioe.printStackTrace();
                        }catch(InterruptedException ie){
                            ie.printStackTrace();
                        }
                    }
                };
                d.start();*/


            }
        });



        findViewById(R.id.test_ssl_proxy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    @Override
                    public void run() {
                        try{
                            InitScript.startSSLIntercept(MainActivity.this,"");
                        }catch (UnknownHostException e){
                            e.printStackTrace();
                        }
                    }
                }.start();

                try {
                    ServerSSL SSLProxyServer = new ServerSSL(getApplicationContext(),8899);
                    Thread t = new Thread(SSLProxyServer);
                    t.start();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        //init app
        findViewById(R.id.btn_init).setOnClickListener(this);
        //set san names
        findViewById(R.id.btn_san_names).setOnClickListener(this);
        //set proxy app
        findViewById(R.id.btn_proxy_apps).setOnClickListener(this);

        btnStartService = (Button)findViewById(R.id.start_http_service);
        btnStop = (Button)findViewById(R.id.stop_service);
        btnSSLProxy = (Button)findViewById(R.id.start_https_service);
        btnStartService.setOnClickListener(this);
        btnStop.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent i;
        switch (v.getId()){
            //init ca, iptables, redsocks etc.
            case  R.id.btn_init:
                AsyncTask task = new InitAsyncTask(getApplicationContext());
                task.execute(new String[]{});
                v.setEnabled(false);
                break;
            //setup proxied apps
            case R.id.btn_proxy_apps:
                i = new Intent(MainActivity.this, AppListActivity.class);
                startActivity(i);
                break;
            //setup subject alternative names of ssl certificates
            case R.id.btn_san_names:
                i = new Intent(getApplicationContext(),SanListActivity.class);
                startActivity(i);
                break;
            case R.id.start_http_service:
                Intent intent = new Intent(MainActivity.this,ProxyService.class);
                intent.putExtra(ProxyService.EXTRA_MODE,ProxyService.MODE_HTTP);
                getApplicationContext().bindService(intent, MainActivity.this, Context.BIND_AUTO_CREATE);
                v.setEnabled(false);
                btnStop.setEnabled(true);
                break;
            case R.id.stop_service:
                getApplicationContext().unbindService(this);
                v.setEnabled(false);
                btnStartService.setEnabled(true);
                break;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        proxyService = ((ProxyService.ProxyServiceBinder)service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }



    /**
     * leave out redsocks
     */
    private void start(final String serverDomain) {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    InitScript.startHTTPIntercept(MainActivity.this,"handhand.eu5.org");
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        };
        t.start();
    }

}
