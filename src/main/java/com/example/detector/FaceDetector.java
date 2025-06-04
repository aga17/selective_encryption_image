package com.example.detector;

import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class FaceDetector {
    private CascadeClassifier faceCascade;

    public FaceDetector(String cascadeFilePath) {
        faceCascade = new CascadeClassifier(cascadeFilePath);
        if (faceCascade.empty()) {
            throw new RuntimeException("Failed to load cascade classifier from " + cascadeFilePath);
        }
    }

    public List<Rect> detectFaces(String imagePath) {
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            throw new RuntimeException("Failed to read image from " + imagePath);
        }

        MatOfRect faceDetections = new MatOfRect();
        faceCascade.detectMultiScale(image, faceDetections);

        List<Rect> faceRects = faceDetections.toList();

        // Optional: Draw rectangles and save output image for testing
        for (Rect rect : faceRects) {
            Imgproc.rectangle(image, rect, new Scalar(0, 255, 0), 2);
        }

        // String outputImagePath = "src\\main\\java\\com\\example\\output\\output.jpg";
        // Imgcodecs.imwrite(outputImagePath, image);
        // System.out.println("Output image saved to " + outputImagePath);

        return faceRects;
    }
}
