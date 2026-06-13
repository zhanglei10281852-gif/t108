package com.company.material.repository;

import com.company.material.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByLocationCode(String locationCode);
    boolean existsByLocationCode(String locationCode);
    List<Location> findByWarehouseId(Long warehouseId);
    List<Location> findByWarehouseIdAndEnabled(Long warehouseId, String enabled);
    List<Location> findByEnabled(String enabled);
    long countByWarehouseId(Long warehouseId);
    long countByWarehouseIdAndEnabled(Long warehouseId, String enabled);
}
