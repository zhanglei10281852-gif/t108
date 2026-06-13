package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "scan_records")
public class ScanRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String operationType;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "target_location_id")
    private Long targetLocationId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal quantity;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "operator_name", nullable = false, length = 50)
    private String operatorName;

    @Column(name = "document_no", length = 30)
    private String documentNo;

    @Column(nullable = false, length = 10)
    private String status;

    private LocalDateTime scanTime;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.scanTime == null) this.scanTime = LocalDateTime.now();
        if (this.status == null) this.status = "成功";
    }
}
