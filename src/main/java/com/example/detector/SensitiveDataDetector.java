package com.example.detector;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SensitiveDataDetector {
    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{10,13}\\b");

    public static List<MatchResult> detectSensitiveData(String Content) {
        List<MatchResult> results = new ArrayList<>();
        Matcher emailMatcher = EMAIL_PATTERN.matcher(Content);
        while (emailMatcher.find()) {
            results.add(emailMatcher.toMatchResult());
        }

        Matcher phoneMatcher = PHONE_PATTERN.matcher(Content);
        while (phoneMatcher.find()) {
            results.add(phoneMatcher.toMatchResult());
        }
        return results;
    }
}
