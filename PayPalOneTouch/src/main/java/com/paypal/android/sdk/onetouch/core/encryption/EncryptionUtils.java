package com.paypal.android.sdk.onetouch.core.encryption;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Very basic string manipulation methods useful for encryption.
 */
public class EncryptionUtils {
    private static final SecureRandom RANDOM;

    static {
        PRNGFixes.apply();
        RANDOM = new SecureRandom();
    }

    public static byte[] generateRandomData(int size) {
        byte[] output = new byte[size];
        RANDOM.nextBytes(output);
        return output;
    }

    public static X509Certificate getX509CertificateFromBase64String(String certificateBase64)
            throws CertificateException {

        byte[] certificate = Base64.decode(certificateBase64, Base64.DEFAULT);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory
                .generateCertificate(new ByteArrayInputStream(certificate));
    }

    /**
     * Returns String of byte array.  Did not use library as none are built into Android.
     *
     * @param array
     * @return
     */
    public static String byteArrayToHexString(byte[] array) {
        if (null == array) return null;
        StringBuilder hexString = new StringBuilder();
        for (byte b : array) {
            int intVal = b & 0xff;
            if (intVal < 0x10) {
                hexString.append("0");
            }
            hexString.append(Integer.toHexString(intVal));
        }

        return hexString.toString().toUpperCase();
    }

    /**
     * Returns byte array of hex string.  Did not use library as none are built into Android.
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Do not use Arrays.equals because of http://codahale.com/a-lesson-in-timing-attacks/
     *
     * @param a
     * @param b
     * @return
     */
    static boolean isEqual(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
