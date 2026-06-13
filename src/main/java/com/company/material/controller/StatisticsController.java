package com.company.material.controller;

import com.company.material.entity.Location;
import com.company.material.entity.Warehouse;
import com.company.material.repository.InventoryLedgerRepository;
import com.company.material.repository.LocationRepository;
import com.company.material.repository.ScanRecordRepository;
import com.company.material.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final ScanRecordRepository scanRecordRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final LocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;

    @GetMapping("/scan/daily")
    public ResponseEntity<?> dailyScanStats(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now().plusDays(1);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Object[]> typeCounts = scanRecordRepository.countByOperationTypeInRange(start, end);
        List<Map<String, Object>> stats = new ArrayList<>();
        for (Object[] row : typeCounts) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("operationType", row[0]);
            stat.put("count", row[1]);
            stats.add(stat);
        }
        return ResponseEntity.ok(Map.of("startDate", startDate, "endDate", endDate, "stats", stats));
    }

    @GetMapping("/scan/by-type")
    public ResponseEntity<?> scanStatsByType() {
        List<Object[]> typeCounts = scanRecordRepository.countByOperationTypeAll();
        List<Map<String, Object>> stats = new ArrayList<>();
        for (Object[] row : typeCounts) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("operationType", row[0]);
            stat.put("count", row[1]);
            stats.add(stat);
        }
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/location/turnover")
    public ResponseEntity<?> locationTurnover() {
        List<Object[]> turnovers = scanRecordRepository.countTurnoverByLocation();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : turnovers) {
            Map<String, Object> item = new HashMap<>();
            Long locationId = (Long) row[0];
            Long count = (Long) row[1];
            String locationCode = locationRepository.findById(locationId)
                    .map(Location::getLocationCode).orElse("未知");
            item.put("locationId", locationId);
            item.put("locationCode", locationCode);
            item.put("turnoverCount", count);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/location/utilization")
    public ResponseEntity<?> locationUtilization(
            @RequestParam(required = false) Long warehouseId) {
        long totalLocations;
        long occupiedLocations;

        if (warehouseId != null) {
            totalLocations = locationRepository.countByWarehouseId(warehouseId);
            occupiedLocations = inventoryLedgerRepository.countOccupiedLocations(warehouseId);
        } else {
            totalLocations = locationRepository.count();
            occupiedLocations = inventoryLedgerRepository.countAllOccupiedLocations();
        }

        double utilizationRate = totalLocations > 0 ? (double) occupiedLocations / totalLocations * 100 : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("totalLocations", totalLocations);
        result.put("occupiedLocations", occupiedLocations);
        result.put("emptyLocations", totalLocations - occupiedLocations);
        result.put("utilizationRate", String.format("%.2f", utilizationRate) + "%");
        if (warehouseId != null) {
            Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
            result.put("warehouseName", warehouse != null ? warehouse.getName() : "");
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/scan/operator-workload")
    public ResponseEntity<?> operatorWorkload(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now().plusDays(1);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Object[]> workloads = scanRecordRepository.countByOperatorInRange(start, end);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : workloads) {
            Map<String, Object> item = new HashMap<>();
            item.put("operatorId", row[0]);
            item.put("operatorName", row[1]);
            item.put("operationCount", row[2]);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/scan/abnormal")
    public ResponseEntity<?> abnormalScanCount(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        long count;
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            count = scanRecordRepository.countAbnormalInRange(start, end);
        } else {
            count = scanRecordRepository.countAbnormalAll();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("abnormalCount", count);
        if (startDate != null) result.put("startDate", startDate);
        if (endDate != null) result.put("endDate", endDate);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalLocations", locationRepository.count());
        overview.put("occupiedLocations", inventoryLedgerRepository.countAllOccupiedLocations());
        overview.put("totalAbnormal", scanRecordRepository.countAbnormalAll());

        List<Object[]> typeCounts = scanRecordRepository.countByOperationTypeAll();
        List<Map<String, Object>> typeStats = new ArrayList<>();
        for (Object[] row : typeCounts) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("operationType", row[0]);
            stat.put("count", row[1]);
            typeStats.add(stat);
        }
        overview.put("scanTypeStats", typeStats);

        List<Object[]> turnovers = scanRecordRepository.countTurnoverByLocation();
        List<Map<String, Object>> topTurnover = new ArrayList<>();
        int limit = Math.min(10, turnovers.size());
        for (int i = 0; i < limit; i++) {
            Object[] row = turnovers.get(i);
            Map<String, Object> item = new HashMap<>();
            Long locationId = (Long) row[0];
            item.put("locationCode", locationRepository.findById(locationId)
                    .map(Location::getLocationCode).orElse("未知"));
            item.put("turnoverCount", row[1]);
            topTurnover.add(item);
        }
        overview.put("topTurnoverLocations", topTurnover);
        return ResponseEntity.ok(overview);
    }
}
