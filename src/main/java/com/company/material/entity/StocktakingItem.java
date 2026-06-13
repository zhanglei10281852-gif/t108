package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stocktaking_items")
public class StocktakingItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "system_quantity", nullable = false, precision = 14, scale = 2)
    private BigDecimal systemQuantity;

    @Column(name = "actual_quantity", precision = 14, scale = 2)
    private BigDecimal actualQuantity;

    @Column(precision = 14, scale = 2)
    private BigDecimal difference;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "operator_name", nullable = false, length = 50)
    private String operatorName;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "batch_no", length = 30)
    private String batchNo;

    private LocalDateTime scanTime;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.scanTime == null) this.scanTime = LocalDateTime.now();
        if (this.status == null) this.status = "待盘点";
    }
}
