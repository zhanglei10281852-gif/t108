package com.company.material.repository;

import com.company.material.entity.InventoryLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InventoryLedgerRepository extends JpaRepository<InventoryLedger, Long> {
    Optional<InventoryLedger> findByMaterialIdAndWarehouseIdAndLocationId(Long materialId, Long warehouseId, Long locationId);

    List<InventoryLedger> findByMaterialId(Long materialId);
    List<InventoryLedger> findByLocationId(Long locationId);
    List<InventoryLedger> findByWarehouseId(Long warehouseId);
    List<InventoryLedger> findByMaterialIdAndLocationId(Long materialId, Long locationId);

    @Query("SELECT il FROM InventoryLedger il WHERE il.quantity > 0")
    List<InventoryLedger> findAllWithStock();

    @Query("SELECT il FROM InventoryLedger il WHERE il.materialId = :materialId AND il.quantity > 0")
    List<InventoryLedger> findStockByMaterialId(@Param("materialId") Long materialId);

    @Query("SELECT il FROM InventoryLedger il WHERE il.locationId = :locationId AND il.quantity > 0")
    List<InventoryLedger> findStockByLocationId(@Param("locationId") Long locationId);

    @Query("SELECT COUNT(DISTINCT il.locationId) FROM InventoryLedger il WHERE il.quantity > 0 AND il.warehouseId = :warehouseId")
    long countOccupiedLocations(@Param("warehouseId") Long warehouseId);

    @Query("SELECT COUNT(DISTINCT il.locationId) FROM InventoryLedger il WHERE il.quantity > 0")
    long countAllOccupiedLocations();
}
