package com.company.material.repository;

import com.company.material.entity.ScanRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ScanRecordRepository extends JpaRepository<ScanRecord, Long> {
    List<ScanRecord> findByOperationType(String operationType);
    List<ScanRecord> findByOperatorId(Long operatorId);
    List<ScanRecord> findByDocumentNo(String documentNo);
    List<ScanRecord> findByScanTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT sr FROM ScanRecord sr WHERE sr.scanTime >= :start AND sr.scanTime < :end")
    List<ScanRecord> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT sr.operationType, COUNT(sr) FROM ScanRecord sr WHERE sr.scanTime >= :start AND sr.scanTime < :end GROUP BY sr.operationType")
    List<Object[]> countByOperationTypeInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT sr.operationType, COUNT(sr) FROM ScanRecord sr GROUP BY sr.operationType")
    List<Object[]> countByOperationTypeAll();

    @Query("SELECT sr.operatorId, sr.operatorName, COUNT(sr) FROM ScanRecord sr WHERE sr.scanTime >= :start AND sr.scanTime < :end GROUP BY sr.operatorId, sr.operatorName")
    List<Object[]> countByOperatorInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(sr) FROM ScanRecord sr WHERE sr.status = '异常' AND sr.scanTime >= :start AND sr.scanTime < :end")
    long countAbnormalInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(sr) FROM ScanRecord sr WHERE sr.status = '异常'")
    long countAbnormalAll();

    @Query("SELECT sr.locationId, COUNT(sr) FROM ScanRecord sr WHERE sr.operationType IN ('入库', '出库') GROUP BY sr.locationId ORDER BY COUNT(sr) DESC")
    List<Object[]> countTurnoverByLocation();
}
