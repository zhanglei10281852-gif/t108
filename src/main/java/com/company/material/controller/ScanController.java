package com.company.material.controller;

import com.company.material.entity.ScanRecord;
import com.company.material.entity.StocktakingItem;
import com.company.material.repository.ScanRecordRepository;
import com.company.material.repository.StocktakingItemRepository;
import com.company.material.service.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scan")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;
    private final ScanRecordRepository scanRecordRepository;
    private final StocktakingItemRepository stocktakingItemRepository;

    @PostMapping("/resolve")
    public ResponseEntity<?> resolveBarcode(@RequestBody Map<String, String> body) {
        String barcode = body.get("barcode");
        if (barcode == null || barcode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "条码不能为空"));
        }
        return ResponseEntity.ok(scanService.resolveBarcode(barcode));
    }

    @PostMapping("/in")
    public ResponseEntity<?> scanIn(@RequestBody Map<String, Object> body) {
        Long materialId = Long.valueOf(body.get("materialId").toString());
        Long locationId = Long.valueOf(body.get("locationId").toString());
        BigDecimal quantity = new BigDecimal(body.get("quantity").toString());
        Long operatorId = body.get("operatorId") != null ? Long.valueOf(body.get("operatorId").toString()) : null;
        String documentNo = (String) body.get("documentNo");
        return ResponseEntity.ok(scanService.scanIn(materialId, locationId, quantity, operatorId, documentNo));
    }

    @PostMapping("/in/batch")
    public ResponseEntity<?> batchScanIn(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        Long operatorId = body.get("operatorId") != null ? Long.valueOf(body.get("operatorId").toString()) : null;
        String documentNo = (String) body.get("documentNo");
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "入库明细不能为空"));
        }
        return ResponseEntity.ok(scanService.batchScanIn(items, operatorId, documentNo));
    }

    @PostMapping("/out")
    public ResponseEntity<?> scanOut(@RequestBody Map<String, Object> body) {
        Long materialId = Long.valueOf(body.get("materialId").toString());
        Long locationId = Long.valueOf(body.get("locationId").toString());
        BigDecimal quantity = new BigDecimal(body.get("quantity").toString());
        Long operatorId = body.get("operatorId") != null ? Long.valueOf(body.get("operatorId").toString()) : null;
        String documentNo = (String) body.get("documentNo");
        return ResponseEntity.ok(scanService.scanOut(materialId, locationId, quantity, operatorId, documentNo));
    }

    @PostMapping("/move")
    public ResponseEntity<?> moveStock(@RequestBody Map<String, Object> body) {
        Long materialId = Long.valueOf(body.get("materialId").toString());
        Long sourceLocationId = Long.valueOf(body.get("sourceLocationId").toString());
        Long targetLocationId = Long.valueOf(body.get("targetLocationId").toString());
        BigDecimal quantity = new BigDecimal(body.get("quantity").toString());
        Long operatorId = body.get("operatorId") != null ? Long.valueOf(body.get("operatorId").toString()) : null;
        return ResponseEntity.ok(scanService.moveStock(materialId, sourceLocationId, targetLocationId, quantity, operatorId));
    }

    @PostMapping("/stocktaking/start")
    public ResponseEntity<?> startStocktaking(@RequestBody Map<String, Object> body) {
        Long locationId = Long.valueOf(body.get("locationId").toString());
        Long operatorId = body.get("operatorId") != null ? Long.valueOf(body.get("operatorId").toString()) : null;
        return ResponseEntity.ok(scanService.startStocktaking(locationId, operatorId));
    }

    @PostMapping("/stocktaking/submit")
    public ResponseEntity<?> submitStocktakingItem(@RequestBody Map<String, Object> body) {
        Long itemId = Long.valueOf(body.get("itemId").toString());
        BigDecimal actualQuantity = new BigDecimal(body.get("actualQuantity").toString());
        Long operatorId = body.get("operatorId") != null ? Long.valueOf(body.get("operatorId").toString()) : null;
        return ResponseEntity.ok(scanService.submitStocktakingItem(itemId, actualQuantity, operatorId));
    }

    @PostMapping("/stocktaking/confirm")
    public ResponseEntity<?> confirmStocktaking(@RequestBody Map<String, String> body) {
        String batchNo = body.get("batchNo");
        Long operatorId = body.get("operatorId") != null ? Long.valueOf(body.get("operatorId")) : null;
        if (batchNo == null || batchNo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "盘点批次号不能为空"));
        }
        return ResponseEntity.ok(scanService.confirmStocktaking(batchNo, operatorId));
    }

    @GetMapping("/stocktaking/batch/{batchNo}")
    public ResponseEntity<?> getStocktakingByBatch(@PathVariable String batchNo) {
        List<StocktakingItem> items = stocktakingItemRepository.findByBatchNo(batchNo);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/query/material-locations")
    public ResponseEntity<?> queryMaterialLocations(@RequestParam Long materialId) {
        return ResponseEntity.ok(scanService.queryMaterialLocations(materialId));
    }

    @GetMapping("/query/location-materials")
    public ResponseEntity<?> queryLocationMaterials(@RequestParam Long locationId) {
        return ResponseEntity.ok(scanService.queryLocationMaterials(locationId));
    }

    @GetMapping("/records")
    public ResponseEntity<?> listScanRecords(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String documentNo) {
        List<ScanRecord> result;
        if (operationType != null) {
            result = scanRecordRepository.findByOperationType(operationType);
        } else if (operatorId != null) {
            result = scanRecordRepository.findByOperatorId(operatorId);
        } else if (documentNo != null) {
            result = scanRecordRepository.findByDocumentNo(documentNo);
        } else {
            result = scanRecordRepository.findAll();
        }
        return ResponseEntity.ok(result);
    }
}
