package com.handhandlab.agentdroid.deprecate;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;
import com.handhandlab.agentdroid.proxy.HandyHttpRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by Handhand on 2015/6/15.
 */
public class AgentClient{

    RequestQueue requestQueue;
    RetryPolicy retryPolicy;
    String password;
    static AgentClient instance;

    private AgentClient(Context context,String password){
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        this.password = password;
        retryPolicy = new DefaultRetryPolicy(
                (int) TimeUnit.SECONDS.toMillis(10),
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    }

    public static AgentClient getInstance(Context context,String password){
        if(instance==null)instance = new AgentClient(context,password);
        return instance;
    }

    /**
     * default password is 123456
     * @param context
     * @return
     */
    public static AgentClient getInstance(Context context){
        if(instance==null)instance = new AgentClient(context,"123456");
        return instance;
    }

    //use volley to post request to goagent php server
    public void postRequest(ConnContext cc,HandyHttpRequest request,String url,Response.Listener listener,Response.ErrorListener errorListener){

        AgentRequest volleyRequest = new AgentRequest(
                Request.Method.POST,
                url,
                listener,
                errorListener,
                encodeBody(request));
        volleyRequest.setRetryPolicy(retryPolicy);

        requestQueue.add(volleyRequest);
        //when async back, write data to ConnContext, register write interests
        //OR write channel directly
    }


    /**
     * post request of volley
     */
    public class AgentRequest extends Request<AgentResponse>{
        byte[] body;//http post body
        String contentType = null;//when using multipart upload, this should be set
        Response.Listener<AgentResponse> listener;

        public AgentRequest(int method, String url, Response.Listener<AgentResponse> listener, Response.ErrorListener errorListener, byte[] body) {
            super(method, url, errorListener);
            this.listener = listener;
            this.body = body;
        }
        public AgentRequest(int method, String url, Response.Listener<AgentResponse> listener, Response.ErrorListener errorListener, byte[] body, String contentType) {
            super(method, url, errorListener);
            this.listener = listener;
            this.body = body;
            this.contentType = contentType;
        }
        @Override
        protected void deliverResponse(AgentResponse response) {
            listener.onResponse(response);
        }
        @Override
        public byte[] getBody() throws AuthFailureError {
            if(body!=null){
                return body;
            }
            return super.getBody();
        }

        @Override
        public String getBodyContentType() {
            if(contentType != null)
                return contentType;
            return super.getBodyContentType();
        }

        @Override
        protected Response<AgentResponse> parseNetworkResponse(NetworkResponse response) {
            //decode,
            String contentType = response.headers.get(CONTENT_HEADER);
            //goagent会将text类型返回为image/gif类型，并将文本数据根据密码的第一位进行异或操作
            if(contentType.equals(TYPE_GIF)){
                byte[] decoded = new byte[response.data.length];
                char c = password.charAt(0);
                for(int i=0;i<response.data.length;i++){
                    decoded[i] = (byte)((int)response.data[i] ^ c);
                }
                //Log.d("haha",new String(decoded));
                AgentResponse agentResponse = new AgentResponse(RESP_TEXT,response.data,new String(decoded));
                return Response.success(agentResponse, HttpHeaderParser.parseCacheHeaders(response));
            }
            else{
                //this is a byte array,其他类型，即图片等二进制流
                AgentResponse agentResponse = new AgentResponse(RESP_MEDIA,response.data,null);
                return Response.success(agentResponse, HttpHeaderParser.parseCacheHeaders(response));
            }
        }
    }
    private static final String CONTENT_HEADER = "Content-Type";
    private static final String TYPE_GIF = "image/gif";
    public static final int RESP_TEXT = 0;
    public static final int RESP_MEDIA = 1;

    /**
     * 自定义Response，分为两种类型
     * text类型
     * 媒体类型
     */
    public class AgentResponse{
        public AgentResponse(int type,byte[] rawData,String decodedText){
            this.type = type;
            this.rawData = rawData;
            this.decodedText = decodedText;
        }
        int type;
        byte[] rawData;
        String decodedText;

        public int getType() {
            return type;
        }

        public byte[] getRawData() {
            return rawData;
        }

        public String getDecodedText() {
            return decodedText;
        }
    }

    /**
     * encode body
     * @return
     */
    public byte[] encodeBody(HandyHttpRequest request){
        //assemble request header, against the protocol of goagent
        StringBuilder sb = new StringBuilder();
        sb.append(request.getMethod()).append(" ");
        String path = request.getPath();
        if(path.startsWith("http")){
            //it's a full path
            sb.append(path);
        }else{
            String host = request.headers.get("host");
            sb.append("http://").append(host).append(path).append("\r\n");
        }
        for(Map.Entry<String,String> entry:request.headers.entrySet()){
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append("\r\n");
        }
        sb.append("X-URLFETCH-PASSWORD").append(":").append(password).append("\r\n");
        Log.d("haha agent","generated header:"+sb.toString());

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
            postStream.write(packn(headersEncoded.length - 6).getBytes());
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
    static String packn(int value) throws UnsupportedEncodingException{
        byte[] bytes = ByteBuffer.allocate(2).putChar((char)value).array();
        return new String(bytes, "utf8");
    }

    static interface AgentListener{
        public void onResponse(AgentResponse response);
    }
}
