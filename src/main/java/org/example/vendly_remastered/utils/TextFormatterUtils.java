package org.example.vendly_remastered.utils;

import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class TextFormatterUtils {
    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d*");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("\\d*(\\.\\d*)?");
    private static final Pattern BARCODE_PATTERN = Pattern.compile("[a-zA-Z0-9]*");

    public static TextFormatter<String> createIntegerFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (INTEGER_PATTERN.matcher(newText).matches()) {
                return change; // Allow change
            }
            return null; // Reject change
        };
        return new TextFormatter<>(filter);
    }

    public static TextFormatter<String> createDecimalFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (DECIMAL_PATTERN.matcher(newText).matches()) {
                return change;
            }
            return null;
        };
        return new TextFormatter<>(filter);
    }

    public static TextFormatter<String> createBarcodeFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (BARCODE_PATTERN.matcher(newText).matches()) {
                return change;
            }
            return null;
        };
        return new TextFormatter<>(filter);
    }
}
