package com.example.runner.Text;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.util.AESUtil;

public class TextDecryptionRunner {
    public static void run(String inputPath, String outputFilePath) throws Exception {
        String content = Files.readString(Paths.get(inputPath));
        Pattern pattern = Pattern.compile("\\{AES_ENCRYPTED:(.+?)\\}");
        Matcher matcher = pattern.matcher(content);

        StringBuffer decryptedContent = new StringBuffer();
        while (matcher.find()) {
            String encryptedPart = matcher.group(1);
            String decrypted = AESUtil.decryptString(encryptedPart);
            matcher.appendReplacement(decryptedContent, decrypted);

        }
        matcher.appendTail(decryptedContent);

        // Write the decrypted content to the output file
        Files.writeString(Paths.get(outputFilePath), decryptedContent.toString());
        System.out.println("Decryption completed.");
    }

}
