package com.handhandlab.agentdroid.goagent;

import android.util.Log;

import com.handhandlab.agentdroid.proxy.HandHttpRequest;
import com.handhandlab.agentdroid.proxy.HttpHelper;
import com.handhandlab.agentdroid.proxy.HttpProxyHandler;

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
public class AgentClient2 extends Thread{

    private static final String CONTENT_HEADER = "Content-Type";
    private static final String TYPE_GIF = "image/gif";
    public static final int RESP_TEXT = 0;
    public static final int RESP_MEDIA = 1;

    private static String GOAGENT_URL;
    private static List<AgentTask> pool = new LinkedList<AgentTask>();
    private static String password;

    private int threadId;

    public AgentClient2(int threadId){
        this.threadId = threadId;
    }

    public static void start(int threadNum, String url, String pw){
        GOAGENT_URL = url;
        password = pw;
        for(int i=0;i<threadNum;i++){
            new AgentClient2(i).start();
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
        HttpURLConnection conn;
        try{
            URL url = new URL(GOAGENT_URL);
            conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Connection","close");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();
            //write request body
            OutputStream out = conn.getOutputStream();
            out.write(encodeBody(task.request,task.handler.handlerId));
            out.flush();
        }catch(IOException ioe){
            ioe.printStackTrace();
            task.handler.write("HTTP/1.1 404 Not Found\\r\\n\\r\\n".getBytes());
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
                //Log.d("haha","ERROR:"+new String(HttpHelper.readStream(err)));
                return;
            }
            readData = HttpHelper.readStream(conn.getInputStream());
        }catch(SocketTimeoutException timeout){
            //connect to server timeout, close the channel
            Log.d("haha","TIME OUTTTTTTTTTTTTT");
            task.handler.write("HTTP/1.1 404 Not Found\\r\\n\\r\\n".getBytes());
            task.handler.closeChannel();
            return;
        }
        catch(IOException ioe){
            ioe.printStackTrace();
            task.handler.write("HTTP/1.1 404 Not Found\\r\\n\\r\\n".getBytes());
            task.handler.closeChannel();
            return;
        }
        String contentType = conn.getHeaderField(CONTENT_HEADER);
        if(contentType.equals(TYPE_GIF)){
            //Log.d("haha "+connContext.request.getPath(),"get data:"+decodeResponse(readData));
            byte[] responseData = decodeResponse(readData);
            Log.d("haha","id: "+task.handler.handlerId+ "\n" +new String(responseData));
            task.handler.write(responseData);
            //task.handler.closeChannel();
        }else{
            Log.d("haha","id: "+task.handler.handlerId+ "\n" +new String(readData));
            task.handler.write(readData);
            //task.handler.closeChannel();
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
    public byte[] encodeBody(HandHttpRequest request,int id){
        //assemble request header, against the protocol of goagent
        StringBuilder sb = new StringBuilder();
        sb.append(request.getMethod()).append(" ");
        String path = request.getPath();
        if(path.startsWith("http")){
            //it's a full path
            sb.append(path);
        }else{
            String host = request.headers.get("Host");
            sb.append("http://").append(host).append(path).append("\r\n");
        }
        for(Map.Entry<String,String> entry:request.headers.entrySet()){
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        sb.append("X-URLFETCH-PASSWORD").append(": ").append(password).append("\r\n");

        Log.d("haha","generated header:"+id+"\n"
                +sb.toString());
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
            postStream.write(packn(headersEncoded.length - 6));
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
        HttpProxyHandler handler;//pass in handler instead of channel, because we might use SSL, can not directly write to channel
        public AgentTask(HandHttpRequest request, HttpProxyHandler handler){
            this.request = request;
            this.handler = handler;
        }
    }
}
