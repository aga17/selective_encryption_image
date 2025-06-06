package com.example.runner;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import com.example.detector.FaceDetector;
import com.example.util.AESUtil;

public class EncryptionRunner {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll");
    }

    public static void run(String cascadeFilePath, String inputImagePath, String outputImagePath) throws Exception {
        FaceDetector detector = new FaceDetector(cascadeFilePath);
        List<Rect> faces = detector.detectFaces(inputImagePath);
        System.out.println("[Encryption] Faces detected: " + faces.size());

        Mat image = Imgcodecs.imread(inputImagePath);

        if (image.empty()) {
            throw new RuntimeException("Could not load image: " + inputImagePath);
        }

        System.out.println("Original image type: " + image.type() + " (should be " + CvType.CV_8UC3 + ")");
        System.out.println("Original image channels: " + image.channels());

        SecretKey aesKey = AESUtil.generateKey();
        String base64Key = Base64.getEncoder().encodeToString(aesKey.getEncoded());
        Files.writeString(Paths.get("src/main/resources/temp/key.txt"), base64Key);

        // Store region metadata
        List<Map<String, Object>> metadataList = new ArrayList<>();

        for (Rect rect : faces) {
            System.out.println("[Encryption] Processing face at: " + rect.x + "," + rect.y +
                    " size: " + rect.width + "x" + rect.height);

            Mat faceRegion = image.submat(rect);
            int numBytes = (int) (faceRegion.total() * faceRegion.channels());
            byte[] faceBytes = new byte[numBytes];
            faceRegion.get(0, 0, faceBytes);

            System.out.println("[Encryption] Original bytes length: " + faceBytes.length);

            // Create checksum for validation
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] originalHash = md.digest(faceBytes);

            byte[] iv = AESUtil.generateIV();
            byte[] encrypted = AESUtil.encryptBytes(faceBytes, aesKey, iv);

            System.out.println("[Encryption] Encrypted bytes length: " + encrypted.length);

            if (encrypted.length != faceBytes.length) {
                System.err.println("[Encryption] Length mismatch! Original: " + faceBytes.length +
                        ", Encrypted: " + encrypted.length);
                continue;
            }

            // Verify encryption worked by testing decryption
            try {
                byte[] testDecrypt = AESUtil.decryptBytes(encrypted, aesKey, iv);
                byte[] testHash = md.digest(testDecrypt);

                if (!MessageDigest.isEqual(originalHash, testHash)) {
                    System.err.println("[Encryption] Validation failed for face at " + rect);
                    continue;
                }
                System.out.println("[Encryption] Validation passed for face at " + rect);
            } catch (Exception e) {
                System.err.println("[Encryption] Test decryption failed: " + e.getMessage());
                continue;
            }

            faceRegion.put(0, 0, encrypted);

            Map<String, Object> regionData = new HashMap<>();
            regionData.put("x", rect.x);
            regionData.put("y", rect.y);
            regionData.put("width", rect.width);
            regionData.put("height", rect.height);
            regionData.put("iv", Base64.getEncoder().encodeToString(iv));
            regionData.put("originalHash", Base64.getEncoder().encodeToString(originalHash));

            metadataList.add(regionData);
        }

        boolean success = Imgcodecs.imwrite(outputImagePath, image);
        if (!success) {
            throw new RuntimeException("Failed to write encrypted image");
        }

        // Save metadata to JSON file
        FileWriter writer = new FileWriter("src/main/resources/temp/metadata.json");
        writer.write(new com.google.gson.Gson().toJson(metadataList));
        writer.close();

        System.out.println("[Encryption] Encrypted image saved to: " + outputImagePath);
        System.out.println("[Encryption] Successfully encrypted " + metadataList.size() + " face regions");
    }
}