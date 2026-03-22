package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.Product;
import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.repository.ProductRepository;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private LargeScenicAreaRepository largeScenicAreaRepository;
    
    // 获取所有商品
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        // 使用 JOIN FETCH 确保加载关联的景区数据
        List<Product> products = productRepository.findAllWithScenicAreas();
        return ResponseEntity.ok(products);
    }
    
    // 获取在售商品
    @GetMapping("/on-sale")
    public ResponseEntity<List<Product>> getOnSaleProducts() {
        // 使用 JOIN FETCH 确保加载关联的景区数据
        List<Product> products = productRepository.findOnSaleWithScenicAreas("ON_SALE");
        return ResponseEntity.ok(products);
    }
    
    // 根据 ID 获取商品详情（包含大景区和小景点）
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        // 使用 JOIN FETCH 确保加载关联的景区数据
        return productRepository.findByIdWithScenicAreas(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // 创建商品
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        System.out.println("创建商品 - 传入的景区数量：" + 
            (product.getLargeScenicAreas() != null ? product.getLargeScenicAreas().size() : 0));
        
        if (product.getLargeScenicAreas() != null && !product.getLargeScenicAreas().isEmpty()) {
            // 从数据库获取完整的景区对象（包含最新价格）
            List<LargeScenicArea> managedAreas = new java.util.ArrayList<>();
            for (LargeScenicArea area : product.getLargeScenicAreas()) {
                if (area.getId() != null) {
                    largeScenicAreaRepository.findById(area.getId())
                        .ifPresent(managedAreas::add);
                }
            }
            product.setLargeScenicAreas(managedAreas);
            System.out.println("创建商品 - 加载后的景区数量：" + managedAreas.size());
        }
        
        // 计算商品价格（使用从数据库获取的最新价格）
        calculateProductPrice(product);
        System.out.println("创建商品 - 计算后的价格：" + product.getPrice());
        
        Product savedProduct = productRepository.save(product);
        return ResponseEntity.ok(savedProduct);
    }
    
    // 更新商品
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        System.out.println("更新商品 ID: " + id);
        System.out.println("传入的景区数量：" + 
            (product.getLargeScenicAreas() != null ? product.getLargeScenicAreas().size() : 0));
        
        return productRepository.findByIdWithScenicAreas(id)
                .map(existingProduct -> {
                    if (product.getName() != null) {
                        existingProduct.setName(product.getName());
                    }
                    if (product.getDescription() != null) {
                        existingProduct.setDescription(product.getDescription());
                    }
                    if (product.getImageUrl() != null) {
                        existingProduct.setImageUrl(product.getImageUrl());
                    }
                    if (product.getStatus() != null) {
                        existingProduct.setStatus(product.getStatus());
                    }
                    if (product.getLargeScenicAreas() != null) {
                        // 关键修复：从数据库重新获取景区信息，确保价格是最新的
                        List<LargeScenicArea> updatedAreas = new java.util.ArrayList<>();
                        for (LargeScenicArea area : product.getLargeScenicAreas()) {
                            if (area.getId() != null) {
                                // 根据 ID 从数据库获取最新的景区信息（包括最新价格）
                                largeScenicAreaRepository.findById(area.getId())
                                    .ifPresent(managedArea -> {
                                        updatedAreas.add(managedArea);
                                        System.out.println("加载景区：" + managedArea.getName() + ", 价格：" + managedArea.getPrice());
                                    });
                            }
                        }
                        existingProduct.setLargeScenicAreas(updatedAreas);
                        System.out.println("更新后的景区数量：" + updatedAreas.size());
                    }
                    // 计算商品价格（使用从数据库获取的最新价格）
                    calculateProductPrice(existingProduct);
                    System.out.println("计算后的套餐价格：" + existingProduct.getPrice());
                    return ResponseEntity.ok(productRepository.save(existingProduct));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // 删除商品
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    // 计算商品价格
    private void calculateProductPrice(Product product) {
        System.out.println("=== 开始计算价格 ===");
        
        if (product.getLargeScenicAreas() == null || product.getLargeScenicAreas().isEmpty()) {
            System.out.println("警告：没有关联的景区，价格设为 0");
            product.setPrice(java.math.BigDecimal.ZERO);
            return;
        }
        
        List<LargeScenicArea> areas = product.getLargeScenicAreas();
        BigDecimal totalPrice = BigDecimal.ZERO;
        
        System.out.println("景区数量：" + areas.size());
        
        for (int i = 0; i < areas.size(); i++) {
            LargeScenicArea area = areas.get(i);
            System.out.println("景区 " + (i+1) + ": " + area.getName() + 
                             ", 价格：" + area.getPrice() + 
                             ", 地点类型：" + area.getIsAreaType());
            
            if (area.getPrice() != null) {
                if (i < 3) {
                    // 前 3 个景区按原价
                    BigDecimal priceToAdd = area.getPrice();
                    totalPrice = totalPrice.add(priceToAdd);
                    System.out.println("  -> 前 3 个景区，添加价格：" + priceToAdd);
                } else {
                    // 之后的景区优惠 5%
                    BigDecimal discountedPrice = area.getPrice().multiply(new BigDecimal("0.95"));
                    totalPrice = totalPrice.add(discountedPrice);
                    System.out.println("  -> 第 " + (i+1) + " 个景区，95 折后价格：" + discountedPrice);
                }
            } else {
                System.out.println("  -> 景区价格为 null，跳过");
            }
        }
        
        // 总价格优惠 1%
        if (areas.size() > 3) {
            BigDecimal discount = totalPrice.multiply(new BigDecimal("0.01"));
            totalPrice = totalPrice.subtract(discount);
            System.out.println("超过 3 个景区，总价优惠 1%: -" + discount + ", 最终价格：" + totalPrice);
        }
        
        // 价格取整数（四舍五入）
        BigDecimal finalPrice = totalPrice.setScale(0, BigDecimal.ROUND_HALF_UP);
        System.out.println("最终价格（四舍五入）: " + finalPrice);
        System.out.println("=== 价格计算完成 ===");
        
        product.setPrice(finalPrice);
    }
}
