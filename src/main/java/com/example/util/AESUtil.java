package com.example.util;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class AESUtil {
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }

    public static byte[] generateIV() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    // Improved method for encrypting image regions
    public static byte[] encryptImageRegion(Mat region, SecretKey key, byte[] iv) throws Exception {
        // Store original image properties
        int rows = region.rows();
        int cols = region.cols();
        int type = region.type();
        int channels = region.channels();

        System.out.println("[Encryption] Region info - Rows: " + rows + ", Cols: " + cols +
                ", Type: " + type + ", Channels: " + channels);

        // Convert to byte array
        int numBytes = (int) (region.total() * region.elemSize());
        byte[] imageData = new byte[numBytes];
        region.get(0, 0, imageData);

        // Encrypt the data
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(imageData);

        System.out.println("[Encryption] Original size: " + imageData.length +
                ", Encrypted size: " + encrypted.length);

        return encrypted;
    }

    // Improved method for decrypting image regions
    public static byte[] decryptImageRegion(Mat region, SecretKey key, IvParameterSpec iv) throws Exception {
        // Get encrypted data from the region
        int numBytes = (int) (region.total() * region.elemSize());
        byte[] encryptedData = new byte[numBytes];
        region.get(0, 0, encryptedData);

        // Decrypt the data
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] decrypted = cipher.doFinal(encryptedData);

        System.out.println("[Decryption] Encrypted size: " + encryptedData.length +
                ", Decrypted size: " + decrypted.length);

        return decrypted;
    }

    // Original methods for backward compatibility
    public static byte[] encryptBytes(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    public static byte[] decryptBytes(byte[] encryptedData, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return cipher.doFinal(encryptedData);
    }

    public static SecretKey loadKeyFromBase64(String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decoded, 0, decoded.length, "AES");
    }

    public static IvParameterSpec loadIVFromBase64(String base64IV) {
        byte[] decoded = Base64.getDecoder().decode(base64IV);
        return new IvParameterSpec(decoded);
    }

    // Method to validate pixel values after decryption
    public static byte[] validatePixelValues(byte[] pixelData) {
        byte[] validated = new byte[pixelData.length];
        for (int i = 0; i < pixelData.length; i++) {
            // Ensure values are in valid range [0, 255]
            int value = pixelData[i] & 0xFF;
            validated[i] = (byte) Math.max(0, Math.min(255, value));
        }
        return validated;
    }
}