package com.paypal.android.sdk.onetouch.core.encryption;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;

/**
 * Very basic string manipulation methods useful for encryption.
 */
public class EncryptionUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

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
     * Returns String of byte array.
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

        return hexString.toString().toUpperCase(Locale.ROOT);
    }

    /**
     * Returns byte array of hex string.
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
     * Checks array equality in a way that avoids timing attacks.
     *
     * @param arrayOne
     * @param arrayTwo
     * @return {@code true} if equal, {@code false} otherwise.
     * @see <a href="http://codahale.com/a-lesson-in-timing-attacks">http://codahale.com/a-lesson-in-timing-attacks/</a>
     */
    static boolean isEqual(byte[] arrayOne, byte[] arrayTwo) {
        if (arrayOne.length != arrayTwo.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < arrayOne.length; i++) {
            result |= arrayOne[i] ^ arrayTwo[i];
        }
        return result == 0;
    }
}
