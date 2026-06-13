package com.company.material.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BarcodeUtilTest {

    @Test
    void testMaterialBarcodeGeneration() {
        String barcode = BarcodeUtil.generateMaterialBarcode("MAT0001");
        assertNotNull(barcode);
        assertEquals(13, barcode.length());
        assertTrue(barcode.matches("\\d{13}"));
    }

    @Test
    void testLocationBarcodeGeneration() {
        String barcode = BarcodeUtil.generateLocationBarcode("WH001-A-01-02-03");
        assertEquals("LOC-WH001-A-01-02-03", barcode);
    }

    @Test
    void testDocumentBarcodeGeneration() {
        String barcode = BarcodeUtil.generateDocumentBarcode("RK20240101001");
        assertEquals("DOC-RK20240101001", barcode);
    }

    @Test
    void testEAN13CheckDigit() {
        String barcode = BarcodeUtil.generateMaterialBarcode("MAT0001");
        int lastDigit = Character.getNumericValue(barcode.charAt(12));
        int computed = BarcodeUtil.validateEAN13(barcode) ? lastDigit : -1;
        assertTrue(BarcodeUtil.validateEAN13(barcode));
    }

    @Test
    void testSvgBarcodeGenerationForMaterial() {
        String barcode = BarcodeUtil.generateMaterialBarcode("MAT0001");
        String svg = BarcodeUtil.generateSvgBarcode(barcode);
        assertNotNull(svg);
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.endsWith("</svg>"));
        assertTrue(svg.contains("rect"));
    }

    @Test
    void testSvgBarcodeGenerationForLocation() {
        String barcode = BarcodeUtil.generateLocationBarcode("WH001-A-01-02-03");
        String svg = BarcodeUtil.generateSvgBarcode(barcode);
        assertNotNull(svg);
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.endsWith("</svg>"));
        assertTrue(svg.contains("rect"));
    }

    @Test
    void testSvgBarcodeGenerationForDocument() {
        String barcode = BarcodeUtil.generateDocumentBarcode("RK20240101001");
        String svg = BarcodeUtil.generateSvgBarcode(barcode);
        assertNotNull(svg);
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.endsWith("</svg>"));
    }

    @Test
    void testParseBarcodeType() {
        assertEquals("物料", BarcodeUtil.parseBarcodeType("0000001000006"));
        assertEquals("库位", BarcodeUtil.parseBarcodeType("LOC-WH001-A-01-01-01"));
        assertEquals("单据", BarcodeUtil.parseBarcodeType("DOC-RK20240101001"));
    }

    @Test
    void testParseLocationCodeFromBarcode() {
        assertEquals("WH001-A-01-01-01", BarcodeUtil.parseLocationCodeFromBarcode("LOC-WH001-A-01-01-01"));
        assertNull(BarcodeUtil.parseLocationCodeFromBarcode("MAT0001"));
    }

    @Test
    void testParseDocumentNoFromBarcode() {
        assertEquals("RK20240101001", BarcodeUtil.parseDocumentNoFromBarcode("DOC-RK20240101001"));
        assertNull(BarcodeUtil.parseDocumentNoFromBarcode("MAT0001"));
    }

    @Test
    void testSvgEmptyBarcode() {
        String svg = BarcodeUtil.generateSvgBarcode("");
        assertNotNull(svg);
        assertTrue(svg.startsWith("<svg"));
    }

    @Test
    void testSvgNullBarcode() {
        String svg = BarcodeUtil.generateSvgBarcode(null);
        assertNotNull(svg);
        assertTrue(svg.startsWith("<svg"));
    }

    @Test
    void testBatchMaterialLabelsSvgGeneration() {
        String[] codes = {"MAT0001", "MAT0002", "MAT0003"};
        for (String code : codes) {
            String barcode = BarcodeUtil.generateMaterialBarcode(code);
            String svg = BarcodeUtil.generateSvgBarcode(barcode);
            assertNotNull(svg);
            assertTrue(svg.startsWith("<svg"));
            assertTrue(svg.contains("</svg>"));
            assertFalse(svg.contains("空条码"));
        }
    }

    @Test
    void testEAN13SvgUsesEAN13Encoding() {
        String barcode = BarcodeUtil.generateMaterialBarcode("MAT0001");
        String svg = BarcodeUtil.generateSvgBarcode(barcode);
        assertNotNull(svg);
        int rectCount = 0;
        int idx = 0;
        while ((idx = svg.indexOf("<rect", idx)) != -1) {
            rectCount++;
            idx += 5;
        }
        assertTrue(rectCount > 10, "EAN-13 barcode should have many bars");
    }

    @Test
    void testCode128SvgGeneration() {
        String svg = BarcodeUtil.generateSvgBarcode("LOC-WH001-A-01-01-01");
        assertNotNull(svg);
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.contains("rect"));
        int rectCount = 0;
        int idx = 0;
        while ((idx = svg.indexOf("<rect", idx)) != -1) {
            rectCount++;
            idx += 5;
        }
        assertTrue(rectCount > 5, "Code 128 barcode should have many bars");
    }
}
