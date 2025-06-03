package com.example.runner;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import com.example.detector.FaceDetector;
import com.example.util.AESUtil;

public class EncryptionRunner {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll"); // Load OpenCV native library
    }

    public static void run(String cascadefilePath, String inputImagepath, String outputImagePath) throws Exception {
        // Detect faces
        FaceDetector detector = new FaceDetector(cascadefilePath);
        List<Rect> faces = detector.detectFaces(inputImagepath);

        System.out.println("[Encrytion]Detected faces: " + faces.size());

        Mat image = Imgcodecs.imread(inputImagepath);

        // Generate AES key and IV
        SecretKey aesKey = AESUtil.generateKey();
        byte[] iv = AESUtil.generateIV();

        // Save key and IV
        Files.writeString(Paths.get("key.txt"), Base64.getEncoder().encodeToString(aesKey.getEncoded()));
        Files.writeString(Paths.get("iv.txt"), Base64.getEncoder().encodeToString(iv));

        for (Rect r : faces) {
            Mat faceRegion = new Mat(image, r);
            int numBytes = (int) (faceRegion.total() * faceRegion.channels());
            byte[] facePixels = new byte[numBytes];
            faceRegion.get(0, 0, facePixels);

            byte[] encrypted = AESUtil.encryptBytes(facePixels, aesKey, iv);

            if (encrypted.length == facePixels.length) {
                faceRegion.put(0, 0, encrypted);
            } else {
                System.out.println("[Encryption] Encrypted length mismatch. Skipping.");
            }
        }

        Imgcodecs.imwrite(outputImagePath, image);
        System.out.println("[Encryption] Encrypted image saved to: " + outputImagePath);
    }
}