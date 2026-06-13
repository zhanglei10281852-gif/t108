package com.company.material.controller;

import com.company.material.entity.Location;
import com.company.material.entity.Material;
import com.company.material.entity.Warehouse;
import com.company.material.repository.LocationRepository;
import com.company.material.repository.MaterialRepository;
import com.company.material.repository.WarehouseRepository;
import com.company.material.util.BarcodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/barcodes")
@RequiredArgsConstructor
public class BarcodeController {

    private final MaterialRepository materialRepository;
    private final LocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;

    @GetMapping("/material/{id}")
    public ResponseEntity<?> getMaterialBarcode(@PathVariable Long id) {
        Material material = materialRepository.findById(id).orElse(null);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }
        String barcode = BarcodeUtil.generateMaterialBarcode(material.getMaterialCode());
        Map<String, Object> data = new HashMap<>();
        data.put("id", material.getId());
        data.put("materialCode", material.getMaterialCode());
        data.put("name", material.getName());
        data.put("specification", material.getSpecification());
        data.put("barcode", barcode);
        data.put("barcodeText", barcode);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/material/{id}/svg")
    public ResponseEntity<String> getMaterialBarcodeSvg(@PathVariable Long id) {
        Material material = materialRepository.findById(id).orElse(null);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }
        String barcode = BarcodeUtil.generateMaterialBarcode(material.getMaterialCode());
        String svg = BarcodeUtil.generateSvgBarcode(barcode);
        return ResponseEntity.ok().contentType(MediaType.valueOf("image/svg+xml")).body(svg);
    }

    @GetMapping("/location/{id}")
    public ResponseEntity<?> getLocationBarcode(@PathVariable Long id) {
        Location location = locationRepository.findById(id).orElse(null);
        if (location == null) {
            return ResponseEntity.notFound().build();
        }
        String barcode = BarcodeUtil.generateLocationBarcode(location.getLocationCode());
        Map<String, Object> data = new HashMap<>();
        data.put("id", location.getId());
        data.put("locationCode", location.getLocationCode());
        data.put("barcode", barcode);
        data.put("barcodeText", barcode);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/location/{id}/svg")
    public ResponseEntity<String> getLocationBarcodeSvg(@PathVariable Long id) {
        Location location = locationRepository.findById(id).orElse(null);
        if (location == null) {
            return ResponseEntity.notFound().build();
        }
        String barcode = BarcodeUtil.generateLocationBarcode(location.getLocationCode());
        String svg = BarcodeUtil.generateSvgBarcode(barcode);
        return ResponseEntity.ok().contentType(MediaType.valueOf("image/svg+xml")).body(svg);
    }

    @GetMapping("/document/{documentNo}")
    public ResponseEntity<?> getDocumentBarcode(@PathVariable String documentNo) {
        String barcode = BarcodeUtil.generateDocumentBarcode(documentNo);
        Map<String, Object> data = new HashMap<>();
        data.put("documentNo", documentNo);
        data.put("barcode", barcode);
        data.put("barcodeText", barcode);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/document/{documentNo}/svg")
    public ResponseEntity<String> getDocumentBarcodeSvg(@PathVariable String documentNo) {
        String barcode = BarcodeUtil.generateDocumentBarcode(documentNo);
        String svg = BarcodeUtil.generateSvgBarcode(barcode);
        return ResponseEntity.ok().contentType(MediaType.valueOf("image/svg+xml")).body(svg);
    }

    @PostMapping("/material/batch-labels")
    public ResponseEntity<?> batchMaterialLabels(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> materialIds = (List<Number>) body.get("materialIds");
        if (materialIds == null || materialIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "物料ID列表不能为空"));
        }
        List<Map<String, Object>> labels = new ArrayList<>();
        for (Number mid : materialIds) {
            Material material = materialRepository.findById(mid.longValue()).orElse(null);
            if (material == null) continue;
            String barcode = BarcodeUtil.generateMaterialBarcode(material.getMaterialCode());
            Map<String, Object> label = new HashMap<>();
            label.put("id", material.getId());
            label.put("materialCode", material.getMaterialCode());
            label.put("name", material.getName());
            label.put("specification", material.getSpecification());
            label.put("unit", material.getUnit());
            label.put("barcode", barcode);
            label.put("svg", BarcodeUtil.generateSvgBarcode(barcode));
            labels.add(label);
        }
        return ResponseEntity.ok(Map.of("labels", labels, "count", labels.size()));
    }

    @PostMapping("/location/batch-labels")
    public ResponseEntity<?> batchLocationLabels(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> locationIds = (List<Number>) body.get("locationIds");
        if (locationIds == null || locationIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "库位ID列表不能为空"));
        }
        List<Map<String, Object>> labels = new ArrayList<>();
        for (Number lid : locationIds) {
            Location location = locationRepository.findById(lid.longValue()).orElse(null);
            if (location == null) continue;
            Warehouse warehouse = warehouseRepository.findById(location.getWarehouseId()).orElse(null);
            String barcode = BarcodeUtil.generateLocationBarcode(location.getLocationCode());
            Map<String, Object> label = new HashMap<>();
            label.put("id", location.getId());
            label.put("locationCode", location.getLocationCode());
            label.put("warehouseName", warehouse != null ? warehouse.getName() : "");
            label.put("zone", location.getZone());
            label.put("row", location.getRow());
            label.put("shelf", location.getShelf());
            label.put("position", location.getPosition());
            label.put("barcode", barcode);
            label.put("svg", BarcodeUtil.generateSvgBarcode(barcode));
            labels.add(label);
        }
        return ResponseEntity.ok(Map.of("labels", labels, "count", labels.size()));
    }

    @GetMapping("/generate")
    public ResponseEntity<?> generateBarcode(@RequestParam String type, @RequestParam String code) {
        String barcode;
        switch (type) {
            case "物料":
                barcode = BarcodeUtil.generateMaterialBarcode(code);
                break;
            case "库位":
                barcode = BarcodeUtil.generateLocationBarcode(code);
                break;
            case "单据":
                barcode = BarcodeUtil.generateDocumentBarcode(code);
                break;
            default:
                return ResponseEntity.badRequest().body(Map.of("error", "不支持的条码类型"));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put("code", code);
        data.put("barcode", barcode);
        data.put("barcodeText", barcode);
        data.put("svg", BarcodeUtil.generateSvgBarcode(barcode));
        return ResponseEntity.ok(data);
    }
}
