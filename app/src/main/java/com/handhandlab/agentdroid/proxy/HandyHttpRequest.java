package com.handhandlab.agentdroid.proxy;

import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * representing the client http request
 */
public class HandyHttpRequest {

    //request method, GET, POST
	String method;

    //path in the request line
    //it will be the complete url if the client knows it's connecting to a proxy, otherwise url = scheme + host + path
	String path;

	public Map<String,String> headers = new HashMap<>();//request headers

    public byte[] body;//request body

    public String headerStr;
	
	public HandyHttpRequest(){}
	
	public HandyHttpRequest(String str){
        //Log.d("haha","request:"+str);

        //find empty line
        int emptyLineIndex = str.indexOf(HTTP.EMPTY_LINE);
        headerStr = str;
        String bodyStr = "";
        if(emptyLineIndex>0){
            //strip headers
            headerStr = str.substring(0,emptyLineIndex);
            //get body
            if(str.length() - 4 > emptyLineIndex){//4 is length of empty line
                bodyStr = str.substring(emptyLineIndex + 4);
                body = bodyStr.getBytes();
            }
        }

        //decode header - request line
		String[] lines = headerStr.split(HTTP.CRLF);
        //request line
		String[] firstLine = lines[0].split(" ");
		method = firstLine[0];
		path = firstLine[1];

        //decode header - other headers
        if(lines.length>1){
            for(int i=1;i<lines.length;i++){// iterate through lines
                String[] keyValue = lines[i].split(":", -1);
                if(keyValue.length<2)continue;
                this.headers.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }

	}

	public String getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}

    public String getHost(){
        return headers.get("host");
    }

}
