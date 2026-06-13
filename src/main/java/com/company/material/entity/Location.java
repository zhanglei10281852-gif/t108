package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "locations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"warehouse_id", "zone", "row_num", "shelf", "position"})
})
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_code", nullable = false, unique = true, length = 30)
    private String locationCode;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(nullable = false, length = 10)
    private String zone;

    @Column(name = "row_num", nullable = false, length = 10)
    private String row;

    @Column(nullable = false, length = 10)
    private String shelf;

    @Column(nullable = false, length = 10)
    private String position;

    @Column(nullable = false, length = 10)
    private String enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.enabled == null) this.enabled = "是";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
