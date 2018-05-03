package com.zenobase.oauth;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Handles out-of-order certificate chains. Obsolete with Java 7u40?
 * Based on https://github.com/rfreedman/android-ssl.
 */
public class CustomX509TrustManager implements X509TrustManager {

    private final X509TrustManager trustManager;

    public CustomX509TrustManager(X509TrustManager trustManager) {
    	this.trustManager = trustManager;
    }

    @Override
	public X509Certificate[] getAcceptedIssuers() {
        return trustManager.getAcceptedIssuers();
    }

    @Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
    	trustManager.checkClientTrusted(chain, authType);
    }

    /**
     * Given the partial or complete certificate chain provided by the peer,
     * build a certificate path to a trusted root and return if it can be validated and is trusted
     * for client SSL authentication based on the authentication type. The authentication type is
     * determined by the actual certificate used. For instance, if RSAPublicKey is used, the authType should be "RSA".
     * Checking is case-sensitive.
     * Defers to the default trust manager first, checks the cert supplied in the ctor if that fails.
     * @param chain the server's certificate chain
     * @param authType the authentication type based on the client certificate
     */
    @Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
    	trustManager.checkServerTrusted(reorderCertificateChain(chain), authType);
    }

    /**
     * Puts the certificate chain in the proper order, to deal with out-of-order
     * certificate chains as are sometimes produced by Apache's mod_ssl
     * @param chain the certificate chain, possibly with bad ordering
     * @return the re-ordered certificate chain
     */
    private X509Certificate[] reorderCertificateChain(X509Certificate[] chain) {

        X509Certificate[] reorderedChain = new X509Certificate[chain.length];
        List<X509Certificate> certificates = Arrays.asList(chain);

        int position = chain.length - 1;
        X509Certificate rootCert = findRootCert(certificates);
        reorderedChain[position] = rootCert;

        X509Certificate cert = rootCert;
        while((cert = findSignedCert(cert, certificates)) != null && position > 0) {
            reorderedChain[--position] = cert;
        }

        return reorderedChain;
    }

    /**
     * A helper method for certificate re-ordering.
     * Finds the root certificate in a possibly out-of-order certificate chain.
     * @param certificates the certificate change, possibly out-of-order
     * @return the root certificate, if any, that was found in the list of certificates
     */
    private X509Certificate findRootCert(List<X509Certificate> certificates) {
        X509Certificate rootCert = null;

        for(X509Certificate cert : certificates) {
            X509Certificate signer = findSigner(cert, certificates);
            if(signer == null || signer.equals(cert)) { // no signer present, or self-signed
                rootCert = cert;
                break;
            }
        }

        return rootCert;
    }

    /**
     * A helper method for certificate re-ordering.
     * Finds the first certificate in the list of certificates that is signed by the sigingCert.
     */
    private X509Certificate findSignedCert(X509Certificate signingCert, List<X509Certificate> certificates) {
        X509Certificate signed = null;

        for(X509Certificate cert : certificates) {
            Principal signingCertSubjectDN = signingCert.getSubjectDN();
            Principal certIssuerDN = cert.getIssuerDN();
            if(certIssuerDN.equals(signingCertSubjectDN) && !cert.equals(signingCert)) {
                signed = cert;
                break;
            }
        }

        return signed;
    }

    /**
     * A helper method for certificate re-ordering.
     * Finds the certificate in the list of certificates that signed the signedCert.
     */
    private X509Certificate findSigner(X509Certificate signedCert, List<X509Certificate> certificates) {
        X509Certificate signer = null;

        for(X509Certificate cert : certificates) {
            Principal certSubjectDN = cert.getSubjectDN();
            Principal issuerDN = signedCert.getIssuerDN();
            if(certSubjectDN.equals(issuerDN)) {
                signer = cert;
                break;
            }
        }

        return signer;
    }


	public static void setDefault() {
		try {
			TrustManagerFactory factory = TrustManagerFactory.getInstance("X509");
			factory.init((KeyStore) null);
			X509TrustManager trustManager = (X509TrustManager) factory.getTrustManagers()[0];
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, new TrustManager[] { new CustomX509TrustManager(trustManager) }, null);
			SSLContext.setDefault(context);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Couldn't find trust default manager", e);
		}
	}
}
