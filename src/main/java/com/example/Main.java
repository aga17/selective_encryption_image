package com.example;

import java.util.Scanner;

import com.example.runner.DecryptionRunner;
import com.example.runner.EncryptionRunner;

public class Main {
    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java4110.dll"); // Load OpenCV native library
    }

    public static void main(String[] args) throws Exception {
        String cascadeFilePath = "src\\main\\resources\\haarcascade_frontalface_default.xml"; // Path to the Haar
                                                                                              // Cascade file
        String imagePath = "src\\main\\resources\\input\\DSC_0733.JPG"; // Path to the input image

        String outputImagePath = "src\\main\\resources\\output\\encrypted_image.png"; // Path to save the
                                                                                      // encrypted image
        String encryptedImagePath = "src\\main\\resources\\output\\encrypted_image.png"; // Path to save the
                                                                                         // decrypted image
        String decryptedImagePath = "src\\main\\resources\\output\\decrypted_image.png"; // Path to save the
                                                                                         // decrypted image

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Choose an option:");
            System.out.println("1. Encrypt Image");
            System.out.println("2. Decrypt Image");
            System.out.println("3. Exit");

            int choice = scanner.nextInt();
            if (choice == 1) {
                // Encrypt the image
                EncryptionRunner.run(cascadeFilePath, imagePath, outputImagePath);
                System.out.println("Image encrypted successfully.");
            } else if (choice == 2) {
                // Decrypt the image
                DecryptionRunner.run(cascadeFilePath, encryptedImagePath, decryptedImagePath);
                System.out.println("Image decrypted successfully.");
            } else if (choice == 3) {
                break; // Exit the loop
            } else {
                System.out.println("Invalid choice. Please try again.");
            }
        }
        scanner.close();
        System.out.println("Program terminated.");
    }
}