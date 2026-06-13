package com.company.material.repository;

import com.company.material.entity.StocktakingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StocktakingItemRepository extends JpaRepository<StocktakingItem, Long> {
    List<StocktakingItem> findByLocationId(Long locationId);
    List<StocktakingItem> findByBatchNo(String batchNo);
    List<StocktakingItem> findByStatus(String status);
    List<StocktakingItem> findByLocationIdAndBatchNo(Long locationId, String batchNo);
}
