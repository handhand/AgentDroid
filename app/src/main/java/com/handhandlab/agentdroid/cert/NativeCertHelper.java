package com.handhandlab.agentdroid.cert;

import android.content.Context;
import android.util.Log;

import com.handhandlab.agentdroid.openssl.CertWrapper;
import com.handhandlab.agentdroid.openssl.OpensslWrapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Created by Handhand on 2015/9/29.
 */
public class NativeCertHelper {
    /**
     * native Openssl implementation of CertHelper.genCa();
     * generate a CA certificate and return the file name in hash format(Android specs)
     * private key is stored in app private directory;
     *
     * @param context
     */
    public static String genCa(Context context) {
        String folder = context.getFilesDir().getAbsolutePath() + "/";
        return OpensslWrapper.genCA(folder);
    }

    /**
     * native Openssl implementation of CertHelper.getCert();
     *
     * @param context
     * @param cn      Common Name to be set in the cert
     * @param sans    Subject Alternative Names to be set int the cert; SAN should also be checked to match the domain by browser...if we are lucky
     * @return a certificate to be used in SSL
     */
    public static CertWrapper genCert(Context context, String cn, String[] sans) {

        int i = 1;
        StringBuilder sb = new StringBuilder();
        for (String san : sans) {
            sb.append("DNS.").append(i++).append(":").append(san).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);//delete the last ","

        String folder = context.getFilesDir().getAbsolutePath() + "/";

        CertWrapper certWrapper  = new CertWrapper();
        byte[] derEncodedCert = OpensslWrapper.genCert(folder, cn, sb.toString(),certWrapper);

        return certWrapper;

    }


}
