package com.paypal.android.sdk.onetouch.core.encryption;

import com.paypal.android.sdk.onetouch.core.exception.InvalidEncryptionDataException;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class OtcCrypto {

    private static final int ENCRYPTION_KEY_SIZE = 32;
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int NONCE_SIZE = 16;
    private static final String AES_CTR_ALGO = "AES/CTR/NoPadding";
    private static final String RSA_ALGO = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
    private static final int AES_KEY_SIZE = 16;
    private static final int DIGEST_SIZE = 32;
    private static final int PUBLIC_KEY_SIZE = 256;
    private static final int MAX_RSA_ENCRYPTABLE_BYTES = 214;

    private byte[] dataDigest(byte[] data, byte[] key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256HMAC = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec digestKey = new SecretKeySpec(key, HMAC_SHA256);
        sha256HMAC.init(digestKey);
        return sha256HMAC.doFinal(data);
    }

    public byte[] generateRandom256BitKey() {
        return EncryptionUtils.generateRandomData(ENCRYPTION_KEY_SIZE);
    }

    public byte[] encryptRSAData(byte[] plainData, Certificate certificate)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException, InvalidEncryptionDataException {
        if (plainData.length > MAX_RSA_ENCRYPTABLE_BYTES) {
            throw new InvalidEncryptionDataException("Data is too large for public key encryption: " +
                    plainData.length + " > " + MAX_RSA_ENCRYPTABLE_BYTES);
        }

        PublicKey publicKey = certificate.getPublicKey();

        Cipher rsaCipher = Cipher.getInstance(RSA_ALGO);
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return rsaCipher.doFinal(plainData);
    }

    public byte[] decryptAESCTRData(byte[] cipherData, byte[] key)
            throws IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException,
            IllegalArgumentException, InvalidAlgorithmParameterException, NoSuchPaddingException,
            BadPaddingException, InvalidEncryptionDataException {
        // we should have at least 1 byte of data
        if (cipherData.length < DIGEST_SIZE + NONCE_SIZE) {
            throw new InvalidEncryptionDataException("data is too small");
        }
        // first 16 bytes is encryption key, 2nd 16 bytes is digest key
        byte[] encryptionKey = new byte[AES_KEY_SIZE];
        System.arraycopy(key, 0, encryptionKey, 0, AES_KEY_SIZE);
        byte[] digestKey = new byte[AES_KEY_SIZE];
        System.arraycopy(key, AES_KEY_SIZE, digestKey, 0, AES_KEY_SIZE);

        // extract signature it is 32 bytes
        byte[] signature = new byte[DIGEST_SIZE];
        System.arraycopy(cipherData, 0, signature, 0, DIGEST_SIZE);

        // extract the rest to calculate digest and compare it to the signature
        byte[] signedData = new byte[cipherData.length - DIGEST_SIZE];
        System.arraycopy(cipherData, DIGEST_SIZE, signedData, 0, cipherData.length - DIGEST_SIZE);
        byte[] digest = dataDigest(signedData, digestKey);
        if (!EncryptionUtils.isEqual(digest, signature)) {
            throw new IllegalArgumentException("Signature mismatch");
        }

        // read nonce
        byte[] nonceData = new byte[NONCE_SIZE];
        System.arraycopy(signedData, 0, nonceData, 0, NONCE_SIZE);

        // init nonce and decrypt
        IvParameterSpec nonceSpec = new IvParameterSpec(nonceData);
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, "AES");

        Cipher cipher = Cipher.getInstance(AES_CTR_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, nonceSpec);
        return cipher.doFinal(signedData, NONCE_SIZE, signedData.length - NONCE_SIZE);
    }
}
