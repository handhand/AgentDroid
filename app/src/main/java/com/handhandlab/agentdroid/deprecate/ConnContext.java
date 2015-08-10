package com.handhandlab.agentdroid.deprecate;

import android.util.Log;

import com.handhandlab.agentdroid.proxy.HandHttpRequest;
import com.handhandlab.agentdroid.proxy.SSLEngineHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ConnContext {
	private static final int BUFFER_SIZE = 1024 * 10;
	public SSLEngineHelper ssl;
	public SocketChannel channel;
	public byte[] inData;
    public byte[] outData;
	//public Request req;
	//public Response resp;
	public ByteBuffer readBuffer;
	public ByteBuffer writeBuffer;
	public HandHttpRequest request;
	
	public ConnContext(){
		readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
	}
	
	/**
	 * 从channel读取数据到data
	 * 必须在key isReadable时才调用
	 * @throws java.io.IOException
	 */
	public byte[] readToData() throws IOException{
        int r = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while ( true ) {
            readBuffer.clear();
            r = channel.read(readBuffer);
            if(r == 0 )break;
            if(r == -1){
                //如果read()方法返回-1，则表示底层连接已经关闭，此时需要关闭信道。
                //关闭信道时，将从选择器的各种集合中移除与该信道关联的键。
                Log.d("haha read","close!");
                channel.close();
                return null;
            }
            readBuffer.flip();
            //baos.write(readBuffer.array());
            //TODO:NEED OPTIMIZE
            byte[] bytes = new byte[readBuffer.remaining()];
            readBuffer.get(bytes);
            baos.write(bytes);
        }
        inData = baos.toByteArray();
        return inData;
	}
	
	
	/**
     * 向客户端写字节流数据
     */
    public void sendBytes(byte[] data) throws IOException {

    	int writeStart = 0;
    	int writeEnd = 0;
    	while(true){
    		writeBuffer.clear();
    		writeStart = writeEnd;
    		int remaining = data.length - writeStart;
    		if(remaining <= 0) break;
    		if(remaining>writeBuffer.capacity())
    			writeEnd = writeStart + writeBuffer.capacity();
    		else
    			writeEnd = writeStart + remaining;
            writeBuffer.put(data, writeStart, writeEnd);
            writeBuffer.flip();
            channel.write(writeBuffer);
    	}

    }

    public void setSendData(byte[] data){
        outData = data;
        Server.registerWrite(this);
    }
    


    private void helper(SocketChannel channel, ByteBuffer buffer,byte[] data) throws IOException {

        int writeStart = 0;
        int writeEnd = 0;
        while(true){
            buffer.clear();
            writeStart = writeEnd;
            int remaining = data.length - writeStart;
            if(remaining <= 0) break;
            if(remaining>buffer.capacity())
                writeEnd = writeStart + buffer.capacity();
            else
                writeEnd = writeStart + remaining;
            buffer.put(data, writeStart, writeEnd);
            buffer.flip();
            channel.write(buffer);
        }

    }
}
