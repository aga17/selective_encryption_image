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

        return detectFacesInMat(image);
    }

    public List<Rect> detectFacesInMat(Mat image) {
        if (image.empty()) {
            throw new RuntimeException("Input Mat is empty");
        }

        // Convert to grayscale if needed
        Mat grayImage = new Mat();
        if (image.channels() == 3) {
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        } else {
            grayImage = image.clone();
        }

        MatOfRect faceDetections = new MatOfRect();
        faceCascade.detectMultiScale(grayImage, faceDetections);

        List<Rect> faceRects = faceDetections.toList();

        return faceRects;
    }

    // Method to detect faces and draw rectangles for debugging
    public List<Rect> detectFacesWithVisualization(Mat image) {
        List<Rect> faces = detectFacesInMat(image);

        // Draw rectangles around detected faces
        for (Rect rect : faces) {
            Imgproc.rectangle(image, rect, new Scalar(0, 255, 0), 2);
        }

        return faces;
    }
}