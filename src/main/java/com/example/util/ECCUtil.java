package com.example.util;

import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import java.security.KeyPair;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class ECCUtil {
    public static KeyPair generateKeyPair() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        return keyGen.generateKeyPair(); // 256-bit key size
    }

    public static byte[] encryptAESKey(SecretKey aesKey, PublicKey publicKey) throws Exception {
        Cipher eccCipher = Cipher.getInstance("ECIES");
        eccCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return eccCipher.doFinal(aesKey.getEncoded());
    }

    public static SecretKey decryptAESKey(byte[] encryptedKey, PrivateKey privateKey) throws Exception {
        Cipher eccCipher = Cipher.getInstance("ECIES");
        eccCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decodedKey = eccCipher.doFinal(encryptedKey);
        return new javax.crypto.spec.SecretKeySpec(decodedKey, "AES");
    }
}
