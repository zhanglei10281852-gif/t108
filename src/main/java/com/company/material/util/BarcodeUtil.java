package com.company.material.util;

import java.util.HashMap;
import java.util.Map;

public class BarcodeUtil {

    private static final String[] CODE128_PATTERNS = {
            "11011001100",
            "11001101100",
            "11001100110",
            "10010011000",
            "10010001100",
            "10001001100",
            "10011001000",
            "10011000100",
            "10001100100",
            "11001001000",
            "11001000100",
            "11000100100",
            "10110011100",
            "10011011100",
            "10011001110",
            "10111001100",
            "10011101100",
            "10011100110",
            "11001110010",
            "11001011100",
            "11001001110",
            "11011100100",
            "11001110100",
            "11101101110",
            "11101001100",
            "11100101100",
            "11100100110",
            "11101100100",
            "11100110100",
            "11100110010",
            "11011011000",
            "11011000110",
            "11000110110",
            "10100011000",
            "10001011000",
            "10001000110",
            "10110001000",
            "10001101000",
            "10001100010",
            "11010001000",
            "11000101000",
            "11000100010",
            "10110111000",
            "10110001110",
            "10001101110",
            "10111011000",
            "10111000110",
            "10001110110",
            "11101110110",
            "11010001110",
            "11000101110",
            "11011101000",
            "11011100010",
            "11011101110",
            "11101011000",
            "11101000110",
            "11100010110",
            "11101101000",
            "11101100010",
            "11100011010",
            "11101111010",
            "11001000010",
            "11110001010",
            "10100110000",
            "10100001100",
            "10010110000",
            "10010000110",
            "10000101100",
            "10000100110",
            "10110010000",
            "10110000100",
            "10011010000",
            "10011000010",
            "10000110100",
            "10000110010",
            "11000010010",
            "11001010000",
            "11110111010",
            "11000010100",
            "10001111010",
            "10100111100",
            "10010111100",
            "10010011110",
            "10111100100",
            "10011110100",
            "10011110010",
            "11110100100",
            "11110010100",
            "11110010010",
            "11011011110",
            "11011110110",
            "11110110110",
            "10101111000",
            "10100011110",
            "10001011110",
            "10111101000",
            "10111100010",
            "11110101000",
            "11110100010",
            "10111011110",
            "10111101110",
            "11101011110",
            "11110101110",
            "11010000100",
            "11010010000",
            "11010011100",
            "1100011101011"
    };

    private static final String[] EAN13_L_ODD = {
            "0001101", "0011001", "0010011", "0111101", "0100011",
            "0110001", "0101111", "0111011", "0110111", "0001011"
    };

    private static final String[] EAN13_L_EVEN = {
            "0100111", "0110011", "0011011", "0100001", "0011101",
            "0111001", "0000101", "0010001", "0001001", "0010111"
    };

    private static final String[] EAN13_RIGHT = {
            "1110010", "1100110", "1101100", "1000010", "1011100",
            "1001110", "1010000", "1000100", "1001000", "1110100"
    };

    private static final int[][] EAN13_PARITY = {
            {0, 0, 0, 0, 0, 0},
            {0, 0, 1, 0, 1, 1},
            {0, 0, 1, 1, 0, 1},
            {0, 0, 1, 1, 1, 0},
            {0, 1, 0, 0, 1, 1},
            {0, 1, 1, 0, 0, 1},
            {0, 1, 1, 1, 0, 0},
            {0, 1, 0, 1, 0, 1},
            {0, 1, 0, 1, 1, 0},
            {0, 1, 1, 0, 1, 0}
    };

    public static String generateMaterialBarcode(String materialCode) {
        String numeric = extractNumeric(materialCode);
        if (numeric.length() >= 12) {
            numeric = numeric.substring(0, 12);
        } else {
            numeric = String.format("%-12s", numeric).replace(' ', '0');
        }
        int checkDigit = computeEAN13Check(numeric);
        return numeric + checkDigit;
    }

    public static String generateLocationBarcode(String locationCode) {
        return "LOC-" + locationCode;
    }

    public static String generateDocumentBarcode(String documentNo) {
        return "DOC-" + documentNo;
    }

    public static String parseBarcodeType(String barcode) {
        if (barcode == null || barcode.isEmpty()) return "未知";
        if (barcode.startsWith("LOC-")) return "库位";
        if (barcode.startsWith("DOC-")) return "单据";
        return "物料";
    }

    public static String parseMaterialCodeFromBarcode(String barcode) {
        if (barcode.startsWith("LOC-") || barcode.startsWith("DOC-")) return null;
        return barcode;
    }

    public static String parseLocationCodeFromBarcode(String barcode) {
        if (!barcode.startsWith("LOC-")) return null;
        return barcode.substring(4);
    }

    public static String parseDocumentNoFromBarcode(String barcode) {
        if (!barcode.startsWith("DOC-")) return null;
        return barcode.substring(4);
    }

    public static boolean validateEAN13(String barcode) {
        if (barcode == null || barcode.length() != 13) return false;
        try {
            int checkDigit = computeEAN13Check(barcode.substring(0, 12));
            return checkDigit == Character.getNumericValue(barcode.charAt(12));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String generateSvgBarcode(String content) {
        try {
            if (content == null || content.isEmpty()) {
                return emptySvg();
            }
            boolean isNumeric = content.matches("\\d{13}");
            String encoded;
            if (isNumeric) {
                encoded = encodeEAN13(content);
            } else {
                encoded = encodeCode128(content);
            }
            return buildSvg(encoded, content);
        } catch (Exception e) {
            return fallbackSvg(content);
        }
    }

    public static Map<String, Object> generateBarcodeData(String type, String id, String code, String name, String specification) {
        String barcode;
        switch (type) {
            case "物料":
                barcode = generateMaterialBarcode(code);
                break;
            case "库位":
                barcode = generateLocationBarcode(code);
                break;
            case "单据":
                barcode = generateDocumentBarcode(code);
                break;
            default:
                barcode = code;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("code", code);
        data.put("name", name);
        data.put("specification", specification);
        data.put("barcode", barcode);
        data.put("svg", generateSvgBarcode(barcode));
        return data;
    }

    private static int computeEAN13Check(String first12) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(first12.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int mod = sum % 10;
        return (mod == 0) ? 0 : 10 - mod;
    }

    private static String extractNumeric(String code) {
        StringBuilder sb = new StringBuilder();
        for (char c : code.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
        }
        return sb.toString();
    }

    private static String encodeEAN13(String data) {
        StringBuilder sb = new StringBuilder();
        sb.append("101");
        int firstDigit = data.charAt(0) - '0';
        int[] parity = EAN13_PARITY[firstDigit];
        for (int i = 1; i <= 6; i++) {
            int digit = data.charAt(i) - '0';
            if (parity[i - 1] == 0) {
                sb.append(EAN13_L_ODD[digit]);
            } else {
                sb.append(EAN13_L_EVEN[digit]);
            }
        }
        sb.append("01010");
        for (int i = 7; i <= 12; i++) {
            int digit = data.charAt(i) - '0';
            sb.append(EAN13_RIGHT[digit]);
        }
        sb.append("101");
        return sb.toString();
    }

    private static String encodeCode128(String text) {
        StringBuilder encoded = new StringBuilder();
        int value = 104;
        if (104 >= CODE128_PATTERNS.length) {
            return simpleEncode(text);
        }
        encoded.append(CODE128_PATTERNS[104]);
        int pos = 1;
        for (char c : text.toCharArray()) {
            int charValue = (int) c - 32;
            if (charValue >= 0 && charValue < CODE128_PATTERNS.length) {
                encoded.append(CODE128_PATTERNS[charValue]);
                value += charValue * pos;
                pos++;
            }
        }
        int checksum = value % 103;
        if (checksum < CODE128_PATTERNS.length) {
            encoded.append(CODE128_PATTERNS[checksum]);
        }
        if (106 < CODE128_PATTERNS.length) {
            encoded.append(CODE128_PATTERNS[106]);
        }
        return encoded.toString();
    }

    private static String simpleEncode(String text) {
        StringBuilder sb = new StringBuilder();
        sb.append("11010");
        for (byte b : text.getBytes()) {
            for (int i = 7; i >= 0; i--) {
                sb.append((b >> i) & 1);
            }
            sb.append("0");
        }
        sb.append("11010");
        return sb.toString();
    }

    private static String buildSvg(String encoded, String content) {
        int moduleWidth = 2;
        int height = 60;
        int quietZone = 10;
        int totalWidth = quietZone * 2 + encoded.length() * moduleWidth;
        int totalHeight = height + 25;

        StringBuilder svg = new StringBuilder();
        svg.append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">",
                totalWidth, totalHeight, totalWidth, totalHeight));
        svg.append(String.format("<rect width=\"%d\" height=\"%d\" fill=\"white\"/>", totalWidth, totalHeight));

        int x = quietZone;
        for (char c : encoded.toCharArray()) {
            if (c == '1') {
                svg.append(String.format("<rect x=\"%d\" y=\"0\" width=\"%d\" height=\"%d\" fill=\"black\"/>",
                        x, moduleWidth, height));
            }
            x += moduleWidth;
        }

        svg.append(String.format("<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" font-family=\"monospace\" font-size=\"12\" fill=\"black\">%s</text>",
                totalWidth / 2, totalHeight - 5, escapeXml(content)));
        svg.append("</svg>");
        return svg.toString();
    }

    private static String emptySvg() {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"40\">"
                + "<rect width=\"100\" height=\"40\" fill=\"white\"/>"
                + "<text x=\"50\" y=\"25\" text-anchor=\"middle\" font-family=\"monospace\" font-size=\"10\" fill=\"gray\">空条码</text>"
                + "</svg>";
    }

    private static String fallbackSvg(String content) {
        int width = Math.max(100, content.length() * 14);
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "\" height=\"40\">"
                + "<rect width=\"" + width + "\" height=\"40\" fill=\"white\"/>"
                + "<text x=\"" + (width / 2) + "\" y=\"25\" text-anchor=\"middle\" font-family=\"monospace\" font-size=\"12\" fill=\"black\">" + escapeXml(content) + "</text>"
                + "</svg>";
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
