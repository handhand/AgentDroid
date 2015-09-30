package com.handhandlab.agentdroid.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.handhandlab.agentdroid.R;
import com.handhandlab.agentdroid.dns.DNSIntercepter;
import com.handhandlab.agentdroid.openssl.OpensslWrapper;
import com.handhandlab.agentdroid.system.InitAsyncTask;
import com.handhandlab.agentdroid.system.InitScript;
import com.handhandlab.agentdroid.utils.DataUtils;
import com.handhandlab.agentdroid.utils.Utils;


public class MainActivity extends Activity implements ServiceConnection,View.OnClickListener{

    static{
        System.loadLibrary("agentdroid");
    }
    ProxyService proxyService;
    TextView tvUrl;
    Button btnStartService;
    Button btnSSLProxy;
    Button btnHTTPProxy;
    Button btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkInit();
        tvUrl = (TextView)findViewById(R.id.tv_url);
        tvUrl.setText(DataUtils.getUrl(this));
        //init app
        //findViewById(R.id.btn_init).setOnClickListener(this);//for debug
        //set san names
        findViewById(R.id.btn_san_names).setOnClickListener(this);
        //set proxy app
        findViewById(R.id.btn_proxy_apps).setOnClickListener(this);
        //set url
        findViewById(R.id.service_url).setOnClickListener(this);
        btnHTTPProxy = (Button)findViewById(R.id.start_http_service);//start http proxy
        btnStop = (Button)findViewById(R.id.stop_service);//stop service
        btnSSLProxy = (Button)findViewById(R.id.start_https_service);//start https proxy
        btnStartService = (Button)findViewById(R.id.start_service);//start http and https proxy

        btnHTTPProxy.setOnClickListener(this);
        btnSSLProxy.setOnClickListener(this);
        btnStartService.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        //check if the service is already running
        if(ProxyService.isRunning){
            Intent i = new Intent(this,ProxyService.class);
            //bindService(i, this, 0);
            setButtonsState(false);
        }
    }

    @Override
    public void onClick(View v) {
        Intent i;
        switch (v.getId()){
            case R.id.service_url:
                openDialog();
                break;
            //init ca, iptables, redsocks etc., for debug
            case  R.id.btn_init:
                AsyncTask task = new InitAsyncTask(getApplicationContext());
                task.execute(new Object[]{});
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
                /*Thread thread = new Thread(new DNSIntercepter());
                thread.start();
                InitScript.interceptDns(MainActivity.this);
                btnStop.setEnabled(true);*/
                break;
            case R.id.start_http_service:
                startService(ProxyService.MODE_HTTP);
                break;
            case R.id.start_https_service:
                startService(ProxyService.MODE_HTTPS);
                break;
            case R.id.start_service:
               startService(ProxyService.MODE_ALL);
                break;
            case R.id.stop_service:
                //getApplicationContext().unbindService(this);
                i = new Intent(MainActivity.this,ProxyService.class);
                stopService(i);
                setButtonsState(true);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        proxyService = ((ProxyService.ProxyServiceBinder)service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    private void startService(int mode){
        if(TextUtils.isEmpty(tvUrl.getText())){
            Toast.makeText(this,R.string.prompt_set_url_first,Toast.LENGTH_LONG).show();
            return;
        }
        Intent i;
        i = new Intent(MainActivity.this,ProxyService.class);
        i.putExtra(ProxyService.EXTRA_MODE,mode);
        startService(i);
        //bindService(i,this,0);
        setButtonsState(false);
    }

    private void checkInit(){
        if(DataUtils.isInit(this)==false){
            InitAsyncTask initTask = new InitAsyncTask(this);
            initTask.execute(new String[]{});
        }
    }

    private void setButtonsState(boolean isEnable){
        btnHTTPProxy.setEnabled(isEnable);
        btnSSLProxy.setEnabled(isEnable);
        btnStartService.setEnabled(isEnable);
        btnStop.setEnabled(!isEnable);
    }

    private void openDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_edit, null);
        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.edit_url);
        userInput.setText(tvUrl.getText());
        // set dialog message
        alertDialogBuilder
                .setTitle("Set url")
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                // edit text
                                String input = userInput.getText().toString();
                                if (Utils.isUrl(input)) {
                                    tvUrl.setText(input);
                                    DataUtils.saveUrl(MainActivity.this,input);
                                } else {
                                    Toast.makeText(MainActivity.this,"Not a url",Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();

    }
}
