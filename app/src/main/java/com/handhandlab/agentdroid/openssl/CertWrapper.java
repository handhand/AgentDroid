package com.handhandlab.agentdroid.openssl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class CertWrapper {
	byte[] certBytes;
	byte[] keyBytes;
	public byte[] getCertBytes() {
		return certBytes;
	}
	public void setCertBytes(byte[] certBytes) {
		this.certBytes = certBytes;
	}
	public byte[] getKeyBytes() {
		return keyBytes;
	}
	public void setKeyBytes(byte[] keyBytes) {
		this.keyBytes = keyBytes;
	}
    public X509Certificate getCert(){
        try{
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream in = new ByteArrayInputStream(certBytes);
            X509Certificate cert = (X509Certificate)certFactory.generateCertificate(in);
            return cert;
        }catch(CertificateException e){
            e.printStackTrace();
        }
        return null;
    }
    public PrivateKey getPrivateKey(){
        try{
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
            return privateKey;
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }catch (InvalidKeySpecException e){
            e.printStackTrace();
        }
        return null;
    }
}
