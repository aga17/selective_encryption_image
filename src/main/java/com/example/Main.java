package com.example;

import java.util.Scanner;

import com.example.runner.Image.*;
import com.example.runner.Video.*;
import com.example.runner.Text.*;

public class Main {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll"); // Load OpenCV native library
    }

    public static void main(String[] args) throws Exception {
        String cascadeFilePath = "src\\main\\resources\\haarcascade_frontalface_default.xml"; // Path to the Haar
                                                                                              // Cascade file

        // input
        String inputTextPath = "src\\main\\resources\\input\\text\\sample_text.txt";
        String inputImagePath = "src\\main\\resources\\input\\image\\sample_face_text.jpg";
        String inputVideoPath = "src\\main\\resources\\input\\video\\sample_face_text.mp4";

        // output (encrypted)
        String encryptedTextPath = "src\\main\\resources\\output\\encrypted\\text\\encrypted_text.txt";
        String encryptedImagePath = "src\\main\\resources\\output\\encrypted\\image\\encrypted_image.png";
        String encryptedVideoPath = "src\\main\\resources\\output\\encrypted\\video\\encrypted_video.mp4";

        // output (decrypted)
        String decryptedTextPath = "src\\main\\resources\\output\\decrypted\\text\\decrypted_text.txt";
        String decryptedImagePath = "src\\main\\resources\\output\\decrypted\\image\\decrypted_image.jpg";
        String decryptedVideoPath = "src\\main\\resources\\output\\decrypted\\video\\decrypted_video.mp4";

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Selective Encryption System ===");
            System.out.println("Choose an option:");
            System.out.println("1. Encrypt Text (Sensitive Data Detection)");
            System.out.println("2. Decrypt Text");
            System.out.println("3. Encrypt Image (Face + Text Detection)");
            System.out.println("4. Decrypt Image");
            System.out.println("5. Encrypt Video (I-Frame + Face Detection)");
            System.out.println("6. Decrypt Video");

            System.out.println("7. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();

            try {
                switch (choice) {
                    case 1:
                        System.out.println("Starting text encryption...");
                        System.out.println("This will detect and encrypt sensitive data in the text file.");
                        TextEncryptionRunner.run(inputTextPath, encryptedTextPath);
                        System.out.println("✓ Text encrypted successfully.");
                        System.out.println("Encrypted text saved to: " + encryptedTextPath);
                        break;

                    case 2:
                        System.out.println("Starting text decryption...");
                        System.out.println("This will decrypt the encrypted text file.");
                        TextDecryptionRunner.run(encryptedTextPath, decryptedTextPath);
                        System.out.println("✓ Text decrypted successfully.");
                        System.out.println("Decrypted text saved to: " + decryptedTextPath);
                        break;

                    case 3:
                        System.out.println("Starting image encryption with face and text detection...");
                        System.out.println("This will detect and encrypt both faces and text regions.");
                        ImageEncryptionRunner.run(cascadeFilePath, inputImagePath, encryptedImagePath);
                        System.out.println("✓ Image encrypted successfully.");
                        System.out.println("Encrypted image saved to: " + encryptedImagePath);
                        break;

                    case 4:
                        System.out.println("Starting image decryption...");
                        System.out.println("This will decrypt both face and text regions.");
                        ImageDecryptionRunner.run(cascadeFilePath, encryptedImagePath, decryptedImagePath);
                        System.out.println("✓ Image decrypted successfully.");
                        System.out.println("Decrypted image saved to: " + decryptedImagePath);
                        break;

                    case 5:
                        System.out.println("Starting video encryption...");
                        System.out.println("This may take several minutes depending on video length...");
                        VideoEncryptionRunner.run(cascadeFilePath, inputVideoPath, encryptedVideoPath);
                        System.out.println("✓ Video encrypted successfully.");
                        System.out.println("Encrypted video saved to: " + encryptedVideoPath);
                        break;

                    case 6:
                        System.out.println("Starting video decryption...");
                        System.out.println("This may take several minutes depending on video length...");
                        VideoDecryptionRunner.run(encryptedVideoPath, decryptedVideoPath);
                        System.out.println("✓ Video decrypted successfully.");
                        System.out.println("Decrypted video saved to: " + decryptedVideoPath);
                        break;

                    case 7:
                        System.out.println("Exiting program...");
                        scanner.close();
                        System.exit(0);
                        break;

                    default:
                        System.out.println("❌ Invalid choice. Please enter a number between 1-6.");
                        break;
                }
            } catch (Exception e) {
                System.err.println("❌ Error occurred: " + e.getMessage());
                e.printStackTrace();
                System.out.println("Please check your file paths and try again.");
            }
        }
    }
}