package com.company.material.config;

import com.company.material.entity.InventoryLedger;
import com.company.material.entity.Location;
import com.company.material.entity.Material;
import com.company.material.entity.Supplier;
import com.company.material.entity.User;
import com.company.material.entity.Warehouse;
import com.company.material.repository.InventoryLedgerRepository;
import com.company.material.repository.LocationRepository;
import com.company.material.repository.MaterialRepository;
import com.company.material.repository.SupplierRepository;
import com.company.material.repository.UserRepository;
import com.company.material.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final LocationRepository locationRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            createUser("admin", "admin123", "系统管理员", "信息中心", "管理员");
            createUser("wzhang", "123456", "张伟", "采购部", "采购员");
            createUser("limei", "123456", "李梅", "仓储部", "库管员");
            createUser("wangq", "123456", "王强", "生产部", "普通员工");
        }

        if (warehouseRepository.count() == 0) {
            createWarehouse("WH001", "原料一号库", "厂区东北角", "李梅", "13800001111");
            createWarehouse("WH002", "成品库", "厂区南门", "赵刚", "13800002222");
            createWarehouse("WH003", "备件库", "维修车间旁", "孙丽", "13800003333");
        }

        if (supplierRepository.count() == 0) {
            createSupplier("SUP001", "华东钢铁有限公司", "陈经理", "021-66668888", "上海市宝山区", "原材料");
            createSupplier("SUP002", "精密轴承制造厂", "刘主管", "0510-88889999", "江苏省无锡市", "机械备件");
            createSupplier("SUP003", "环球电气设备公司", "周工", "020-77776666", "广东省广州市", "电气设备");
        }

        if (materialRepository.count() == 0) {
            createMaterial("MAT0001", "热轧钢板", "原材料", "吨", "Q235B 10mm", new BigDecimal("4200.00"), 50);
            createMaterial("MAT0002", "深沟球轴承", "机械备件", "个", "6206-2RS", new BigDecimal("35.50"), 200);
            createMaterial("MAT0003", "三相异步电机", "电气设备", "台", "Y2-132M-4 7.5kW", new BigDecimal("1850.00"), 10);
            createMaterial("MAT0004", "液压油", "辅料", "桶", "L-HM46 200L", new BigDecimal("980.00"), 30);
            createMaterial("MAT0005", "劳保手套", "低值易耗", "副", "丁腈防滑", new BigDecimal("8.50"), 500);
        }

        if (locationRepository.count() == 0) {
            Warehouse wh001 = warehouseRepository.findByWarehouseCode("WH001").orElse(null);
            Warehouse wh002 = warehouseRepository.findByWarehouseCode("WH002").orElse(null);
            Warehouse wh003 = warehouseRepository.findByWarehouseCode("WH003").orElse(null);

            if (wh001 != null) {
                for (String zone : new String[]{"A", "B"}) {
                    for (int row = 1; row <= 2; row++) {
                        for (int shelf = 1; shelf <= 2; shelf++) {
                            for (int pos = 1; pos <= 3; pos++) {
                                createLocation(
                                    String.format("WH001-%s-%02d-%02d-%02d", zone, row, shelf, pos),
                                    wh001.getId(), zone,
                                    String.format("%02d", row),
                                    String.format("%02d", shelf),
                                    String.format("%02d", pos)
                                );
                            }
                        }
                    }
                }
            }

            if (wh002 != null) {
                for (String zone : new String[]{"A"}) {
                    for (int row = 1; row <= 2; row++) {
                        for (int shelf = 1; shelf <= 2; shelf++) {
                            for (int pos = 1; pos <= 2; pos++) {
                                createLocation(
                                    String.format("WH002-%s-%02d-%02d-%02d", zone, row, shelf, pos),
                                    wh002.getId(), zone,
                                    String.format("%02d", row),
                                    String.format("%02d", shelf),
                                    String.format("%02d", pos)
                                );
                            }
                        }
                    }
                }
            }

            if (wh003 != null) {
                for (String zone : new String[]{"A"}) {
                    for (int row = 1; row <= 1; row++) {
                        for (int shelf = 1; shelf <= 2; shelf++) {
                            for (int pos = 1; pos <= 2; pos++) {
                                createLocation(
                                    String.format("WH003-%s-%02d-%02d-%02d", zone, row, shelf, pos),
                                    wh003.getId(), zone,
                                    String.format("%02d", row),
                                    String.format("%02d", shelf),
                                    String.format("%02d", pos)
                                );
                            }
                        }
                    }
                }
            }
        }

        if (inventoryLedgerRepository.count() == 0) {
            Material mat1 = materialRepository.findByMaterialCode("MAT0001").orElse(null);
            Material mat2 = materialRepository.findByMaterialCode("MAT0002").orElse(null);
            Material mat3 = materialRepository.findByMaterialCode("MAT0003").orElse(null);
            Material mat4 = materialRepository.findByMaterialCode("MAT0004").orElse(null);
            Material mat5 = materialRepository.findByMaterialCode("MAT0005").orElse(null);

            Warehouse wh001 = warehouseRepository.findByWarehouseCode("WH001").orElse(null);
            Warehouse wh003 = warehouseRepository.findByWarehouseCode("WH003").orElse(null);

            Location loc1 = locationRepository.findByLocationCode("WH001-A-01-01-01").orElse(null);
            Location loc2 = locationRepository.findByLocationCode("WH001-A-01-01-02").orElse(null);
            Location loc3 = locationRepository.findByLocationCode("WH001-A-01-02-01").orElse(null);
            Location loc4 = locationRepository.findByLocationCode("WH001-B-01-01-01").orElse(null);
            Location loc5 = locationRepository.findByLocationCode("WH003-A-01-01-01").orElse(null);
            Location loc6 = locationRepository.findByLocationCode("WH003-A-01-01-02").orElse(null);

            if (mat1 != null && wh001 != null && loc1 != null) {
                createInventoryLedger(mat1.getId(), wh001.getId(), loc1.getId(), new BigDecimal("100"));
            }
            if (mat2 != null && wh001 != null && loc2 != null) {
                createInventoryLedger(mat2.getId(), wh001.getId(), loc2.getId(), new BigDecimal("500"));
            }
            if (mat4 != null && wh001 != null && loc3 != null) {
                createInventoryLedger(mat4.getId(), wh001.getId(), loc3.getId(), new BigDecimal("30"));
            }
            if (mat5 != null && wh001 != null && loc4 != null) {
                createInventoryLedger(mat5.getId(), wh001.getId(), loc4.getId(), new BigDecimal("800"));
            }
            if (mat3 != null && wh003 != null && loc5 != null) {
                createInventoryLedger(mat3.getId(), wh003.getId(), loc5.getId(), new BigDecimal("5"));
            }
            if (mat2 != null && wh003 != null && loc6 != null) {
                createInventoryLedger(mat2.getId(), wh003.getId(), loc6.getId(), new BigDecimal("50"));
            }
        }
    }

    private void createUser(String username, String password, String realName, String dept, String role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setRealName(realName);
        u.setDepartment(dept);
        u.setRole(role);
        userRepository.save(u);
    }

    private void createWarehouse(String code, String name, String location, String manager, String phone) {
        Warehouse w = new Warehouse();
        w.setWarehouseCode(code);
        w.setName(name);
        w.setLocation(location);
        w.setManager(manager);
        w.setPhone(phone);
        warehouseRepository.save(w);
    }

    private void createSupplier(String code, String name, String contact, String phone, String address, String category) {
        Supplier s = new Supplier();
        s.setSupplierCode(code);
        s.setName(name);
        s.setContactPerson(contact);
        s.setPhone(phone);
        s.setAddress(address);
        s.setCategory(category);
        supplierRepository.save(s);
    }

    private void createMaterial(String code, String name, String category, String unit, String spec, BigDecimal price, int safety) {
        Material m = new Material();
        m.setMaterialCode(code);
        m.setName(name);
        m.setCategory(category);
        m.setUnit(unit);
        m.setSpecification(spec);
        m.setReferencePrice(price);
        m.setSafetyStock(safety);
        materialRepository.save(m);
    }

    private void createLocation(String locationCode, Long warehouseId, String zone, String row, String shelf, String position) {
        Location loc = new Location();
        loc.setLocationCode(locationCode);
        loc.setWarehouseId(warehouseId);
        loc.setZone(zone);
        loc.setRow(row);
        loc.setShelf(shelf);
        loc.setPosition(position);
        loc.setEnabled("是");
        locationRepository.save(loc);
    }

    private void createInventoryLedger(Long materialId, Long warehouseId, Long locationId, BigDecimal quantity) {
        InventoryLedger ledger = new InventoryLedger();
        ledger.setMaterialId(materialId);
        ledger.setWarehouseId(warehouseId);
        ledger.setLocationId(locationId);
        ledger.setQuantity(quantity);
        inventoryLedgerRepository.save(ledger);
    }
}
