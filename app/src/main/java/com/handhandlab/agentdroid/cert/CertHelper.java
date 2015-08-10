package com.handhandlab.agentdroid.cert;

import android.content.Context;
import android.util.Log;

import com.handhandlab.agentdroid.utils.Utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.X509Extension;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERInteger;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.AuthorityKeyIdentifier;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.GeneralNames;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x509.TBSCertificateStructure;
import org.spongycastle.asn1.x509.Time;
import org.spongycastle.asn1.x509.V3TBSCertificateGenerator;
import org.spongycastle.asn1.x509.X509CertificateStructure;
import org.spongycastle.asn1.x509.X509Extensions;
import org.spongycastle.asn1.x509.X509Name;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.util.PrivateKeyFactory;
import org.spongycastle.jce.PKCS10CertificationRequest;
import org.spongycastle.jce.PrincipalUtil;
import org.spongycastle.jce.X509Principal;
import org.spongycastle.openssl.PEMReader;
import org.spongycastle.openssl.PEMWriter;
import org.spongycastle.openssl.PKCS8Generator;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.spongycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.bc.BcRSAContentSignerBuilder;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.x509.X509V3CertificateGenerator;
import org.spongycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.spongycastle.x509.extension.SubjectKeyIdentifierStructure;

import javax.security.auth.x500.X500Principal;

public class CertHelper {
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }
	
	// signers name 
	private static final String ISSUER = "CN=handhandlab.com, OU=AgentDroid, O=handhand lab, L=Canton, ST=Canton, C=CN";
	
	// subjects name - the same as we are self signed.
	private static final String SUBJECT = ISSUER;
	
	private static final String SIG_ALG = "SHA1WithRSAEncryption";
	
	private static final String CERT_PATH = "/sdcard/test_cert.pem";
    private static final String CERT_FILE = "agentdroidca";

    /**
     * generate a CA certificate
     * 1.generate keypair, and save them in app private dir
     * 2.generate certificate with the keypair, and self signed it;
     * 3.rename the generated certificate according to its hash(according to Android specification)
     *
     * @param context
     * @return the generated CA certificate
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidKeyException
     * @throws SecurityException
     * @throws SignatureException
     * @throws CertificateEncodingException
     * @throws IOException
     */
	public static String genCa(Context context)
			throws NoSuchAlgorithmException, 
			NoSuchProviderException, 
			InvalidKeyException, 
			SecurityException, 
			SignatureException, 
			CertificateEncodingException, 
			IOException 
	{
		long aDay = Utils.getOneDay();

        //generate public and private key for ca
		KeyPairGenerator caKeyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
        caKeyPairGen.initialize(1024, new SecureRandom());
        KeyPair keypair = caKeyPairGen.genKeyPair();
        //save keypair
        saveKeyPair(context.getFilesDir(),keypair,"ca");
		
        X509V3CertificateGenerator  v3CertGen = new X509V3CertificateGenerator();

        // create the certificate - version 3
        v3CertGen.setSerialNumber(BigInteger.valueOf(0x1234ABCDL));
        v3CertGen.setIssuerDN(new X509Principal(ISSUER));
        v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 30 * aDay));
        v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + 36500 * aDay));
        v3CertGen.setSubjectDN(new X509Principal(SUBJECT));
        v3CertGen.setPublicKey(keypair.getPublic());
        v3CertGen.setSignatureAlgorithm(SIG_ALG);

        // Is a CA
        v3CertGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(true));

        v3CertGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(keypair.getPublic()));

        //self signed
        X509Certificate cert = v3CertGen.generateX509Certificate(keypair.getPrivate());

        //get hash code
        int hex = X509_NAME_hash(cert.getSubjectX500Principal(),"md5");


        String certFileName = Integer.toHexString(hex)+".0";
        Log.d("haha",certFileName);

        // write to app storage
        /*File dir = context.getFilesDir();
        FileOutputStream fos = new FileOutputStream(new File(dir,certFileName));
        fos.write(cert.getEncoded());  
        fos.close();*/
        // write to app storage
        File dir = context.getFilesDir();
        //convert to pem format
        byte[] base64 = convertToBase64PEMString(cert);
        Log.d("haha","pem:"+base64);
        FileOutputStream fos = new FileOutputStream(new File(dir,certFileName));
        fos.write(base64);
        fos.flush();
        fos.close();

        return certFileName;
	}

    public static KeyPair genKeyPair()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException,NoSuchProviderException,InvalidKeyException,SignatureException{
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGen.initialize(1024, new SecureRandom());
        KeyPair keypair = keyPairGen.genKeyPair();
        return keypair;
    }

    /**
     *
     * @param context
     * @param cn Common Name to be set in the cert
     * @param keypair keypair used to generate the cert
     * @param sanNames Subject Alternative Names to be set int the cert; SAN should also be checked to match the domain by browser...if we are lucky
     * @return a certificate to be used in SSL
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NoSuchProviderException
     * @throws InvalidKeyException
     * @throws SignatureException
     * @throws CertificateException
     */
    public static X509Certificate genCert(Context context,String cn,KeyPair keypair,String[] sanNames)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException,NoSuchProviderException,InvalidKeyException,SignatureException,
                    CertificateException{

        //get ca key pair
        KeyPair caKeyPair = loadKeyPair(context,"ca");
        //get ca file
        X509Certificate caCert = null;
        caCert = readCa(context);
        Log.d("haha",caCert.getIssuerDN().getName());

        //gen key and sign
        long aDay = Utils.getOneDay();

        X509V3CertificateGenerator  v3CertGen = new X509V3CertificateGenerator();
        //
        // issuer
        //

        //
        // subjects name table.
        //
        Hashtable attrs = new Hashtable();
        Vector order = new Vector();

        attrs.put(X509Principal.C, "CN");
        attrs.put(X509Principal.O, "My");
        attrs.put(X509Principal.OU, "My");
        attrs.put(X509Principal.CN, cn);

        order.addElement(X509Principal.C);
        order.addElement(X509Principal.O);
        order.addElement(X509Principal.OU);
        order.addElement(X509Principal.CN);

        //
        // create the certificate - version 3
        //
        v3CertGen.reset();

        v3CertGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        v3CertGen.setIssuerDN(new X509Principal(ISSUER));
        v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 10*aDay));
        v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + 3650*aDay));
        v3CertGen.setSubjectDN(new X509Principal(order, attrs));
        v3CertGen.setPublicKey(keypair.getPublic());
        v3CertGen.setSignatureAlgorithm(SIG_ALG);

        //
        // extensions
        //
        v3CertGen.addExtension(
                X509Extensions.SubjectKeyIdentifier,
                false,
                new SubjectKeyIdentifierStructure(keypair.getPublic()));

        v3CertGen.addExtension(
                X509Extensions.AuthorityKeyIdentifier,
                false,
                new AuthorityKeyIdentifierStructure(caCert.getPublicKey()));

        //subject alternative names
        if(sanNames!=null){
            GeneralName[] gms = new GeneralName[sanNames.length];
            for(int i=0;i<sanNames.length;i++){
                gms[i] = new GeneralName(GeneralName.dNSName, sanNames[i]);
            }
            GeneralNames subjectAltName = new GeneralNames(gms);
            v3CertGen.addExtension(X509Extensions.SubjectAlternativeName, false, subjectAltName);
        }

        //subjectAltName = new GeneralNames(new GeneralName(GeneralName.Rfc822Name, "phil@uletide.com"));
        //subjectAltName = new GeneralNames(new GeneralName(GeneralName.DnsName, "*.whackamole.com"));

        X509Certificate cert = v3CertGen.generateX509Certificate(caKeyPair.getPrivate());
        cert.verify(caKeyPair.getPublic());

        /*X509Certificate otherCert = readOther(context);
        cert.verify(otherCert.getPublicKey());*/

        return cert;
    }

    /**
     * save private key and public key of a keypair in the directory
     *
     * @param dir
     * @param keyPair
     * @param name keys will be stored as name_private.key and name_public.key
     * @throws IOException
     */
    public static void saveKeyPair(File dir,KeyPair keyPair,String name) throws IOException{

        // Store Public Key.
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());
        FileOutputStream fos = new FileOutputStream(new File(dir, name+"_public.key"));
        fos.write(x509EncodedKeySpec.getEncoded());
        fos.close();

        // Store Private Key.
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded());
        fos = new FileOutputStream(new File(dir, name+"_private.key"));
        fos.write(pkcs8EncodedKeySpec.getEncoded());
        fos.close();
    }

    public static KeyPair loadKeyPair(Context context,String name)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Read Public Key.
        File filePublicKey = new File(context.getFilesDir(), name+"_public.key");
        FileInputStream fis = new FileInputStream(filePublicKey);
        byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
        fis.read(encodedPublicKey);
        fis.close();

        // Read Private Key.
        File filePrivateKey = new File(context.getFilesDir(), name+"_private.key");
        fis = new FileInputStream(filePrivateKey);
        byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
        fis.read(encodedPrivateKey);
        fis.close();

        // Generate KeyPair.
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return new KeyPair(publicKey, privateKey);
    }

    /**
     *
     * @param f
     * @throws Exception
     */
    private static X509Certificate readCertificate(File f) throws CertificateException,IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // Use BufferedInputStream (which supports mark and reset) so that each
        // generateCertificate call consumes one certificate.
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
        X509Certificate cert = (X509Certificate)cf.generateCertificate(in);
        in.close();
        return cert;
    }

    private static X509Certificate readCa(Context context) throws CertificateException,IOException {
        File dir = context.getFilesDir();
        return readPem(context,"1ac875a0.0");
        //return readCertificate(new File(dir,"1ac875a0.0"));
    }

    /*private static X509Certificate readOther(Context context) throws CertificateException,IOException {
        return readCertificate(new File("/sdcard/test_cert.pem"));
    }*/

    /**
     *
     * @param principal
     * @param algorithm
     * @return
     */
    private static int X509_NAME_hash(X500Principal principal, String algorithm) {
        try {
            byte[] digest = MessageDigest.getInstance(algorithm).digest(principal.getEncoded());
            return Memory.peekInt(digest, 0, ByteOrder.LITTLE_ENDIAN);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Converts a {@link X509Certificate} instance into a Base-64 encoded string (PEM format).
     *
     * @param x509Cert A X509 Certificate instance
     * @return PEM formatted String
     * @throws CertificateEncodingException
     */
    public static byte[] convertToBase64PEMString(Certificate x509Cert) throws IOException {
        /*StringWriter sw = new StringWriter();
        PEMWriter pw = new PEMWriter(sw);
        pw.writeObject(x509Cert);
        return sw.toString();*/

        try {
            ByteArrayOutputStream bao=new ByteArrayOutputStream();
            OutputStreamWriter osw=new OutputStreamWriter(bao);
            PEMWriter pw=new PEMWriter(osw);
            pw.writeObject(x509Cert);
            pw.close();
            return bao.toByteArray();
        }
        catch (  IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static X509Certificate readPem(Context context,String fileName){
        File dir = context.getFilesDir();
        X509Certificate x509=null;
        PEMReader in=null;
        try {
            in=new PEMReader(new InputStreamReader(new FileInputStream(new File(dir,fileName))));
            x509=(X509Certificate)in.readObject();
            //verify
            Log.d("haha","read pem get:"+x509.getIssuerDN().getName());

        }
        catch (  IOException e) {
            e.printStackTrace();
        }
        return x509;

    }
}
