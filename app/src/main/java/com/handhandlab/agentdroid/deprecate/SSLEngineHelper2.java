package com.handhandlab.agentdroid.deprecate;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.handhandlab.agentdroid.cert.CertHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

public class SSLEngineHelper2 {

	private static final String PASSPHRASE = "";//password for keystore
	private static final String KEY_STORE_PATH = "/sdcard/test.bks";

	SSLEngine sslEngine;
	private SSLContext sslContext;
	private ByteBuffer appOut; // clear text buffer for out
    private ByteBuffer appIn; // clear text buffer for in
    private ByteBuffer netOut; // encrypted buffer for out
    private ByteBuffer netIn; // encrypted buffer for in

    public boolean handshakeDone = false;

    private CharsetDecoder decoder = Charset.forName("UTF8").newDecoder();

    public SSLEngineHelper2(Context context, String domain){
    	status = NOT_INIT;
        try
        {
            createSSLContext(context,domain);
        } catch (GeneralSecurityException e)
        {
            System.out.println("initializing SSL context failed");
            e.printStackTrace();
        } catch (IOException e)
        {
            System.out.println("reading keystore or truststore file failed");
            e.printStackTrace();
        }

        createSSLEngines();
        createBuffers();
    }

    private void createSSLEngines()
    {
        sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);//work in a server mode
        sslEngine.setNeedClientAuth(false);//DO NOT need client authentication
    }

	private void createBuffers()
    {
        SSLSession session = sslEngine.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();

        appOut = ByteBuffer.allocateDirect(netBufferMax); ;//server only reply this sentence
        appIn = ByteBuffer.allocate(appBufferMax + 10);//appIn is bigger than the allowed max application buffer siz
        netOut = ByteBuffer.allocateDirect(netBufferMax);//direct allocate for better performance
        netIn = ByteBuffer.allocateDirect(netBufferMax);
    }

    //the ssl context initialization
    private void createSSLContext(Context context,String url) throws GeneralSecurityException, FileNotFoundException, IOException
    {
        Uri uri = Uri.parse(url);
        String domain = uri.getHost();

        KeyStore ks = KeyStore.getInstance("BKS");
        //KeyStore ts = KeyStore.getInstance("BKS");

        char[] passphrase = PASSPHRASE.toCharArray();

        ks.load(null);
        //ks.load(new FileInputStream(KEY_STORE_PATH), passphrase);
        //ts.load(new FileInputStream(KEY_STORE_PATH), passphrase);
        //Log.d("haha", "default alg:"+KeyManagerFactory.getDefaultAlgorithm());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, passphrase);

        //TODO:gen cert and sign on the fly;
        X509Certificate[] certChain = new X509Certificate[1];
        //gen key pair
        KeyPair keyPair = CertHelper.genKeyPair();
        //use key pair to gen cert,it depends on 1, CertHelper gen a ca and put it in /etc/security/cacerts 2, keypair of ca in /data/data/xxx/files
        X509Certificate cert = CertHelper.genCert(context,domain,keyPair,null);
        certChain[0] = cert;
        ks.setEntry(domain,
                new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), certChain),
                new KeyStore.PasswordProtection("".toCharArray()));
        //END

        //TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        //tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("SSL");

        sslCtx.init(kmf.getKeyManagers(), /*tmf.getTrustManagers()*/null, null);

        sslContext = sslCtx;

    }

    public void startHandshake(){
        try{
            sslEngine.beginHandshake();
            status = HANDSHAKING;
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void onReadData(byte[] data,SocketChannel channel){
        if(status == HANDSHAKING){
            HandshakeStatus hsStatus = sslEngine.getHandshakeStatus();
            switch (hsStatus){
                case FINISHED:
                    Log.d("haha","finished!");
                    status = HANDSHAKED;
                    break;
                case NEED_TASK:
                    Log.d("haha","need task");
                    break;
                case NEED_UNWRAP:
                    break;
            }
        }
    }

    public void doHandShake(SocketChannel sc) throws IOException
    {

        sslEngine.beginHandshake();//explicitly begin the handshake
        status = HANDSHAKING;
        HandshakeStatus hsStatus = sslEngine.getHandshakeStatus();
        while (!handshakeDone)
        {
            switch(hsStatus){
                case FINISHED:
                	Log.d("haha", "FINISH");
                    //the status become FINISHED only when the ssl handshake is finished
                    //but we still need to sendBytes data, so do nothing here
                    break;
                case NEED_TASK:
                    //do the delegate task if there is some extra work such as checking the keystore during the handshake
                	Log.d("haha", "NEED TASK");
                    hsStatus = doTask();
                    break;
                case NEED_UNWRAP:
                    //unwrap means unwrap the ssl packet to get ssl handshake information
                	//Log.d("haha", "UNWRAP");
                    sc.read(netIn);
                    netIn.flip();
                    hsStatus = doUnwrap();
                    break;
                case NEED_WRAP:
                    //wrap means wrap the app packet into an ssl packet to add ssl handshake information
                	Log.d("haha", "WRAP");
                    hsStatus = doWrap();
                    sc.write(netOut);
                    netOut.clear();
                    break;
                case NOT_HANDSHAKING:
                	Log.d("haha", "NOT_HANDSHAKING");
                    //now it is not in a handshake or say byebye status. here it means handshake is over and ready for ssl talk
                    //sc.configureBlocking(false);//set the socket to unblocking mode
                    //sc.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE);//register the readToData and write event
                    handshakeDone = true;
                    status = HANDSHAKED;
                    break;
            }
        }
        Log.d("haha", "handshake done?!");
    }

    private HandshakeStatus doTask() {
        Runnable runnable;
        while ((runnable = sslEngine.getDelegatedTask()) != null)
        {
            Log.d("haha", "running delegated task...");
            runnable.run();
        }
        HandshakeStatus hsStatus = sslEngine.getHandshakeStatus();
        if (hsStatus == HandshakeStatus.NEED_TASK)
        {
            //throw new Exception("handshake shouldn't need additional tasks");
        	//Log.d("haha", "handshake shouldn't need additional tasks");
        }
        Log.d("haha", "new HandshakeStatus: " + hsStatus);

        return hsStatus;
    }

    private HandshakeStatus doUnwrap() throws SSLException{
        HandshakeStatus hsStatus;
        do{//do unwrap until the state is change to "NEED_WRAP"
            SSLEngineResult engineResult = sslEngine.unwrap(netIn, appIn);
            //log("haha", engineResult);
            hsStatus = doTask();
        }while(hsStatus ==  HandshakeStatus.NEED_UNWRAP && netIn.remaining()>0);
        //Log.d("haha", "new HandshakeStatus: " + hsStatus);
        netIn.clear();
        return hsStatus;
    }

    private HandshakeStatus doWrap() throws SSLException{
        HandshakeStatus hsStatus;
        SSLEngineResult engineResult = sslEngine.wrap(appOut, netOut);
        log("server wrap: ", engineResult);
        hsStatus = doTask();
        Log.d("haha", "new HandshakeStatus: " + hsStatus);
        netOut.flip();
        return hsStatus;
    }

    //close an ssl talk, similar to the handshake steps
    private void doSSLClose(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        key.cancel();

        try
        {
            sc.configureBlocking(true);
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        HandshakeStatus hsStatus = sslEngine.getHandshakeStatus();
        while(handshakeDone) {
            switch(hsStatus) {
            case FINISHED:

                break;
            case NEED_TASK:
                hsStatus = doTask();
                break;
            case NEED_UNWRAP:
                sc.read(netIn);
                netIn.flip();
                hsStatus = doUnwrap();
                break;
            case NEED_WRAP:
                hsStatus = doWrap();
                sc.write(netOut);
                netOut.clear();
                break;
            case NOT_HANDSHAKING:
                handshakeDone = false;
                sc.close();
                break;
            }
        }
    }

    public static void log(String str, SSLEngineResult result)
    {
        HandshakeStatus hsStatus = result.getHandshakeStatus();
        /*Log.d("haha", str + result.getStatus() + "/" + hsStatus + ", " + result.bytesConsumed() + "/"
                + result.bytesProduced() + " bytes");   */
        if (hsStatus == HandshakeStatus.FINISHED)
        {
            Log.d("haha", "ready for application data");
        }
    }

    /**
     * 从channel中读取加密的数据，返回解密数据
     * @param sc
     * @return
     * @throws java.io.IOException
     */
    public String read(byte[] inData) throws IOException{
    	Log.d("haha", "readToData!!!!!!");
    	if (sslEngine.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING)  
        {
            netIn.clear();
            appIn.clear();
            //sc.readToData(netIn);
            netIn = netIn.put(inData);
            netIn.flip();  
              
            SSLEngineResult engineResult = sslEngine.unwrap(netIn, appIn);  
            log("server unwrap: ", engineResult);  
            doTask();  
            //runDelegatedTasks(engineResult, sslEngine);  
            netIn.compact();  
            if (engineResult.getStatus() == SSLEngineResult.Status.OK)  
            {  
                System.out.println("text recieved");  
                appIn.flip();// ready for reading  
                System.out.println(decoder.decode(appIn));
                appIn.compact();
                return new String(decoder.decode(appIn).array());
            }  
            else if(engineResult.getStatus() == SSLEngineResult.Status.CLOSED) {  
                //doSSLClose(key);  
            }
        }
        return null;
    }
    
    public void write(SocketChannel sc,byte[] data) throws IOException{
    	/*appOut.clear();
    	appOut.put(data);*/
    	appOut = ByteBuffer.wrap(data);
    	SSLEngineResult engineResult = sslEngine.wrap(appOut, netOut);  
        //log("server wrap: ", engineResult);  
        doTask();  
        //runDelegatedTasks(engineResult, sslEngine);  
        if (engineResult.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING)  
        {  
            System.out.println("text sent");  
        }  
        netOut.flip();  
        sc.write(netOut);  
        //Log.d("haha", "dd"+netOut.remaining());
        netOut.compact();  
    }
    
    public static final int NOT_INIT = 0;
    public static final int HANDSHAKING = 1;
    public static final int HANDSHAKED = 2;
    private int status;
    public int getStatus(){
    	return status;
    }
}
