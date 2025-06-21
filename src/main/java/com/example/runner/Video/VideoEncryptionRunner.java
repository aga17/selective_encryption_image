package com.example.runner.Video;

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

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import com.example.detector.FaceDetector;
import com.example.detector.TextDetector;
import com.example.util.AESUtil;

public class VideoEncryptionRunner {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll");
    }

    public static void run(String cascadeFilePath, String inputVideoPath, String outputVideoPath) throws Exception {
        VideoCapture cap = new VideoCapture(inputVideoPath);

        if (!cap.isOpened()) {
            throw new RuntimeException("Could not open video: " + inputVideoPath);
        }

        // Get video properties
        double fps = cap.get(Videoio.CAP_PROP_FPS);
        int frameWidth = (int) cap.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int frameHeight = (int) cap.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        int totalFrames = (int) cap.get(Videoio.CAP_PROP_FRAME_COUNT);

        System.out.println("[Video Encryption] Video properties:");
        System.out.println("FPS: " + fps);
        System.out.println("Resolution: " + frameWidth + "x" + frameHeight);
        System.out.println("Total frames: " + totalFrames);

        // Create directory for encrypted frames
        String encryptedFramesDir = "src/main/resources/temp/video/encrypted_frames/";
        Files.createDirectories(Paths.get(encryptedFramesDir));

        // Initialize face detector and text detector
        System.out.println("[Video Encryption] Initializing face detector...");
        FaceDetector faceDetector = new FaceDetector(cascadeFilePath);
        System.out.println("[Video Encryption] Face detector initialized successfully.");
        System.out.println("[Video Encryption] Initializing text detector...");
        TextDetector textDetector = new TextDetector(cascadeFilePath);
        System.out.println("[Video Encryption] Text detector initialized successfully.");

        // Generate AES key for this video
        SecretKey aesKey = AESUtil.generateKey();
        String base64Key = Base64.getEncoder().encodeToString(aesKey.getEncoded());
        Files.writeString(Paths.get("src/main/resources/temp/video/video_key.txt"), base64Key);

        // Store encryption metadata
        List<Map<String, Object>> videoMetadata = new ArrayList<>();

        Mat frame = new Mat();
        int frameNumber = 0;
        int keyFrameInterval = (int) Math.max(1, fps);
        int encryptedFrames = 0;

        System.out.println("[Video Encryption] Processing frames...");

        while (cap.read(frame)) {
            if (frame.empty()) {
                break;
            }

            boolean isKeyFrame = (frameNumber % keyFrameInterval == 0);
            if (isKeyFrame) {
                System.out.println("[Video Encryption] Processing I-frame " + frameNumber);

                // Detect faces and text in this keyframe
                List<Rect> faces = faceDetector.detectFacesInMat(frame);
                List<Rect> textRegions = textDetector.detectTextsInMat(frame);

                if (!faces.isEmpty() || !textRegions.isEmpty()) {
                    Map<String, Object> frameMetadata = new HashMap<>();
                    frameMetadata.put("frameNumber", frameNumber);
                    frameMetadata.put("isEncrypted", true);

                    List<Map<String, Object>> faceRegions = new ArrayList<>();
                    List<Map<String, Object>> textRegionsData = new ArrayList<>();

                    // Encrypt each detected face
                    for (Rect faceRect : faces) {
                        try {
                            Map<String, Object> faceData = encryptFaceRegion(frame, faceRect, aesKey);
                            if (faceData != null) {
                                faceRegions.add(faceData);
                            }
                        } catch (Exception e) {
                            System.err.println("[Video Encryption] Failed to encrypt face at frame " +
                                    frameNumber + ": " + e.getMessage());
                        }
                    }

                    // Encrypt each detected text region
                    for (Rect textRect : textRegions) {
                        try {
                            Map<String, Object> textData = encryptTextRegion(frame, textRect, aesKey);
                            if (textData != null) {
                                textData.put("type", "text");
                                textRegionsData.add(textData);
                            }
                        } catch (Exception e) {
                            System.err.println("[Video Encryption] Failed to encrypt text at frame " +
                                    frameNumber + ": " + e.getMessage());
                        }
                    }

                    frameMetadata.put("faceRegions", faceRegions);
                    frameMetadata.put("textRegions", textRegionsData);
                    frameMetadata.put("facesEncrypted", faceRegions.size());
                    frameMetadata.put("textRegionsEncrypted", textRegionsData.size());
                    videoMetadata.add(frameMetadata);
                    encryptedFrames++;

                    System.out.println("[Video Encryption] Encrypted " + faceRegions.size() +
                            " faces in I-frame " + frameNumber);
                    System.out.println("[Video Encryption] Encrypted " + textRegionsData.size() +
                            " text regions in I-frame " + frameNumber);
                } else {
                    // No faces detected, add metadata indicating no encryption
                    Map<String, Object> frameMetadata = new HashMap<>();
                    frameMetadata.put("frameNumber", frameNumber);
                    frameMetadata.put("isEncrypted", false);
                    frameMetadata.put("facesEncrypted", 0);
                    frameMetadata.put("textRegionsEncrypted", 0);
                    videoMetadata.add(frameMetadata);
                }
            }

            // Save frame as PNG (lossless) - all frames for complete video
            String framePath = encryptedFramesDir + String.format("frame_%06d.png", frameNumber);
            boolean saved = Imgcodecs.imwrite(framePath, frame);

            if (!saved) {
                System.err.println("[Video Encryption] Failed to save frame " + frameNumber);
            }

            frameNumber++;

            // Progress indicator
            if (frameNumber % 30 == 0) {
                double progress = (double) frameNumber / totalFrames * 100;
                System.out.printf("[Video Encryption] Progress: %.1f%% (%d/%d frames)\n",
                        progress, frameNumber, totalFrames);
            }
        }

        cap.release();

        // Save video metadata
        FileWriter metadataWriter = new FileWriter("src/main/resources/temp/video/video_metadata.json");
        metadataWriter.write(new com.google.gson.Gson().toJson(videoMetadata));
        metadataWriter.close();

        // Store video properties for reconstruction
        Map<String, Object> videoProps = new HashMap<>();
        videoProps.put("fps", fps);
        videoProps.put("width", frameWidth);
        videoProps.put("height", frameHeight);
        videoProps.put("totalFrames", totalFrames);
        videoProps.put("originalVideoPath", inputVideoPath);
        videoProps.put("encryptedFramesDir", encryptedFramesDir);

        FileWriter propsWriter = new FileWriter("src/main/resources/temp/video/video_properties.json");
        propsWriter.write(new com.google.gson.Gson().toJson(videoProps));
        propsWriter.close();

        // Create a reconstruction script
        createReconstructionScript(encryptedFramesDir, outputVideoPath, fps);

        System.out.println("[Video Encryption] ✓ Video encryption completed!");
        System.out.println("[Video Encryption] Encrypted frames saved to: " + encryptedFramesDir);
        System.out.println("[Video Encryption] Total frames processed: " + frameNumber);
        System.out.println("[Video Encryption] Frames with encrypted faces: " + encryptedFrames);
        System.out.println("");
        System.out.println("=== MANUAL RECONSTRUCTION REQUIRED ===");
        System.out.println("Due to OpenCV codec limitations, please reconstruct the video manually:");
        System.out.println("1. Use FFmpeg command (see reconstruction_script.bat)");
        System.out.println("2. Or use any video editing software to combine the PNG frames");
        System.out.println("3. Encrypted frames are in: " + encryptedFramesDir);
    }

    private static void createReconstructionScript(String framesDir, String outputPath, double fps) {
        try {
            String scriptContent = String.format(
                    "@echo off\n" +
                            "echo Reconstructing video from encrypted frames...\n" +
                            "echo.\n" +
                            "echo Using FFmpeg to combine frames:\n" +
                            "ffmpeg -framerate %.2f -i \"%sframe_%%06d.png\" -c:v libx264 -pix_fmt yuv420p \"%s\"\n" +
                            "echo.\n" +
                            "echo If FFmpeg is not installed, you can:\n" +
                            "echo 1. Download FFmpeg from https://ffmpeg.org/\n" +
                            "echo 2. Or use any video editing software to combine the PNG frames\n" +
                            "echo.\n" +
                            "pause\n",
                    fps, framesDir.replace("/", "\\"), outputPath.replace("/", "\\"));

            Files.writeString(Paths.get("src/main/resources/temp/video/reconstruction_script.bat"), scriptContent);

            // Also create a shell script for Unix systems
            String shellScript = String.format(
                    "#!/bin/bash\n" +
                            "echo \"Reconstructing video from encrypted frames...\"\n" +
                            "echo\n" +
                            "echo \"Using FFmpeg to combine frames:\"\n" +
                            "ffmpeg -framerate %.2f -i \"%sframe_%%06d.png\" -c:v libx264 -pix_fmt yuv420p \"%s\"\n" +
                            "echo\n" +
                            "echo \"If FFmpeg is not installed, you can:\"\n" +
                            "echo \"1. Install FFmpeg: sudo apt install ffmpeg (Ubuntu) or brew install ffmpeg (macOS)\"\n"
                            +
                            "echo \"2. Or use any video editing software to combine the PNG frames\"\n",
                    fps, framesDir, outputPath);

            Files.writeString(Paths.get("src/main/resources/temp/video/reconstruction_script.sh"), shellScript);

            System.out.println("[Video Encryption] Reconstruction scripts created:");
            System.out.println("  - Windows: src/main/resources/temp/video/reconstruction_script.bat");
            System.out.println("  - Unix/Linux: src/main/resources/temp/video/reconstruction_script.sh");

        } catch (Exception e) {
            System.err.println("[Video Encryption] Failed to create reconstruction script: " + e.getMessage());
        }
    }

    private static Map<String, Object> encryptFaceRegion(Mat frame, Rect faceRect, SecretKey aesKey) throws Exception {
        // Extract face region
        Mat faceRegion = frame.submat(faceRect);
        int numBytes = (int) (faceRegion.total() * faceRegion.channels());
        byte[] faceBytes = new byte[numBytes];
        faceRegion.get(0, 0, faceBytes);

        // Create hash for validation
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] originalHash = md.digest(faceBytes);

        // Generate IV and encrypt
        byte[] iv = AESUtil.generateIV();
        byte[] encrypted = AESUtil.encryptBytes(faceBytes, aesKey, iv);

        // Validate encryption
        if (encrypted.length != faceBytes.length) {
            throw new RuntimeException("Encryption length mismatch");
        }

        // Test decryption to ensure it works
        byte[] testDecrypt = AESUtil.decryptBytes(encrypted, aesKey, iv);
        byte[] testHash = md.digest(testDecrypt);

        if (!MessageDigest.isEqual(originalHash, testHash)) {
            throw new RuntimeException("Encryption validation failed");
        }

        // Apply encrypted data back to frame
        faceRegion.put(0, 0, encrypted);

        // Return metadata
        Map<String, Object> faceData = new HashMap<>();
        faceData.put("x", faceRect.x);
        faceData.put("y", faceRect.y);
        faceData.put("width", faceRect.width);
        faceData.put("height", faceRect.height);
        faceData.put("iv", Base64.getEncoder().encodeToString(iv));
        faceData.put("originalHash", Base64.getEncoder().encodeToString(originalHash));

        return faceData;
    }

    private static Map<String, Object> encryptTextRegion(Mat frame, Rect textRect, SecretKey aesKey) throws Exception {
        // Extract text region
        Mat textRegion = frame.submat(textRect);
        int numBytes = (int) (textRegion.total() * textRegion.channels());
        byte[] textBytes = new byte[numBytes];
        textRegion.get(0, 0, textBytes);

        // Create hash for validation
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] originalHash = md.digest(textBytes);

        // Generate IV and encrypt
        byte[] iv = AESUtil.generateIV();
        byte[] encrypted = AESUtil.encryptBytes(textBytes, aesKey, iv);

        // Validate encryption
        if (encrypted.length != textBytes.length) {
            throw new RuntimeException("Encryption length mismatch");
        }

        // Test decryption to ensure it works
        byte[] testDecrypt = AESUtil.decryptBytes(encrypted, aesKey, iv);
        byte[] testHash = md.digest(testDecrypt);

        if (!MessageDigest.isEqual(originalHash, testHash)) {
            throw new RuntimeException("Encryption validation failed");
        }

        // Apply encrypted data back to frame
        textRegion.put(0, 0, encrypted);

        // Return metadata
        Map<String, Object> textData = new HashMap<>();
        textData.put("x", textRect.x);
        textData.put("y", textRect.y);
        textData.put("width", textRect.width);
        textData.put("height", textRect.height);
        textData.put("iv", Base64.getEncoder().encodeToString(iv));
        textData.put("originalHash", Base64.getEncoder().encodeToString(originalHash));
        textData.put("type", "text");

        return textData;
    }
}