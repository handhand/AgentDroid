package com.handhandlab.agentdroid.proxy;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import android.util.Log;

public class HttpHelper {

	public static byte[] get(String urlStr) throws IOException {
		int index = urlStr.indexOf(":");
		if(index>0)
			urlStr = urlStr.substring(0, index);
		URL url = new URL("http://"+urlStr);
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		try {
			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			return readStream(in);
		} finally {
			urlConnection.disconnect();
		}

	}

    /**
     * 处理服务器的响应结果（将输入流转化成字符串）
     * read the InputStream, and return the result as byte array
     * @param inputStream inputStream get from HttpUrlConnection
     * @return
     * @throws SocketTimeoutException
     */
	public static byte[] readStream(InputStream inputStream) throws SocketTimeoutException{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] data = new byte[1024];
		int len = 0;
		try {
            //int total = 0;
			while ((len = inputStream.read(data)) != -1) {
                //Log.d("haha", "len:"+len);
                //total = total + len;
				byteArrayOutputStream.write(data, 0, len);
			}
            //Log.d("haha","total:"+total);
		}catch(SocketTimeoutException timeout){
           throw timeout;
        }catch (IOException e) {
			e.printStackTrace();
		}
		//Log.d("haha", "server return count:"+byteArrayOutputStream.toByteArray().length);
		return byteArrayOutputStream.toByteArray();
	}
}
