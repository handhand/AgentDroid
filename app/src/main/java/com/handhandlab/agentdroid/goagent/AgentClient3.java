package com.handhandlab.agentdroid.goagent;

import android.util.Log;

import com.handhandlab.agentdroid.proxy.HTTP;
import com.handhandlab.agentdroid.proxy.HandHttpRequest;
import com.handhandlab.agentdroid.proxy.HttpHelper;
import com.handhandlab.agentdroid.proxy.ProxyHandler;
import com.handhandlab.agentdroid.proxy.SSLProxyHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by Handhand on 2015/6/19.
 */
public class AgentClient3 extends Thread{

    private static final String CONTENT_HEADER = "Content-Type";
    private static final String TYPE_GIF = "image/gif";

    private static String GOAGENT_URL;
    private static List<AgentTask> pool = new LinkedList<AgentTask>();
    private static String password;

    private int threadId;

    public AgentClient3(int threadId){
        this.threadId = threadId;
    }

    public static void start(int threadNum, String url, String pw){
        GOAGENT_URL = url;
        password = pw;
        for(int i=0;i<threadNum;i++){
            new AgentClient3(i).start();
        }
    }

    public void run() {
        AgentTask task;
        while (true) {
            try {
                synchronized (pool) {
                    while (pool.isEmpty()) {
                        pool.wait();
                    }
                    task = pool.remove(0);
                }
                doJob(task);
            }
            catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private void doJob(AgentTask task){
        //Log.d("haha thread","thread do job:"+threadId);
        Log.d("haha","do job");
        HttpURLConnection conn;
        try{
            URL url = new URL(GOAGENT_URL);
            conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(10000);
            //conn.setReadTimeout(10000);//do not set read timeout, at least we connected successfully
            //conn.setRequestProperty("Connection","close");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();
            //write request body
            OutputStream out = conn.getOutputStream();
            out.write(encodeBody(task.request,task));
            out.flush();
        }catch(IOException ioe){
            ioe.printStackTrace();
            task.handler.write(HTTP.RESPONSE_404.getBytes());
            task.handler.closeChannel();
            return;
        }

        //read response
        byte[] readData;
        try{
            //Log.d("haha","server response code:"+conn.getResponseCode());
            if(conn.getResponseCode()!=200){
                InputStream err = conn.getErrorStream();
                //Log.d("haha thread","thread do job error:"+threadId);
                Log.d("haha","ERROR:"+new String(HttpHelper.readStream(err)));
                return;
            }
            readData = HttpHelper.readStream(conn.getInputStream());
        }catch(SocketTimeoutException timeout){
            //connect to server timeout, close the channel
            Log.d("haha","TIME OUTTTTTTTTTTTTT");
            task.handler.write(HTTP.RESPONSE_404.getBytes());
            task.handler.closeChannel();
            return;
        }
        catch(IOException ioe){
            ioe.printStackTrace();
            task.handler.write(HTTP.RESPONSE_404.getBytes());
            task.handler.closeChannel();
            return;
        }
        String contentType = conn.getHeaderField(CONTENT_HEADER);
        if(contentType.equals(TYPE_GIF)){
            byte[] responseData = decodeResponse(readData);
            task.handler.write(responseData);
            task.handler.onResponse();//callback, give a chance handler to execute some codes, e.g. close connection etc.
        }else{
            Log.d("haha","response:" +new String(readData));
            task.handler.write(readData);
            task.handler.onResponse();
        }
        conn.disconnect();
        //Log.d("haha thread","thread do job finish:"+threadId);
    }

    public static void goAgent(AgentTask task) {
        synchronized (pool) {
            pool.add(pool.size(), task);
            pool.notifyAll();
        }
    }

    /**
     * encode body
     * @return
     */
    public byte[] encodeBody(HandHttpRequest request,AgentTask task){
        //assemble request header, against the protocol of goagent
        StringBuilder sb = new StringBuilder();
        sb.append(request.getMethod()).append(" ");
        String path = request.getPath();
        if(path.startsWith("http")){
            //it's a full path
            sb.append(path);
        }else{
            String host = request.headers.get("Host");
            String scheme = "http://";
            if(task.handler instanceof SSLProxyHandler){
                scheme = "https://";
            }
            sb.append(scheme).append(host).append(path).append(" HTTP/1.1\r\n");//request line
        }
        for(Map.Entry<String,String> entry:request.headers.entrySet()){
            //skip headers
            if(entry.getKey().equals("Connection"))continue;//skip header according to Proxy.py
            //if(entry.getKey().equals("Accept-Encoding"))continue;//for debug purpose
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(HTTP.CRLF);
        }
        sb.append("X-URLFETCH-PASSWORD").append(": ").append(password).append(HTTP.CRLF);
        Log.d("haha","generated header:" +sb.toString());
        //StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream encodeStream = new ByteArrayOutputStream();
        ByteArrayOutputStream postStream = new ByteArrayOutputStream();
        // Encode a String into bytes
        DeflaterOutputStream dos = new DeflaterOutputStream(encodeStream);
        try{
            //deflate header
            dos.write(sb.toString().getBytes());
            dos.flush();
            dos.close();
            //goagent uses php's gzinflate, it's not identical with java's DeflateOutputStream, need some tweaking
            //to work with php's gzinflate:http://php.net/manual/en/function.gzinflate.php
            byte[] headersEncoded = encodeStream.toByteArray();
            postStream.write(packn(headersEncoded.length - 6));//write the first 2 bytes as the headers data length
            postStream.write(headersEncoded,2,headersEncoded.length-2-4);
            //write body
            if(request.body!=null)
                postStream.write(request.body);

            return postStream.toByteArray();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
        return null;
    }

    /**
     *
     * same as php pack(n,value)
     * work with goagent's unpack(n,value);
     */
    static byte[] packn(int value) throws UnsupportedEncodingException {
        byte[] bytes = ByteBuffer.allocate(2).putChar((char)value).array();
        return bytes;
    }


    static byte[] decodeResponse(byte[] data){
        byte[] decoded = new byte[data.length];
        char c = password.charAt(0);
        for(int i=0;i<data.length;i++){
            decoded[i] = (byte)((int)data[i] ^ c);
        }
        return decoded;
    }

    public static class AgentTask{
        HandHttpRequest request;
        ProxyHandler handler;//pass in handler instead of channel, because we might use SSL, can not directly write to channel
        public AgentTask(HandHttpRequest request, ProxyHandler handler){
            this.request = request;
            this.handler = handler;
        }
    }

    //------------test-------------
    private String setConnection(String str){
        int connIndex = str.indexOf("Connection");
        if(connIndex>=0){
            StringBuilder sb = new StringBuilder(str);
            int connEnd = str.indexOf("\r\n",connIndex);
            sb.delete(connIndex,connEnd + 2);
            return sb.toString();
        }
        else{
            return str;
        }
    }
}
