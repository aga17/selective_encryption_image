package com.example.runner.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import java.util.regex.MatchResult;

import javax.crypto.SecretKey;

import com.example.detector.SensitiveDataDetector;
import com.example.util.AESUtil;
import com.example.util.ECCUtil;

public class TextEncryptionRunner {
    public static void run(String inputPath, String outputFilePath) throws Exception {
        String content = Files.readString(Paths.get(inputPath));

        // generate ECC and AES keys
        SecretKey aesKey = AESUtil.generateKey();
        KeyPair eccKeyPair = ECCUtil.generateKeyPair();

        // encrpyt AES key with ECC public key
        byte[] encryptedAESKey = ECCUtil.encryptAESKey(aesKey, eccKeyPair.getPublic());
        String encryptedAESKeyBase64 = Base64.getEncoder().encodeToString(encryptedAESKey);

        List<MatchResult> matches = SensitiveDataDetector.detectSensitiveData(content);
        if (matches.isEmpty()) {
            System.out.println("No sensitive data found in the text.");
            return;
        }

        StringBuilder encryptedContent = new StringBuilder(content);
        int offset = 0;
        for (MatchResult match : matches) {
            String sensitiveText = match.group();
            String encryptedText = AESUtil.encryptString(sensitiveText, aesKey);
            String replacement = "{AES_ENCRYPTED:" + encryptedText + "}";

            encryptedContent.replace(match.start() + offset, match.end() + offset, replacement);
            offset += replacement.length() - sensitiveText.length();
        }
        // Files.writeString(Path.of(outputFilePath), encryptedContent.toString());
        // System.out.println("Encryption completed.");

        // 4. Write ECC Encrypted AES Key + Content
        String finalOutput = "ENCRYPTED_AES_KEY:" + encryptedAESKeyBase64 + "\n" + encryptedContent.toString();
        Files.writeString(Path.of(outputFilePath), finalOutput);

        // For demo, also save private key for decryption
        Files.writeString(Path.of("src\\main\\resources\\temp\\text\\private_key.txt"),
                Base64.getEncoder().encodeToString(eccKeyPair.getPrivate().getEncoded()));

        System.out.println("Encryption completed with ECC-AES Hybrid.");
    }
}
