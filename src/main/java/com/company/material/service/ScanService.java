package com.company.material.service;

import com.company.material.entity.*;
import com.company.material.repository.*;
import com.company.material.util.BarcodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScanService {

    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final ScanRecordRepository scanRecordRepository;
    private final StocktakingItemRepository stocktakingItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public Map<String, Object> scanIn(Long materialId, Long locationId, BigDecimal quantity,
                                       Long operatorId, String documentNo) {
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material == null) {
            return errorRecord("入库", materialId, locationId, quantity, operatorId, documentNo, "物料不存在");
        }
        if (!"在用".equals(material.getStatus())) {
            return errorRecord("入库", materialId, locationId, quantity, operatorId, documentNo, "物料已停用");
        }

        Location location = locationRepository.findById(locationId).orElse(null);
        if (location == null) {
            return errorRecord("入库", materialId, locationId, quantity, operatorId, documentNo, "库位不存在");
        }
        if (!"是".equals(location.getEnabled())) {
            return errorRecord("入库", materialId, locationId, quantity, operatorId, documentNo, "库位已停用");
        }

        String operatorName = getOperatorName(operatorId);

        InventoryLedger ledger = inventoryLedgerRepository
                .findByMaterialIdAndWarehouseIdAndLocationId(materialId, location.getWarehouseId(), locationId)
                .orElse(null);
        if (ledger == null) {
            ledger = new InventoryLedger();
            ledger.setMaterialId(materialId);
            ledger.setWarehouseId(location.getWarehouseId());
            ledger.setLocationId(locationId);
            ledger.setQuantity(BigDecimal.ZERO);
        }
        ledger.setQuantity(ledger.getQuantity().add(quantity));
        inventoryLedgerRepository.save(ledger);

        ScanRecord record = new ScanRecord();
        record.setOperationType("入库");
        record.setMaterialId(materialId);
        record.setLocationId(locationId);
        record.setQuantity(quantity);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setDocumentNo(documentNo);
        record.setStatus("成功");
        scanRecordRepository.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "扫码入库成功");
        result.put("materialCode", material.getMaterialCode());
        result.put("materialName", material.getName());
        result.put("locationCode", location.getLocationCode());
        result.put("quantity", quantity);
        result.put("currentStock", ledger.getQuantity());
        return result;
    }

    @Transactional
    public List<Map<String, Object>> batchScanIn(List<Map<String, Object>> items, Long operatorId, String documentNo) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Long materialId = Long.valueOf(item.get("materialId").toString());
            Long locationId = Long.valueOf(item.get("locationId").toString());
            BigDecimal quantity = new BigDecimal(item.get("quantity").toString());
            results.add(scanIn(materialId, locationId, quantity, operatorId, documentNo));
        }
        return results;
    }

    @Transactional
    public Map<String, Object> scanOut(Long materialId, Long locationId, BigDecimal quantity,
                                        Long operatorId, String documentNo) {
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material == null) {
            return errorRecord("出库", materialId, locationId, quantity, operatorId, documentNo, "物料不存在");
        }

        Location location = locationRepository.findById(locationId).orElse(null);
        if (location == null) {
            return errorRecord("出库", materialId, locationId, quantity, operatorId, documentNo, "库位不存在");
        }

        InventoryLedger ledger = inventoryLedgerRepository
                .findByMaterialIdAndWarehouseIdAndLocationId(materialId, location.getWarehouseId(), locationId)
                .orElse(null);

        if (ledger == null || ledger.getQuantity().compareTo(quantity) < 0) {
            BigDecimal currentStock = (ledger == null) ? BigDecimal.ZERO : ledger.getQuantity();
            return errorRecord("出库", materialId, locationId, quantity, operatorId, documentNo,
                    "库位库存不足，当前库存: " + currentStock);
        }

        String operatorName = getOperatorName(operatorId);

        ledger.setQuantity(ledger.getQuantity().subtract(quantity));
        if (ledger.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            inventoryLedgerRepository.delete(ledger);
        } else {
            inventoryLedgerRepository.save(ledger);
        }

        ScanRecord record = new ScanRecord();
        record.setOperationType("出库");
        record.setMaterialId(materialId);
        record.setLocationId(locationId);
        record.setQuantity(quantity);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setDocumentNo(documentNo);
        record.setStatus("成功");
        scanRecordRepository.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "扫码出库成功");
        result.put("materialCode", material.getMaterialCode());
        result.put("materialName", material.getName());
        result.put("locationCode", location.getLocationCode());
        result.put("quantity", quantity);
        result.put("currentStock", ledger.getQuantity());
        return result;
    }

    @Transactional
    public Map<String, Object> moveStock(Long materialId, Long sourceLocationId, Long targetLocationId,
                                          BigDecimal quantity, Long operatorId) {
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material == null) {
            return errorRecord("移库", materialId, sourceLocationId, quantity, operatorId, null, "物料不存在");
        }

        Location sourceLocation = locationRepository.findById(sourceLocationId).orElse(null);
        if (sourceLocation == null) {
            return errorRecord("移库", materialId, sourceLocationId, quantity, operatorId, null, "源库位不存在");
        }
        if (!"是".equals(sourceLocation.getEnabled())) {
            return errorRecord("移库", materialId, sourceLocationId, quantity, operatorId, null, "源库位已停用");
        }

        Location targetLocation = locationRepository.findById(targetLocationId).orElse(null);
        if (targetLocation == null) {
            return errorRecord("移库", materialId, sourceLocationId, quantity, operatorId, null, "目标库位不存在");
        }
        if (!"是".equals(targetLocation.getEnabled())) {
            return errorRecord("移库", materialId, sourceLocationId, quantity, operatorId, null, "目标库位已停用");
        }

        InventoryLedger sourceLedger = inventoryLedgerRepository
                .findByMaterialIdAndWarehouseIdAndLocationId(materialId, sourceLocation.getWarehouseId(), sourceLocationId)
                .orElse(null);

        if (sourceLedger == null || sourceLedger.getQuantity().compareTo(quantity) < 0) {
            BigDecimal currentStock = (sourceLedger == null) ? BigDecimal.ZERO : sourceLedger.getQuantity();
            return errorRecord("移库", materialId, sourceLocationId, quantity, operatorId, null,
                    "源库位库存不足，当前库存: " + currentStock);
        }

        String operatorName = getOperatorName(operatorId);

        sourceLedger.setQuantity(sourceLedger.getQuantity().subtract(quantity));
        if (sourceLedger.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            inventoryLedgerRepository.delete(sourceLedger);
        } else {
            inventoryLedgerRepository.save(sourceLedger);
        }

        InventoryLedger targetLedger = inventoryLedgerRepository
                .findByMaterialIdAndWarehouseIdAndLocationId(materialId, targetLocation.getWarehouseId(), targetLocationId)
                .orElse(null);
        if (targetLedger == null) {
            targetLedger = new InventoryLedger();
            targetLedger.setMaterialId(materialId);
            targetLedger.setWarehouseId(targetLocation.getWarehouseId());
            targetLedger.setLocationId(targetLocationId);
            targetLedger.setQuantity(BigDecimal.ZERO);
        }
        targetLedger.setQuantity(targetLedger.getQuantity().add(quantity));
        inventoryLedgerRepository.save(targetLedger);

        ScanRecord record = new ScanRecord();
        record.setOperationType("移库");
        record.setMaterialId(materialId);
        record.setLocationId(sourceLocationId);
        record.setTargetLocationId(targetLocationId);
        record.setQuantity(quantity);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setStatus("成功");
        scanRecordRepository.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "移库成功");
        result.put("materialCode", material.getMaterialCode());
        result.put("materialName", material.getName());
        result.put("sourceLocationCode", sourceLocation.getLocationCode());
        result.put("targetLocationCode", targetLocation.getLocationCode());
        result.put("quantity", quantity);
        return result;
    }

    @Transactional
    public Map<String, Object> startStocktaking(Long locationId, Long operatorId) {
        Location location = locationRepository.findById(locationId).orElse(null);
        if (location == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "库位不存在");
            return result;
        }

        String batchNo = "PD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String operatorName = getOperatorName(operatorId);

        List<InventoryLedger> ledgers = inventoryLedgerRepository.findStockByLocationId(locationId);
        List<StocktakingItem> items = new ArrayList<>();

        for (InventoryLedger ledger : ledgers) {
            StocktakingItem item = new StocktakingItem();
            item.setLocationId(locationId);
            item.setMaterialId(ledger.getMaterialId());
            item.setSystemQuantity(ledger.getQuantity());
            item.setActualQuantity(null);
            item.setDifference(null);
            item.setOperatorId(operatorId);
            item.setOperatorName(operatorName);
            item.setStatus("待盘点");
            item.setBatchNo(batchNo);
            stocktakingItemRepository.save(item);
            items.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "盘点任务已创建");
        result.put("batchNo", batchNo);
        result.put("locationCode", location.getLocationCode());
        result.put("itemCount", items.size());
        result.put("items", items);
        return result;
    }

    @Transactional
    public Map<String, Object> submitStocktakingItem(Long itemId, BigDecimal actualQuantity, Long operatorId) {
        StocktakingItem item = stocktakingItemRepository.findById(itemId).orElse(null);
        if (item == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "盘点项不存在");
            return result;
        }

        String operatorName = getOperatorName(operatorId);
        item.setActualQuantity(actualQuantity);
        item.setDifference(actualQuantity.subtract(item.getSystemQuantity()));
        item.setStatus("已盘点");
        item.setOperatorId(operatorId);
        item.setOperatorName(operatorName);
        item.setScanTime(LocalDateTime.now());
        stocktakingItemRepository.save(item);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "盘点数据已提交");
        result.put("itemId", item.getId());
        result.put("systemQuantity", item.getSystemQuantity());
        result.put("actualQuantity", item.getActualQuantity());
        result.put("difference", item.getDifference());
        return result;
    }

    @Transactional
    public Map<String, Object> confirmStocktaking(String batchNo, Long operatorId) {
        List<StocktakingItem> items = stocktakingItemRepository.findByBatchNo(batchNo);
        List<Map<String, Object>> differences = new ArrayList<>();
        String operatorName = getOperatorName(operatorId);

        for (StocktakingItem item : items) {
            if (!"已盘点".equals(item.getStatus())) continue;

            if (item.getDifference() != null && item.getDifference().compareTo(BigDecimal.ZERO) != 0) {
                Map<String, Object> diff = new HashMap<>();
                diff.put("itemId", item.getId());
                diff.put("materialId", item.getMaterialId());
                diff.put("systemQuantity", item.getSystemQuantity());
                diff.put("actualQuantity", item.getActualQuantity());
                diff.put("difference", item.getDifference());
                differences.add(diff);

                Location location = locationRepository.findById(item.getLocationId()).orElse(null);
                if (location == null) continue;

                InventoryLedger ledger = inventoryLedgerRepository
                        .findByMaterialIdAndWarehouseIdAndLocationId(item.getMaterialId(), location.getWarehouseId(), item.getLocationId())
                        .orElse(null);

                if (ledger != null) {
                    BigDecimal newQty = item.getActualQuantity();
                    if (newQty.compareTo(BigDecimal.ZERO) == 0) {
                        inventoryLedgerRepository.delete(ledger);
                    } else {
                        ledger.setQuantity(newQty);
                        inventoryLedgerRepository.save(ledger);
                    }
                } else if (item.getActualQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    ledger = new InventoryLedger();
                    ledger.setMaterialId(item.getMaterialId());
                    ledger.setWarehouseId(location.getWarehouseId());
                    ledger.setLocationId(item.getLocationId());
                    ledger.setQuantity(item.getActualQuantity());
                    inventoryLedgerRepository.save(ledger);
                }

                item.setStatus("已确认");
                stocktakingItemRepository.save(item);

                ScanRecord record = new ScanRecord();
                record.setOperationType("盘点");
                record.setMaterialId(item.getMaterialId());
                record.setLocationId(item.getLocationId());
                record.setQuantity(item.getDifference().abs());
                record.setOperatorId(operatorId);
                record.setOperatorName(operatorName);
                record.setStatus("成功");
                scanRecordRepository.save(record);
            } else {
                item.setStatus("无差异");
                stocktakingItemRepository.save(item);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "盘点已确认");
        result.put("batchNo", batchNo);
        result.put("totalItems", items.size());
        result.put("differenceCount", differences.size());
        result.put("differences", differences);
        return result;
    }

    public Map<String, Object> resolveBarcode(String barcode) {
        Map<String, Object> result = new HashMap<>();
        String type = BarcodeUtil.parseBarcodeType(barcode);

        switch (type) {
            case "库位": {
                String locationCode = BarcodeUtil.parseLocationCodeFromBarcode(barcode);
                Location location = locationRepository.findByLocationCode(locationCode).orElse(null);
                if (location == null) {
                    result.put("success", false);
                    result.put("error", "库位不存在: " + locationCode);
                    recordAbnormalScan("库位", barcode);
                    return result;
                }
                result.put("success", true);
                result.put("type", "库位");
                result.put("id", location.getId());
                result.put("code", location.getLocationCode());
                result.put("warehouseId", location.getWarehouseId());
                result.put("enabled", location.getEnabled());
                return result;
            }
            case "单据": {
                String docNo = BarcodeUtil.parseDocumentNoFromBarcode(barcode);
                result.put("success", true);
                result.put("type", "单据");
                result.put("documentNo", docNo);
                return result;
            }
            default: {
                Material material = materialRepository.findByMaterialCode(barcode).orElse(null);
                if (material == null) {
                    String eanCode = BarcodeUtil.generateMaterialBarcode(barcode);
                    result.put("success", false);
                    result.put("error", "物料不存在: " + barcode);
                    recordAbnormalScan("物料", barcode);
                    return result;
                }
                result.put("success", true);
                result.put("type", "物料");
                result.put("id", material.getId());
                result.put("code", material.getMaterialCode());
                result.put("name", material.getName());
                result.put("specification", material.getSpecification());
                result.put("unit", material.getUnit());
                result.put("status", material.getStatus());
                return result;
            }
        }
    }

    public Map<String, Object> queryMaterialLocations(Long materialId) {
        Map<String, Object> result = new HashMap<>();
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material == null) {
            result.put("success", false);
            result.put("error", "物料不存在");
            return result;
        }
        List<InventoryLedger> ledgers = inventoryLedgerRepository.findStockByMaterialId(materialId);
        List<Map<String, Object>> locationList = new ArrayList<>();
        for (InventoryLedger ledger : ledgers) {
            Map<String, Object> loc = new HashMap<>();
            loc.put("locationId", ledger.getLocationId());
            Location location = locationRepository.findById(ledger.getLocationId()).orElse(null);
            loc.put("locationCode", location != null ? location.getLocationCode() : "未知");
            Warehouse warehouse = warehouseRepository.findById(ledger.getWarehouseId()).orElse(null);
            loc.put("warehouseName", warehouse != null ? warehouse.getName() : "未知");
            loc.put("quantity", ledger.getQuantity());
            locationList.add(loc);
        }
        result.put("success", true);
        result.put("materialId", materialId);
        result.put("materialCode", material.getMaterialCode());
        result.put("materialName", material.getName());
        result.put("locations", locationList);
        return result;
    }

    public Map<String, Object> queryLocationMaterials(Long locationId) {
        Map<String, Object> result = new HashMap<>();
        Location location = locationRepository.findById(locationId).orElse(null);
        if (location == null) {
            result.put("success", false);
            result.put("error", "库位不存在");
            return result;
        }
        List<InventoryLedger> ledgers = inventoryLedgerRepository.findStockByLocationId(locationId);
        List<Map<String, Object>> materialList = new ArrayList<>();
        for (InventoryLedger ledger : ledgers) {
            Map<String, Object> mat = new HashMap<>();
            mat.put("materialId", ledger.getMaterialId());
            Material material = materialRepository.findById(ledger.getMaterialId()).orElse(null);
            mat.put("materialCode", material != null ? material.getMaterialCode() : "未知");
            mat.put("materialName", material != null ? material.getName() : "未知");
            mat.put("specification", material != null ? material.getSpecification() : "");
            mat.put("unit", material != null ? material.getUnit() : "");
            mat.put("quantity", ledger.getQuantity());
            materialList.add(mat);
        }
        Warehouse warehouse = warehouseRepository.findById(location.getWarehouseId()).orElse(null);
        result.put("success", true);
        result.put("locationId", locationId);
        result.put("locationCode", location.getLocationCode());
        result.put("warehouseName", warehouse != null ? warehouse.getName() : "未知");
        result.put("materials", materialList);
        return result;
    }

    private Map<String, Object> errorRecord(String operationType, Long materialId, Long locationId,
                                             BigDecimal quantity, Long operatorId, String documentNo, String errorMsg) {
        String operatorName = getOperatorName(operatorId);
        ScanRecord record = new ScanRecord();
        record.setOperationType(operationType);
        record.setMaterialId(materialId != null ? materialId : 0L);
        record.setLocationId(locationId != null ? locationId : 0L);
        record.setQuantity(quantity != null ? quantity : BigDecimal.ZERO);
        record.setOperatorId(operatorId != null ? operatorId : 0L);
        record.setOperatorName(operatorName);
        record.setDocumentNo(documentNo);
        record.setStatus("异常");
        scanRecordRepository.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", errorMsg);
        return result;
    }

    private void recordAbnormalScan(String type, String barcode) {
        ScanRecord record = new ScanRecord();
        record.setOperationType("扫码异常");
        record.setMaterialId(0L);
        record.setLocationId(0L);
        record.setQuantity(BigDecimal.ZERO);
        record.setOperatorId(0L);
        record.setOperatorName("系统");
        record.setStatus("异常");
        scanRecordRepository.save(record);
    }

    private String getOperatorName(Long operatorId) {
        if (operatorId == null) return "未知";
        return userRepository.findById(operatorId).map(User::getRealName).orElse("未知");
    }
}
