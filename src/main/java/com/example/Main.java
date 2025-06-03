package com.example;

import com.example.runner.DecryptionRunner;

public class Main {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll"); // Load OpenCV native library
    }

    public static void main(String[] args) throws Exception {
        String cascadeFilePath = "src\\main\\resources\\haarcascade_frontalface_default.xml"; // Path to the Haar
                                                                                              // Cascade file
        String imagePath = "src\\main\\java\\com\\example\\input\\input.jpg"; // Path to the input image

        String outputImagePath = "src\\main\\java\\com\\example\\output\\encrypted_output.jpg"; // Path to save the
                                                                                                // encrypted image

        String encryptedImagePath = "src\\main\\java\\com\\example\\output\\encrypted_output.jpg"; // Path to save the
                                                                                                   // decrypted image
        String decryptedImagePath = "src\\main\\java\\com\\example\\output\\decrypted_output.jpg"; // Path to save the
                                                                                                   // decrypted image
        DecryptionRunner.run(cascadeFilePath, encryptedImagePath, decryptedImagePath);

    }
}