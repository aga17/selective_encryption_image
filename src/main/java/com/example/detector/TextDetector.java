package com.example.detector;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.features2d.MSER;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class TextDetector {
    private CascadeClassifier textCascade;

    public TextDetector(String cascadeFilePath) {
        textCascade = new CascadeClassifier(cascadeFilePath);
        if (textCascade.empty()) {
            throw new RuntimeException("Failed to load cascade classifier from " + cascadeFilePath);
        }
    }

    public List<Rect> detectTexts(String inputImagePath) {
        // read the image
        Mat image = Imgcodecs.imread(inputImagePath);

        if (image.empty()) {
            throw new RuntimeException("Failed to read image from " + inputImagePath);
        }

        return detectTextsInMat(image);
    }

    public List<Rect> detectTextsInMat(Mat image) {
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

        // Use MSER for text detection
        MSER mser = MSER.create();
        List<MatOfPoint> regions = new ArrayList<>();
        MatOfRect bboxes = new MatOfRect();
        mser.detectRegions(grayImage, regions, bboxes);

        List<Rect> textRects = new ArrayList<>();
        for (Rect rect : bboxes.toList()) {
            // Filter out small regions
            if (rect.width > 10 && rect.height > 10) {
                textRects.add(rect);
                // Optional: Draw rectangles for debugging
                Imgproc.rectangle(image, rect, new org.opencv.core.Scalar(0, 255, 0), 2);
            }
        }

        return textRects;
    }
}
