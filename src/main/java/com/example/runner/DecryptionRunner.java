package com.example.runner;

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

public class DecryptionRunner {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll");
    }

    public static void run(String cascadeFilePath, String inputImagePath, String outputImagePath) throws Exception {
        Mat image = Imgcodecs.imread(inputImagePath);

        if (image.empty()) {
            throw new RuntimeException("Could not load image: " + inputImagePath);
        }

        // Load AES key
        String base64Key = Files.readString(Paths.get("src/main/resources/temp/key.txt"));
        byte[] keyBytes = AESUtil.decodeBase64(base64Key);
        SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");

        // Read metadata
        String metadataJson = Files.readString(Paths.get("src/main/resources/temp/metadata.json"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metadataList = new com.google.gson.Gson().fromJson(metadataJson, List.class);

        System.out.println("Image type: " + image.type() + " (should be " + CvType.CV_8UC3 + " for 3-channel)");
        System.out.println("Image channels: " + image.channels());

        for (Map<String, Object> region : metadataList) {
            int x = ((Double) region.get("x")).intValue();
            int y = ((Double) region.get("y")).intValue();
            int width = ((Double) region.get("width")).intValue();
            int height = ((Double) region.get("height")).intValue();
            byte[] iv = Base64.getDecoder().decode((String) region.get("iv"));

            System.out.println("Processing region: " + x + "," + y + " " + width + "x" + height);

            Rect rect = new Rect(x, y, width, height);
            Mat faceRegion = image.submat(rect);

            int numBytes = (int) (faceRegion.total() * faceRegion.channels());
            byte[] encryptedBytes = new byte[numBytes];
            faceRegion.get(0, 0, encryptedBytes);

            System.out.println("Expected bytes: " + numBytes + ", Got: " + encryptedBytes.length);

            try {
                byte[] decrypted = AESUtil.decryptBytes(encryptedBytes, aesKey, iv);

                if (decrypted.length != encryptedBytes.length) {
                    System.err.println("Length mismatch! Expected: " + encryptedBytes.length +
                            ", Got: " + decrypted.length);
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

                faceRegion.put(0, 0, decrypted);
                System.out.println("Successfully decrypted region");

            } catch (Exception e) {
                System.err.println("Decryption failed for region: " + e.getMessage());
                e.printStackTrace();
            }
        }

        boolean success = Imgcodecs.imwrite(outputImagePath, image);
        if (success) {
            System.out.println("Decryption complete. Output written to " + outputImagePath);
        } else {
            System.err.println("Failed to write output image");
        }
    }
}