package com.example.runner.Video;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import com.example.util.AESUtil;

public class VideoDecryptionRunner {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll");
    }

    @SuppressWarnings("unchecked")
    public static void run(String inputVideoPath, String outputVideoPath) throws Exception {
        // Note: inputVideoPath is ignored since we work with frames directly

        // Load video properties
        String videoPropsJson = Files.readString(Paths.get("src/main/resources/temp/video/video_properties.json"));
        Map<String, Object> videoProps = new com.google.gson.Gson().fromJson(videoPropsJson, Map.class);

        double fps = (Double) videoProps.get("fps");
        int frameWidth = ((Double) videoProps.get("width")).intValue();
        int frameHeight = ((Double) videoProps.get("height")).intValue();
        int totalFrames = ((Double) videoProps.get("totalFrames")).intValue();
        String encryptedFramesDir = (String) videoProps.get("encryptedFramesDir");

        System.out.println("[Video Decryption] Video properties:");
        System.out.println("FPS: " + fps);
        System.out.println("Resolution: " + frameWidth + "x" + frameHeight);
        System.out.println("Total frames: " + totalFrames);
        System.out.println("Encrypted frames dir: " + encryptedFramesDir);

        // Create directory for decrypted frames
        String decryptedFramesDir = "src/main/resources/temp/video/decrypted_frames/";
        Files.createDirectories(Paths.get(decryptedFramesDir));

        // Load AES key
        String base64Key = Files.readString(Paths.get("src/main/resources/temp/video/video_key.txt"));
        byte[] keyBytes = AESUtil.decodeBase64(base64Key);
        SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");

        // Load video metadata
        String metadataJson = Files.readString(Paths.get("src/main/resources/temp/video/video_metadata.json"));
        List<Map<String, Object>> videoMetadata = new com.google.gson.Gson().fromJson(metadataJson, List.class);

        // Create a lookup map for encrypted frames
        Map<Integer, Map<String, Object>> encryptedFrames = new java.util.HashMap<>();
        for (Map<String, Object> frameData : videoMetadata) {
            Integer frameNumber = ((Double) frameData.get("frameNumber")).intValue();
            Boolean isEncrypted = (Boolean) frameData.get("isEncrypted");
            if (isEncrypted != null && isEncrypted) {
                encryptedFrames.put(frameNumber, frameData);
            }
        }

        System.out.println("[Video Decryption] Found " + encryptedFrames.size() + " encrypted frames to decrypt");
        System.out.println("[Video Decryption] Starting decryption process...");

        int decryptedFrameCount = 0;
        int processedFrames = 0;

        // Process each frame
        for (int frameNumber = 0; frameNumber < totalFrames; frameNumber++) {
            String inputFramePath = encryptedFramesDir + String.format("frame_%06d.png", frameNumber);
            String outputFramePath = decryptedFramesDir + String.format("frame_%06d.png", frameNumber);

            // Check if frame file exists
            if (!Files.exists(Paths.get(inputFramePath))) {
                System.err.println("[Video Decryption] Frame file not found: " + inputFramePath);
                continue;
            }

            // Load frame
            Mat frame = Imgcodecs.imread(inputFramePath);
            if (frame.empty()) {
                System.err.println("[Video Decryption] Failed to load frame: " + inputFramePath);
                continue;
            }

            // Check if this frame has encrypted content
            if (encryptedFrames.containsKey(frameNumber)) {
                Map<String, Object> frameMetadata = encryptedFrames.get(frameNumber);
                List<Map<String, Object>> faceRegions = (List<Map<String, Object>>) frameMetadata.get("faceRegions");

                if (faceRegions != null && !faceRegions.isEmpty()) {
                    System.out.println("[Video Decryption] Decrypting frame " + frameNumber +
                            " with " + faceRegions.size() + " encrypted regions");

                    int decryptedRegions = 0;
                    for (Map<String, Object> faceData : faceRegions) {
                        try {
                            if (decryptFaceRegion(frame, faceData, aesKey)) {
                                decryptedRegions++;
                            }
                        } catch (Exception e) {
                            System.err.println("[Video Decryption] Failed to decrypt region in frame " +
                                    frameNumber + ": " + e.getMessage());
                        }
                    }

                    if (decryptedRegions > 0) {
                        decryptedFrameCount++;
                        System.out.println("[Video Decryption] Successfully decrypted " +
                                decryptedRegions + "/" + faceRegions.size() + " regions in frame " + frameNumber);
                    }
                }
            }

            // Save decrypted frame
            boolean saved = Imgcodecs.imwrite(outputFramePath, frame);
            if (!saved) {
                System.err.println("[Video Decryption] Failed to save decrypted frame: " + outputFramePath);
            }

            processedFrames++;

            // Progress indicator
            if (frameNumber % 30 == 0 || frameNumber == totalFrames - 1) {
                double progress = (double) (frameNumber + 1) / totalFrames * 100;
                System.out.printf("[Video Decryption] Progress: %.1f%% (%d/%d frames)\n",
                        progress, frameNumber + 1, totalFrames);
            }
        }

        // Create reconstruction script for decrypted video
        createDecryptedReconstructionScript(decryptedFramesDir, outputVideoPath, fps);

        System.out.println("[Video Decryption] ✓ Video decryption completed!");
        System.out.println("[Video Decryption] Decrypted frames saved to: " + decryptedFramesDir);
        System.out.println("[Video Decryption] Total frames processed: " + processedFrames);
        System.out.println("[Video Decryption] Frames with decrypted faces: " + decryptedFrameCount);
        System.out.println("");
        System.out.println("=== MANUAL RECONSTRUCTION REQUIRED ===");
        System.out.println("To create the final decrypted video, run:");
        System.out.println("  Windows: src/main/resources/temp/decrypted_reconstruction_script.bat");
        System.out.println("  Unix/Linux: src/main/resources/temp/decrypted_reconstruction_script.sh");
    }

    private static boolean decryptFaceRegion(Mat frame, Map<String, Object> faceData, SecretKey aesKey)
            throws Exception {
        // Extract region coordinates
        int x = ((Double) faceData.get("x")).intValue();
        int y = ((Double) faceData.get("y")).intValue();
        int width = ((Double) faceData.get("width")).intValue();
        int height = ((Double) faceData.get("height")).intValue();
        byte[] iv = Base64.getDecoder().decode((String) faceData.get("iv"));

        // Validate coordinates
        if (x < 0 || y < 0 || x + width > frame.cols() || y + height > frame.rows()) {
            System.err.println(
                    "[Video Decryption] Invalid region coordinates: " + x + "," + y + " " + width + "x" + height);
            return false;
        }

        Rect rect = new Rect(x, y, width, height);
        Mat faceRegion = frame.submat(rect);

        // Get encrypted bytes
        int numBytes = (int) (faceRegion.total() * faceRegion.channels());
        byte[] encryptedBytes = new byte[numBytes];
        faceRegion.get(0, 0, encryptedBytes);

        try {
            // Decrypt the region
            byte[] decrypted = AESUtil.decryptBytes(encryptedBytes, aesKey, iv);

            if (decrypted.length != encryptedBytes.length) {
                System.err.println("[Video Decryption] Length mismatch! Expected: " + encryptedBytes.length +
                        ", Got: " + decrypted.length);
                return false;
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

            // Apply decrypted data back to frame
            faceRegion.put(0, 0, decrypted);
            return true;

        } catch (Exception e) {
            System.err.println("[Video Decryption] Decryption failed for region: " + e.getMessage());
            return false;
        }
    }

    private static void createDecryptedReconstructionScript(String framesDir, String outputPath, double fps) {
        try {
            String scriptContent = String.format(
                    "@echo off\n" +
                            "echo Reconstructing DECRYPTED video from frames...\n" +
                            "echo.\n" +
                            "echo Using FFmpeg to combine decrypted frames:\n" +
                            "ffmpeg -framerate %.2f -i \"%sframe_%%06d.png\" -c:v libx264 -pix_fmt yuv420p \"%s\"\n" +
                            "echo.\n" +
                            "echo Decrypted video will be saved as: %s\n" +
                            "echo.\n" +
                            "pause\n",
                    fps, framesDir.replace("/", "\\"), outputPath.replace("/", "\\"), outputPath);

            Files.writeString(Paths.get("src/main/resources/temp/video/decrypted_reconstruction_script.bat"),
                    scriptContent);

            // Also create a shell script for Unix systems
            String shellScript = String.format(
                    "#!/bin/bash\n" +
                            "echo \"Reconstructing DECRYPTED video from frames...\"\n" +
                            "echo\n" +
                            "echo \"Using FFmpeg to combine decrypted frames:\"\n" +
                            "ffmpeg -framerate %.2f -i \"%sframe_%%06d.png\" -c:v libx264 -pix_fmt yuv420p \"%s\"\n" +
                            "echo\n" +
                            "echo \"Decrypted video will be saved as: %s\"\n",
                    fps, framesDir, outputPath, outputPath);

            Files.writeString(Paths.get("src/main/resources/temp/video/decrypted_reconstruction_script.sh"),
                    shellScript);

        } catch (Exception e) {
            System.err.println("[Video Decryption] Failed to create reconstruction script: " + e.getMessage());
        }
    }
}