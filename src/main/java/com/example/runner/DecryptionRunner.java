package com.example.runner;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import com.example.detector.FaceDetector;
import com.example.util.AESUtil;

public class DecryptionRunner {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll"); // Load OpenCV native library
    }

    public static void run(String cascadeFilePath, String encryptedImagePath, String outputImagePath) throws Exception {
        // load AES key and IV
        String keyBase64 = Files.readString(Paths.get("key.txt"));
        String ivBase64 = Files.readString(Paths.get("iv.txt"));
        SecretKey aesKey = AESUtil.loadKeyFromBase64(keyBase64);
        IvParameterSpec iv = AESUtil.loadIVFromBase64(ivBase64);

        // Detect faces in encrypted image
        FaceDetector detector = new FaceDetector(cascadeFilePath);
        List<Rect> faces = detector.detectFaces(encryptedImagePath);
        System.out.println("[Decryption] Faces detected: " + faces.size());

        Mat image = Imgcodecs.imread(encryptedImagePath);

        for (Rect r : faces) {
            Mat faceRegion = new Mat(image, r);
            int numBytes = (int) (faceRegion.total() * faceRegion.channels());
            byte[] encryptedPixels = new byte[numBytes];
            faceRegion.get(0, 0, encryptedPixels);

            byte[] decrypted = AESUtil.decryptBytes(encryptedPixels, aesKey, iv);

            if (decrypted.length == encryptedPixels.length) {
                faceRegion.put(0, 0, decrypted);
            } else {
                System.out.println("[Decryption] Decrypted length mismatch. Skipping.");
            }
        }

        Imgcodecs.imwrite(outputImagePath, image);
        System.out.println("[Decryption] Decrypted image saved to: " + outputImagePath);
    }
}
