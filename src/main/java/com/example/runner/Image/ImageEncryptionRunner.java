package com.example.runner.Image;

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
import com.example.detector.TextDetector;
import com.example.util.AESUtil;

public class ImageEncryptionRunner {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll");
    }

    public static void run(String cascadeFilePath, String inputImagePath, String outputImagePath) throws Exception {
        // Initialize detectors
        FaceDetector faceDetector = new FaceDetector(cascadeFilePath);
        TextDetector textDetector = new TextDetector(cascadeFilePath);

        // Detect faces and text
        List<Rect> faces = faceDetector.detectFaces(inputImagePath);
        List<Rect> textRegions = textDetector.detectTexts(inputImagePath);

        System.out.println("[Encryption] Faces detected: " + faces.size());
        System.out.println("[Encryption] Text regions detected: " + textRegions.size());

        Mat image = Imgcodecs.imread(inputImagePath);

        if (image.empty()) {
            throw new RuntimeException("Could not load image: " + inputImagePath);
        }

        System.out.println("Original image type: " + image.type() + " (should be " + CvType.CV_8UC3 + ")");
        System.out.println("Original image channels: " + image.channels());

        SecretKey aesKey = AESUtil.generateKey();
        String base64Key = Base64.getEncoder().encodeToString(aesKey.getEncoded());
        Files.writeString(Paths.get("src/main/resources/temp/image/image_key.txt"), base64Key);

        // Store region metadata
        List<Map<String, Object>> metadataList = new ArrayList<>();

        // Process face regions
        for (Rect rect : faces) {
            System.out.println("[Encryption] Processing face at: " + rect.x + "," + rect.y +
                    " size: " + rect.width + "x" + rect.height);

            Map<String, Object> regionData = encryptRegion(image, rect, aesKey, "face");
            if (regionData != null) {
                metadataList.add(regionData);
            }
        }

        // Process text regions
        for (Rect rect : textRegions) {
            System.out.println("[Encryption] Processing text at: " + rect.x + "," + rect.y +
                    " size: " + rect.width + "x" + rect.height);

            Map<String, Object> regionData = encryptRegion(image, rect, aesKey, "text");
            if (regionData != null) {
                metadataList.add(regionData);
            }
        }

        boolean success = Imgcodecs.imwrite(outputImagePath, image);
        if (!success) {
            throw new RuntimeException("Failed to write encrypted image");
        }

        // Save metadata to JSON file
        FileWriter writer = new FileWriter("src/main/resources/temp/image/image_metadata.json");
        writer.write(new com.google.gson.Gson().toJson(metadataList));
        writer.close();

        System.out.println("[Encryption] Encrypted image saved to: " + outputImagePath);
        System.out.println("[Encryption] Successfully encrypted " + faces.size() + " face regions");
        System.out.println("[Encryption] Successfully encrypted " + textRegions.size() + " text regions");
        System.out.println("[Encryption] Total encrypted regions: " + metadataList.size());
    }

    /**
     * Encrypt a single region (face or text) and return metadata
     */
    private static Map<String, Object> encryptRegion(Mat image, Rect rect, SecretKey aesKey, String regionType) {
        try {
            Mat region = image.submat(rect);
            int numBytes = (int) (region.total() * region.channels());
            byte[] regionBytes = new byte[numBytes];
            region.get(0, 0, regionBytes);

            System.out.println("[Encryption] Original " + regionType + " bytes length: " + regionBytes.length);

            // Create checksum for validation
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] originalHash = md.digest(regionBytes);

            byte[] iv = AESUtil.generateIV();
            byte[] encrypted = AESUtil.encryptBytes(regionBytes, aesKey, iv);

            System.out.println("[Encryption] Encrypted " + regionType + " bytes length: " + encrypted.length);

            if (encrypted.length != regionBytes.length) {
                System.err.println("[Encryption] Length mismatch for " + regionType + "! Original: " +
                        regionBytes.length + ", Encrypted: " + encrypted.length);
                return null;
            }

            // Verify encryption worked by testing decryption
            try {
                byte[] testDecrypt = AESUtil.decryptBytes(encrypted, aesKey, iv);
                byte[] testHash = md.digest(testDecrypt);

                if (!MessageDigest.isEqual(originalHash, testHash)) {
                    System.err.println("[Encryption] Validation failed for " + regionType + " at " + rect);
                    return null;
                }
                System.out.println("[Encryption] Validation passed for " + regionType + " at " + rect);
            } catch (Exception e) {
                System.err.println("[Encryption] Test decryption failed for " + regionType + ": " + e.getMessage());
                return null;
            }

            region.put(0, 0, encrypted);

            Map<String, Object> regionData = new HashMap<>();
            regionData.put("x", rect.x);
            regionData.put("y", rect.y);
            regionData.put("width", rect.width);
            regionData.put("height", rect.height);
            regionData.put("iv", Base64.getEncoder().encodeToString(iv));
            regionData.put("originalHash", Base64.getEncoder().encodeToString(originalHash));
            regionData.put("type", regionType); // Add type field to distinguish between face and text

            return regionData;

        } catch (Exception e) {
            System.err.println("[Encryption] Failed to encrypt " + regionType + " region: " + e.getMessage());
            return null;
        }
    }
}