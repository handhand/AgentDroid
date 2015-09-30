package com.handhandlab.agentdroid.openssl;

public class OpensslWrapper {
	
	/**
	 * @param priFolder where the private key of the CA WILL be stored
	 * @return the file name of the CA certificate
	 */
	public static native String genCA(String priFolder);
	
	/**
	 * @param folder where the private key of the CA is stored
	 * @param domain domain name used in CN
	 * @param sans Subject Alternative Names in the format of "DNS.1:xxx.com,DNS.2:yyy.com,..."
	 * @param certWrapper DER encoded bytes of the certificate and the PKCS8 encoded bytes of the private key
	 * @return DER encoded byte array
	 */
	public static native byte[] genCert(String folder,String domain,String sans,CertWrapper certWrapper);
}
