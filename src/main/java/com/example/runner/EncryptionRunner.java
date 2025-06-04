package com.example.runner;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.example.detector.FaceDetector;
import com.example.util.AESUtil;

public class EncryptionRunner {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll");
    }

    public static void run(String cascadefilePath, String inputImagepath, String outputImagePath) throws Exception {
        // Detect faces
        FaceDetector detector = new FaceDetector(cascadefilePath);
        List<Rect> faces = detector.detectFaces(inputImagepath);

        System.out.println("[Encryption] Detected faces: " + faces.size());

        Mat image = Imgcodecs.imread(inputImagepath);
        System.out.println("[Encryption] Image info - Rows: " + image.rows() +
                ", Cols: " + image.cols() + ", Type: " + image.type() +
                ", Channels: " + image.channels());

        // Generate AES key and IV
        SecretKey aesKey = AESUtil.generateKey();
        byte[] iv = AESUtil.generateIV();

        // Save key and IV
        Files.writeString(Paths.get("key.txt"), Base64.getEncoder().encodeToString(aesKey.getEncoded()));
        Files.writeString(Paths.get("iv.txt"), Base64.getEncoder().encodeToString(iv));

        // Save face coordinates for decryption
        saveFaceCoordinates(faces);

        // Create a copy to show before/after comparison
        Mat originalImage = image.clone();

        for (int i = 0; i < faces.size(); i++) {
            Rect r = faces.get(i);
            System.out.println("[Encryption] Processing face " + (i + 1) + ": " +
                    "x=" + r.x + ", y=" + r.y + ", w=" + r.width + ", h=" + r.height);

            Mat faceRegion = new Mat(image, r);

            // Debug: Save original face region
            Imgcodecs.imwrite("debug_face_" + i + "_original.jpg", faceRegion);

            int numBytes = (int) (faceRegion.total() * faceRegion.channels());
            byte[] facePixels = new byte[numBytes];
            faceRegion.get(0, 0, facePixels);

            System.out.println("[Encryption] Face " + (i + 1) + " pixel data length: " + facePixels.length);

            // Print first few pixel values for debugging
            System.out.print("[Encryption] Original pixels (first 10): ");
            for (int j = 0; j < Math.min(10, facePixels.length); j++) {
                System.out.print((facePixels[j] & 0xFF) + " ");
            }
            System.out.println();

            byte[] encrypted = AESUtil.encryptBytes(facePixels, aesKey, iv);

            if (encrypted.length == facePixels.length) {
                faceRegion.put(0, 0, encrypted);

                // Debug: Save encrypted face region
                Imgcodecs.imwrite("debug_face_" + i + "_encrypted.jpg", faceRegion);

                System.out.println("[Encryption] Face " + (i + 1) + " encrypted successfully");

                // Print first few encrypted values for debugging
                System.out.print("[Encryption] Encrypted pixels (first 10): ");
                for (int j = 0; j < Math.min(10, encrypted.length); j++) {
                    System.out.print((encrypted[j] & 0xFF) + " ");
                }
                System.out.println();
            } else {
                System.out.println("[Encryption] Face " + (i + 1) + " - Length mismatch! Original: " +
                        facePixels.length + ", Encrypted: " + encrypted.length);
            }
        }

        // Save the encrypted image
        Imgcodecs.imwrite(outputImagePath, image);
        System.out.println("[Encryption] Encrypted image saved to: " + outputImagePath);

        // Save comparison image showing original faces with rectangles
        for (Rect r : faces) {
            Imgproc.rectangle(originalImage, r, new Scalar(0, 255, 0), 2);
        }
        Imgcodecs.imwrite("debug_original_with_rectangles.jpg", originalImage);
    }

    private static void saveFaceCoordinates(List<Rect> faces) throws IOException {
        try (FileWriter writer = new FileWriter("face_coordinates.txt")) {
            for (Rect face : faces) {
                writer.write(face.x + "," + face.y + "," + face.width + "," + face.height + "\n");
            }
        }
        System.out.println("[Encryption] Face coordinates saved to face_coordinates.txt");
    }
}