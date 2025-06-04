package com.example.runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import com.example.util.AESUtil;

public class DecryptionRunner {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll");
    }

    public static void run(String cascadeFilePath, String encryptedImagePath, String outputImagePath) throws Exception {
        // Load AES key and IV
        String keyBase64 = Files.readString(Paths.get("key.txt"));
        String ivBase64 = Files.readString(Paths.get("iv.txt"));
        SecretKey aesKey = AESUtil.loadKeyFromBase64(keyBase64);
        IvParameterSpec iv = AESUtil.loadIVFromBase64(ivBase64);

        // Load face coordinates instead of detecting faces in encrypted image
        List<Rect> faces = loadFaceCoordinates();
        System.out.println("[Decryption] Loaded face coordinates: " + faces.size());

        Mat image = Imgcodecs.imread(encryptedImagePath);
        System.out.println("[Decryption] Image info - Rows: " + image.rows() +
                ", Cols: " + image.cols() + ", Type: " + image.type() +
                ", Channels: " + image.channels());

        for (int i = 0; i < faces.size(); i++) {
            Rect r = faces.get(i);
            System.out.println("[Decryption] Processing face " + (i + 1) + ": " +
                    "x=" + r.x + ", y=" + r.y + ", w=" + r.width + ", h=" + r.height);

            Mat faceRegion = new Mat(image, r);

            // Debug: Save encrypted face region before decryption
            Imgcodecs.imwrite("debug_face_" + i + "_before_decrypt.jpg", faceRegion);

            int numBytes = (int) (faceRegion.total() * faceRegion.channels());
            byte[] encryptedPixels = new byte[numBytes];
            faceRegion.get(0, 0, encryptedPixels);

            System.out.println(
                    "[Decryption] Face " + (i + 1) + " encrypted pixel data length: " + encryptedPixels.length);

            // Print first few encrypted values for debugging
            System.out.print("[Decryption] Encrypted pixels (first 10): ");
            for (int j = 0; j < Math.min(10, encryptedPixels.length); j++) {
                System.out.print((encryptedPixels[j] & 0xFF) + " ");
            }
            System.out.println();

            byte[] decrypted = AESUtil.decryptBytes(encryptedPixels, aesKey, iv);

            if (decrypted.length == encryptedPixels.length) {
                // Print first few decrypted values for debugging
                System.out.print("[Decryption] Decrypted pixels (first 10): ");
                for (int j = 0; j < Math.min(10, decrypted.length); j++) {
                    System.out.print((decrypted[j] & 0xFF) + " ");
                }
                System.out.println();

                // Validate pixel values are in correct range
                byte[] validatedPixels = AESUtil.validatePixelValues(decrypted);

                faceRegion.put(0, 0, validatedPixels);

                // Debug: Save decrypted face region
                Imgcodecs.imwrite("debug_face_" + i + "_after_decrypt.jpg", faceRegion);

                System.out.println("[Decryption] Face " + (i + 1) + " decrypted successfully");
            } else {
                System.out.println("[Decryption] Face " + (i + 1) + " - Length mismatch! Encrypted: " +
                        encryptedPixels.length + ", Decrypted: " + decrypted.length);
            }
        }

        Imgcodecs.imwrite(outputImagePath, image);
        System.out.println("[Decryption] Decrypted image saved to: " + outputImagePath);
    }

    private static List<Rect> loadFaceCoordinates() throws IOException {
        List<Rect> faces = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("face_coordinates.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int width = Integer.parseInt(parts[2]);
                    int height = Integer.parseInt(parts[3]);
                    faces.add(new Rect(x, y, width, height));
                    System.out.println("[Decryption] Loaded face coordinates: x=" + x +
                            ", y=" + y + ", w=" + width + ", h=" + height);
                }
            }
        }
        return faces;
    }
}