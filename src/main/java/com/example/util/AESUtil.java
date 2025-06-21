package com.example.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_CTR_NO_PADDING = "AES/CTR/NoPadding";
    private static final int AES_KEY_SIZE = 128;
    private static final int IV_SIZE = 16;
    private static final String DEFAULT_STRING_KEY = "0123456789abcdef";

    // for byte-level encryption (files, images, videos, etc.)
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }

    public static byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static byte[] encryptBytes(byte[] input, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(input);
    }

    public static byte[] decryptBytes(byte[] input, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(input);
    }

    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] decodeBase64(String data) {
        return Base64.getDecoder().decode(data);
    }

    // public static String encryptString(String plainText, SecretKey key) throws
    // Exception {
    // Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
    // cipher.init(Cipher.ENCRYPT_MODE, key);

    // byte[] encyptedData = cipher.doFinal(plainText.getBytes());

    // return Base64.getEncoder().encodeToString(encyptedData);
    // }

    // public static String decryptString(String cipherText, SecretKey key) throws
    // Exception {
    // Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
    // cipher.init(Cipher.DECRYPT_MODE, key);

    // byte[] decodedData = Base64.getDecoder().decode(cipherText);
    // byte[] decryptedData = cipher.doFinal(decodedData);

    // return new String(decryptedData, StandardCharsets.UTF_8);
    // }

    // for string region encryption (text, etc.)
    public static String encryptString(String plainText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
        SecretKeySpec keySpec = new SecretKeySpec(DEFAULT_STRING_KEY.getBytes(StandardCharsets.UTF_8),
                AES_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(new byte[IV_SIZE]); // Static IV
        // (simple for demo)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return encodeBase64(encrypted);
    }

    public static String decryptString(String cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
        SecretKeySpec keySpec = new SecretKeySpec(DEFAULT_STRING_KEY.getBytes(StandardCharsets.UTF_8),
                AES_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(new byte[IV_SIZE]);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decoded = decodeBase64(cipherText);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
