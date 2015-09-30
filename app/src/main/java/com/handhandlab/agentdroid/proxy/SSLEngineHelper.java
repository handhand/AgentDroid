package com.handhandlab.agentdroid.proxy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.handhandlab.agentdroid.cert.CertHelper;
import com.handhandlab.agentdroid.cert.NativeCertHelper;
import com.handhandlab.agentdroid.openssl.CertWrapper;
import com.handhandlab.agentdroid.utils.DataUtils;

/**
 * Helper class to wrap around the SSLEngine implementation details
 */
public class SSLEngineHelper{
	
	private static final String PASSPHRASE = "";//password for keystore
	//private static final String KEY_STORE_PATH = "/sdcard/test.bks";
	
	SSLEngine sslEngine;//actual SSLEngine doing all the SSL stuff, one engine per connection
	private SSLContext sslContext; 
	private ByteBuffer appOut; // clear text buffer for out  
    private ByteBuffer appIn; // clear text buffer for in  
    private ByteBuffer netOut; // encrypted buffer for out  
    private ByteBuffer netIn; // encrypted buffer for in  
    
    public boolean handshakeDone = false;
    
    private CharsetDecoder decoder = Charset.forName("UTF8").newDecoder();

    private static KeyStore keyStore;//keystore used by all engines
    private static KeyManagerFactory kmf;//ditto

    private static Set<String> certHashes = new HashSet<String>();//check if there is already a corresponding cert in keystore

    /**
     * @param context
     * @param domain hostname to be set in Common Name field of the certificate
     */
    public SSLEngineHelper(Context context,String domain){
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
        sslEngine.setNeedClientAuth(false);//do not need client authentication
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

    /**
     *
     * @param context
     * @param url protocol + host
     * @throws GeneralSecurityException
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void createSSLContext(Context context,String url) throws GeneralSecurityException, FileNotFoundException, IOException
    {
        //init keystore
        initKeyStore();

        Uri uri = Uri.parse(url);
        String domain = uri.getHost();

        //subject alternative names
        //browser will also check SAN to match the uri
        String[] sanNames = DataUtils.getSanNames(context);

        //check if there is a cert in keystore
        //if not, generate a certificate and sign it with ca on the fly, then add to keystore
        if(certHashes.contains(domain)==false){

            X509Certificate[] certChain = new X509Certificate[1];

            //use key pair to gen a signed cert,it depends on
            // 1, CertHelper has generated a ca and put it in /etc/security/cacerts (with proper hash names)
            // 2, keypair of ca in /data/data/com.handhandlab.agentdroid/files
            //java impl
            //gen key pair
            //KeyPair keyPair = CertHelper.genKeyPair();
            //X509Certificate cert = CertHelper.genCert(context,domain,keyPair,sanNames);
            //PrivateKey privateKey = keyPair.getPrivate();
            //native openssl impl
            CertWrapper certWrapper = NativeCertHelper.genCert(context,domain,sanNames);
            X509Certificate cert = certWrapper.getCert();
            PrivateKey privateKey = certWrapper.getPrivateKey();

            certChain[0] = cert;

            //an ssl keystore entry should have the private key, and the certificate (chain).
            keyStore.setEntry(domain,
                    new KeyStore.PrivateKeyEntry(privateKey, certChain),
                    new KeyStore.PasswordProtection("".toCharArray()));

            certHashes.add(domain);
        }

        //we don't need client verification, so no need for TrustStore
        //TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        //tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("TLS");
  
        sslCtx.init(kmf.getKeyManagers(), /*tmf.getTrustManagers()*/null, null);
  
        sslContext = sslCtx;  
          
    }

    /**
     * init a keystore to store the certs
     * SSLEngine will get certs from the keystore
     */
    private void initKeyStore(){
        if(keyStore==null){
            try{
                KeyStore ks = KeyStore.getInstance("BKS");

                char[] passphrase = PASSPHRASE.toCharArray();

                ks.load(null);

                kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

                kmf.init(ks, passphrase);

                keyStore = ks;

            }catch(KeyStoreException e){
                e.printStackTrace();
            }catch(NoSuchAlgorithmException e){
                e.printStackTrace();
            }catch(CertificateException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }catch(UnrecoverableKeyException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * do ssl handshaking with client
     *
     * @param sc channel connecting to the client
     * @throws IOException
     */
    public void doHandShake(SocketChannel sc) throws IOException  
    {
        sslEngine.beginHandshake();//explicitly begin the handshake
        HandshakeStatus hsStatus = sslEngine.getHandshakeStatus();
        //SSLEngine is like a state machine, doing stuff according to state switching
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
                    handshakeDone = true;
                    break;  
            }  
        }  
        Log.d("haha", "handshake done?!");
    }

    /**
     * sslEngine needs to do some long running task
     * @return
     */
    private HandshakeStatus doTask() {  
        Runnable runnable;
        if(sslEngine==null)Log.d("haha","engine null");
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
        //Log.d("haha", "new HandshakeStatus: " + hsStatus);
          
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
        //Log.d("haha", "new HandshakeStatus: " + hsStatus);
        netOut.flip();  
        return hsStatus;  
    }  
      
    //close an ssl talk, similar to the handshake steps  
    private void doSSLClose(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();  
        key.cancel();  
        sc.close();
        /*try
        {  
            sc.configureBlocking(true);  
        } catch (IOException e)  
        {
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
        }  */
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
     * read data from channel and decrypt it.
     * @param key
     * @param sc
     * @return decrypted plain text data
     * @throws IOException
     */
    public String sslRead(SelectionKey key,SocketChannel sc)throws IOException{
        netIn.clear();
        sc.read(netIn);
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
            String data = decoder.decode(appIn).toString();
            appIn.compact();
            return data;
        }
        else if(engineResult.getStatus() == SSLEngineResult.Status.CLOSED) {
            doSSLClose(key);
        }
        return null;
    }

    /**
     * encrypt the data through SSLEngine, and transfer the encrypted data to channel
     * @param sc
     * @param data plain text data
     * @throws IOException
     */
    public void sslWrite(SocketChannel sc,byte[] data) throws IOException{
        //TODO:need optimizing
        appOut = ByteBuffer.wrap(data);
        do{
            SSLEngineResult engineResult = sslEngine.wrap(appOut,netOut);
            Log.d("haha","appOut remain:"+appOut.remaining());
            doTask();
            netOut.flip();
            sc.write(netOut);
            netOut.compact();
        }while (appOut.hasRemaining());
    }
    
    public void write(SocketChannel sc,byte[] data) throws IOException{
        sslWrite(sc,data);
    	/*appOut.clear();
    	appOut.put(data);*/
        /*Log.d("haha","data len:"+data.length);
        Log.d("haha","netOut cap:"+netOut.capacity());
    	appOut = ByteBuffer.wrap(data);
        Log.d("haha","appOut cap:"+appOut.capacity());
    	SSLEngineResult engineResult = sslEngine.wrap(appOut, netOut);
        Log.d("haha","netOut remain:"+netOut.remaining());
        Log.d("haha","appOut remain:"+appOut.remaining());
        Log.d("haha", engineResult.getStatus() + "/" + engineResult.bytesConsumed() + "/"
                + engineResult.bytesProduced() + " bytes");
        doTask();
        //runDelegatedTasks(engineResult, sslEngine);  

        netOut.flip();  
        sc.write(netOut);
        netOut.compact();  */
    }

}
