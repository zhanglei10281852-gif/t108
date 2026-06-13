package com.company.material.util;

import java.util.HashMap;
import java.util.Map;

public class BarcodeUtil {

    private static final String CODE128_PATTERN = "11011001100,11001101100,11001100110,10010011000,10010001100,"
            + "10001001100,10011001000,10011000100,10001100100,11001001000,"
            + "11001000100,11000100100,10110011100,10011011100,10011001110,"
            + "10111001100,10011101100,10011100110,11001110010,11001011100,"
            + "11001001110,11011100100,11001110100,11101101110,11101001100,"
            + "11100101100,11100100110,11101100100,11100110100,11100110010,"
            + "11011011000,11011000110,11000110110,10100011000,10001011000,"
            + "10001000110,10110001000,10001101000,10001100010,11010001000,"
            + "11000101000,11000100010,10110111000,10110001110,10001101110,"
            + "10111011000,10111000110,10001110110,11101110110,11010000100,"
            + "11010010000,11010011100,1100011101011";

    private static final String[] CODE128_B = CODE128_PATTERN.split(",");

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
        String encoded = encodeCode128(content);
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

    private static String encodeCode128(String text) {
        StringBuilder encoded = new StringBuilder();
        int value = 104;
        encoded.append(CODE128_B[104]);
        int pos = 1;
        for (char c : text.toCharArray()) {
            int charValue = (int) c - 32;
            if (charValue >= 0 && charValue < 106) {
                encoded.append(CODE128_B[charValue]);
                value += charValue * pos;
                pos++;
            }
        }
        int checksum = value % 103;
        encoded.append(CODE128_B[checksum]);
        encoded.append(CODE128_B[106]);
        return encoded.toString();
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
