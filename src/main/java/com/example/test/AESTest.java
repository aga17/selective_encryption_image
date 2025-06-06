package com.example.test;

import com.example.util.AESUtil;
import javax.crypto.SecretKey;
import java.util.Arrays;

public class AESTest {
    public static void main(String[] args) throws Exception {
        // Test AES encryption/decryption with simple byte array
        byte[] testData = new byte[256]; // Simulate image data
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i % 256); // Fill with values 0-255
        }

        SecretKey key = AESUtil.generateKey();
        byte[] iv = AESUtil.generateIV();

        System.out.println("Original data length: " + testData.length);

        byte[] encrypted = AESUtil.encryptBytes(testData, key, iv);
        System.out.println("Encrypted data length: " + encrypted.length);

        byte[] decrypted = AESUtil.decryptBytes(encrypted, key, iv);
        System.out.println("Decrypted data length: " + decrypted.length);

        boolean isEqual = Arrays.equals(testData, decrypted);
        System.out.println("Data integrity check: " + (isEqual ? "PASSED" : "FAILED"));

        if (!isEqual) {
            System.out.println("First few bytes comparison:");
            for (int i = 0; i < Math.min(20, testData.length); i++) {
                System.out.printf("Index %d: Original=%d, Decrypted=%d%n",
                        i, testData[i] & 0xFF, decrypted[i] & 0xFF);
            }
        }
    }
}