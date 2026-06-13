package com.company.material.controller;

import com.company.material.entity.Location;
import com.company.material.entity.Warehouse;
import com.company.material.repository.LocationRepository;
import com.company.material.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Location location) {
        if (location.getLocationCode() == null || location.getWarehouseId() == null
                || location.getZone() == null || location.getRow() == null
                || location.getShelf() == null || location.getPosition() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "库位编码、仓库ID、区、排、层、位为必填"));
        }
        if (!warehouseRepository.existsById(location.getWarehouseId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "所属仓库不存在"));
        }
        if (locationRepository.existsByLocationCode(location.getLocationCode())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "库位编码已存在"));
        }
        location.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(locationRepository.save(location));
    }

    @PostMapping("/batch")
    public ResponseEntity<?> batchCreate(@RequestBody Map<String, Object> body) {
        Long warehouseId = Long.valueOf(body.get("warehouseId").toString());
        String zone = (String) body.get("zone");
        int rowStart = Integer.parseInt(body.get("rowStart").toString());
        int rowEnd = Integer.parseInt(body.get("rowEnd").toString());
        int shelfStart = Integer.parseInt(body.get("shelfStart").toString());
        int shelfEnd = Integer.parseInt(body.get("shelfEnd").toString());
        int posStart = Integer.parseInt(body.get("posStart").toString());
        int posEnd = Integer.parseInt(body.get("posEnd").toString());

        if (!warehouseRepository.existsById(warehouseId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "所属仓库不存在"));
        }

        Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
        String warehouseCode = warehouse != null ? warehouse.getWarehouseCode() : "WH";

        int count = 0;
        for (int r = rowStart; r <= rowEnd; r++) {
            for (int s = shelfStart; s <= shelfEnd; s++) {
                for (int p = posStart; p <= posEnd; p++) {
                    String locationCode = String.format("%s-%s-%02d-%02d-%02d",
                            warehouseCode, zone, r, s, p);
                    if (!locationRepository.existsByLocationCode(locationCode)) {
                        Location loc = new Location();
                        loc.setLocationCode(locationCode);
                        loc.setWarehouseId(warehouseId);
                        loc.setZone(zone);
                        loc.setRow(String.format("%02d", r));
                        loc.setShelf(String.format("%02d", s));
                        loc.setPosition(String.format("%02d", p));
                        loc.setEnabled("是");
                        locationRepository.save(loc);
                        count++;
                    }
                }
            }
        }
        return ResponseEntity.ok(Map.of("message", "批量创建库位成功", "count", count));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String enabled,
            @RequestParam(required = false) String keyword) {
        List<Location> result;
        if (warehouseId != null && enabled != null) {
            result = locationRepository.findByWarehouseIdAndEnabled(warehouseId, enabled);
        } else if (warehouseId != null) {
            result = locationRepository.findByWarehouseId(warehouseId);
        } else if (enabled != null) {
            result = locationRepository.findByEnabled(enabled);
        } else {
            result = locationRepository.findAll();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return locationRepository.findById(id)
                .map(l -> ResponseEntity.ok((Object) l))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Location body) {
        return locationRepository.findById(id).map(l -> {
            if (body.getZone() != null) l.setZone(body.getZone());
            if (body.getRow() != null) l.setRow(body.getRow());
            if (body.getShelf() != null) l.setShelf(body.getShelf());
            if (body.getPosition() != null) l.setPosition(body.getPosition());
            if (body.getEnabled() != null) l.setEnabled(body.getEnabled());
            return ResponseEntity.ok((Object) locationRepository.save(l));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggleEnabled(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String enabled = body.get("enabled");
        if (!"是".equals(enabled) && !"否".equals(enabled)) {
            return ResponseEntity.badRequest().body(Map.of("error", "启用状态值无效"));
        }
        return locationRepository.findById(id).map(l -> {
            l.setEnabled(enabled);
            locationRepository.save(l);
            return ResponseEntity.ok(Map.of("message", "状态更新成功", "enabled", enabled));
        }).orElse(ResponseEntity.notFound().build());
    }
}
