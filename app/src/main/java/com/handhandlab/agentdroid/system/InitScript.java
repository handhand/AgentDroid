package com.handhandlab.agentdroid.system;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.handhandlab.agentdroid.R;
import com.handhandlab.agentdroid.cert.CertHelper;
import com.handhandlab.agentdroid.utils.DataUtils;
import com.handhandlab.agentdroid.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper to run some shell scripts
 *
 * Created by Handhand on 2015/7/10.
 */
public class InitScript {

    private static final String IPTABLES = "/data/data/com.handhandlab.agentdroid/files/iptables";
    private static final String REDSOCKS = "/data/data/com.handhandlab.agentdroid/files/redsocks";

    /**
     * initiate when app first runs;
     * 1. copy iptables, redsocks, redsocks.conf from assets to private dir, and make them executable
     * 2. generate ca, including the keypair, and store them in app dir.(NOTE: the cert must rename according to its hash)
     * 3. copy ca cert to /etc/security/cacerts
     * 4. setup some predefined san
     * @param context
     */
    public static void systemInit(Context context)throws IOException,NoSuchAlgorithmException,
            NoSuchProviderException,
            InvalidKeyException,
            SecurityException,
            SignatureException,
            CertificateEncodingException,
            IOException {
        Utils.copyFile(context,"iptables");
        Utils.copyFile(context, "redsocks");
        Utils.copyFile(context, "redsocks.conf");
        String[] cmds = {
                "chmod 777 /data/data/com.handhandlab.agentdroid/files/iptables",
                "chmod 777 /data/data/com.handhandlab.agentdroid/files/redsocks",
                "chmod 777 /data/data/com.handhandlab.agentdroid/files/redsocks.conf"
        };
        Shell.runAsRoot(cmds);
        //generate ca and its keypair, rename ca cert, and copy it to system directory
        String certFileName = CertHelper.genCa(context);
        String[] cmdCerts = new String[]{
                "mount -o remount,rw /system",
                "cat /data/data/com.handhandlab.agentdroid/files/" + certFileName + " > /etc/security/cacerts/" + certFileName,
                //"cat /data/data/com.handhandlab.agentdroid/files/" + certFileName + " > /sdcard/verify_pem"
        };
        Shell.runAsRoot(cmdCerts);

        //add sans
        DataUtils.clear(context);
        String[] san = context.getResources().getStringArray(R.array.SubjectAlternativeNames);
        for(String name:san){
            DataUtils.addSanName(context,name);
        }
    }

    public static void startInterceptAll(Context context,String serverDomain)throws UnknownHostException{
        //get uid of proxied app
        Map<String,?> allProxied = DataUtils.getProxiedApps(context);

        //get ip address of server
        InetAddress address = InetAddress.getByName(serverDomain);//"handhand.eu5.org"
        String ip = address.getHostAddress();
        //redirect 80 to our app
        final String PROXY_APP_HTTP = IPTABLES + " -t nat -A OUTPUT -p tcp --dport 80 -m owner --uid-owner {uid} -j DNAT --to-destination 127.0.0.1:8888";
        //start redsocks
        final String START_REDSOCKS = REDSOCKS + " -p /data/data/com.handhandlab.agentdroid/files/redsocks.pid -c /data/data/com.handhandlab.agentdroid/files/redsocks.conf";
        //redirect 443 to redsocks
        final String PROXY_APP_HTTPS = IPTABLES + " -t nat -A OUTPUT -p tcp --dport 443 -m owner --uid-owner {uid} -j DNAT --to-destination 127.0.0.1:8818";
        final String CLEAR_TABLE = IPTABLES + " -t nat -F";
        final String DEFAULT_PASS_ALL = IPTABLES + " -t nat -j RETURN";
        //commands to be executed;
        List<String> iptableCmds = new ArrayList<>();
        //clear table first
        iptableCmds.add(CLEAR_TABLE);
        iptableCmds.add(START_REDSOCKS);
        //setup proxied app
        for (Map.Entry<String, ?> entry : allProxied.entrySet()) {
            int uid = (Integer) entry.getValue();
            String cmd_http = PROXY_APP_HTTP.replace("{uid}", String.valueOf(uid));
            String cmd_https = PROXY_APP_HTTPS.replace("{uid}",String.valueOf(uid));
            iptableCmds.add(cmd_http);
            iptableCmds.add(cmd_https);
        }
        //let other go through
        iptableCmds.add(DEFAULT_PASS_ALL);
        Shell.runAsRoot(iptableCmds);
    }

    /**
     * use iptables to redirect HTTP connection to our app(listening on 8888);
     * no need to use redsocks as HTTP is plain text, we can be the actual server
     *
     * redirect port 80 to port 8888, which our http (proxy) server will be listening on.
     *
     * @param context
     * @param serverDomain
     * @throws UnknownHostException
     */
    public static void startHTTPIntercept(Context context,String serverDomain)throws UnknownHostException{
        //get uid of proxied app
        Map<String,?> allProxied = DataUtils.getProxiedApps(context);

        //get ip address of server
        //InetAddress address = InetAddress.getByName(serverDomain);//"handhand.eu5.org"
        //String ip = address.getHostAddress();
        final String PROXY_APP = IPTABLES + " -t nat -A OUTPUT -p tcp --dport 80 -m owner --uid-owner {uid} -j DNAT --to-destination 127.0.0.1:8888";
        final String CLEAR_TABLE = IPTABLES + " -t nat -F";
        final String DEFAULT_PASS_ALL = IPTABLES + " -t nat -j RETURN";
        //commands to be executed;
        List<String> iptableCmds = new ArrayList<>();
        //clear table first
        iptableCmds.add(CLEAR_TABLE);
        //setup proxied app
        for (Map.Entry<String, ?> entry : allProxied.entrySet()) {
            int uid = (Integer) entry.getValue();
            String cmd = PROXY_APP.replace("{uid}", String.valueOf(uid));
            Log.d("haha", cmd);
            iptableCmds.add(cmd);
        }
        //let other go through
        iptableCmds.add(DEFAULT_PASS_ALL);
        Shell.runAsRoot(iptableCmds);
    }

    /**
     * 0. clear Iptables first
     * 1. start Iptables to intercept 443 connection to Redsocks(port 8118)
     * 2. Redsocks makes the connection a CONNECT request, and redirect it to our app listening on port 8899
     * NOTE: "man in the middle"
     *
     * @param context
     * @param serverDomain
     * @throws UnknownHostException
     */
    public static void startSSLIntercept(Context context,String serverDomain)throws UnknownHostException{
        //get uid of proxied app
        SharedPreferences prefs = context.getSharedPreferences("proxied", 0);
        Map<String, ?> allProxied = prefs.getAll();

        //get ip address of server
        InetAddress address = InetAddress.getByName(serverDomain);//"handhand.eu5.org"
        String ip = address.getHostAddress();
        final String START_REDSOCKS = REDSOCKS + " -p /data/data/com.handhandlab.agentdroid/files/redsocks.pid -c /data/data/com.handhandlab.agentdroid/files/redsocks.conf";
        final String PROXY_APP = IPTABLES + " -t nat -A OUTPUT -p tcp --dport 443 -m owner --uid-owner {uid} -j DNAT --to-destination 127.0.0.1:8818";
        final String CLEAR_TABLE = IPTABLES + " -t nat -F";
        final String DEFAULT_PASS_ALL = IPTABLES + " -t nat -j RETURN";
        //commands to be executed;
        List<String> startCommands = new ArrayList<>();
        //clear table first
        startCommands.add(CLEAR_TABLE);
        //setup proxied app
        for (Map.Entry<String, ?> entry : allProxied.entrySet()) {
            int uid = (Integer) entry.getValue();
            String cmd = PROXY_APP.replace("{uid}", String.valueOf(uid));
            Log.d("haha", cmd);
            startCommands.add(cmd);
        }
        //let other go through
        startCommands.add(DEFAULT_PASS_ALL);
        //start redsocks
        startCommands.add(START_REDSOCKS);
        Shell.runAsRoot(startCommands);
    }

    /**
     * stop iptables and redsocks from intercepting connections
     */
    public static void stopIntercept(){
        final String STOP_REDSOCKS = REDSOCKS + "kill -9 `cat /data/data/com.handhandlab.agentdroid/files/redsocks.pid`";
        final String CLEAR_TABLE = IPTABLES + " -t nat -F";
        String[] stopCmds = {STOP_REDSOCKS,CLEAR_TABLE};
        Shell.runAsRoot(stopCmds);
    }

    public static boolean checkInit(Context context){
        File dir = context.getFilesDir();
        String[] files = dir.list();
        String[] checkFiles = {"redsocks","iptables","ca_private.key","ca_public.key","redsocks.conf"};
        boolean isInit = true;
        for(String checkFile:checkFiles){
            boolean found = false;
            for(String fileInDir:files){
                if(checkFile.equals(fileInDir)){
                    found = true;
                    break;
                }
            }
            if(found==false)return false;
        }
        return true;
    }

}
