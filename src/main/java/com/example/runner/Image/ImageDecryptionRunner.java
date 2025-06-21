package com.example.runner.Image;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import com.example.util.AESUtil;

public class ImageDecryptionRunner {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll");
    }

    public static void run(String cascadeFilePath, String inputImagePath, String outputImagePath) throws Exception {
        Mat image = Imgcodecs.imread(inputImagePath);

        if (image.empty()) {
            throw new RuntimeException("Could not load image: " + inputImagePath);
        }

        // Load AES key
        String base64Key = Files.readString(Paths.get("src/main/resources/temp/image/image_key.txt"));
        byte[] keyBytes = AESUtil.decodeBase64(base64Key);
        SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");

        // Read metadata
        String metadataJson = Files.readString(Paths.get("src/main/resources/temp/image/image_metadata.json"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metadataList = new com.google.gson.Gson().fromJson(metadataJson, List.class);

        System.out.println("Image type: " + image.type() + " (should be " + CvType.CV_8UC3 + " for 3-channel)");
        System.out.println("Image channels: " + image.channels());

        int faceCount = 0;
        int textCount = 0;
        int successfulDecryptions = 0;

        for (Map<String, Object> region : metadataList) {
            int x = ((Double) region.get("x")).intValue();
            int y = ((Double) region.get("y")).intValue();
            int width = ((Double) region.get("width")).intValue();
            int height = ((Double) region.get("height")).intValue();
            byte[] iv = Base64.getDecoder().decode((String) region.get("iv"));

            // Get region type (default to "face" for backward compatibility)
            String regionType = region.containsKey("type") ? (String) region.get("type") : "face";

            if ("face".equals(regionType)) {
                faceCount++;
            } else if ("text".equals(regionType)) {
                textCount++;
            }

            System.out.println("Processing " + regionType + " region: " + x + "," + y + " " + width + "x" + height);

            Rect rect = new Rect(x, y, width, height);
            Mat regionMat = image.submat(rect);

            int numBytes = (int) (regionMat.total() * regionMat.channels());
            byte[] encryptedBytes = new byte[numBytes];
            regionMat.get(0, 0, encryptedBytes);

            System.out.println("Expected bytes: " + numBytes + ", Got: " + encryptedBytes.length);

            try {
                byte[] decrypted = AESUtil.decryptBytes(encryptedBytes, aesKey, iv);

                if (decrypted.length != encryptedBytes.length) {
                    System.err.println("Length mismatch for " + regionType + "! Expected: " +
                            encryptedBytes.length + ", Got: " + decrypted.length);
                    continue;
                }

                // Validate and clamp pixel values to 0-255 range
                for (int i = 0; i < decrypted.length; i++) {
                    int value = decrypted[i] & 0xFF; // Convert to unsigned
                    if (value < 0)
                        value = 0;
                    if (value > 255)
                        value = 255;
                    decrypted[i] = (byte) value;
                }

                regionMat.put(0, 0, decrypted);
                System.out.println("Successfully decrypted " + regionType + " region");
                successfulDecryptions++;

            } catch (Exception e) {
                System.err.println("Decryption failed for " + regionType + " region: " + e.getMessage());
                e.printStackTrace();
            }
        }

        boolean success = Imgcodecs.imwrite(outputImagePath, image);
        if (success) {
            System.out.println("Decryption complete. Output written to " + outputImagePath);
            System.out.println("Summary:");
            System.out.println("  - Face regions decrypted: " + faceCount);
            System.out.println("  - Text regions decrypted: " + textCount);
            System.out
                    .println("  - Total successful decryptions: " + successfulDecryptions + "/" + metadataList.size());
        } else {
            System.err.println("Failed to write output image");
        }
    }
}