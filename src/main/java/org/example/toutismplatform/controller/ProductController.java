package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.Product;
import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.repository.ProductRepository;
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
    
    // 获取所有商品
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return ResponseEntity.ok(products);
    }
    
    // 获取在售商品
    @GetMapping("/on-sale")
    public ResponseEntity<List<Product>> getOnSaleProducts() {
        List<Product> products = productRepository.findByStatus("ON_SALE");
        return ResponseEntity.ok(products);
    }
    
    // 根据 ID 获取商品详情（包含大景区和小景点）
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // 创建商品
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        // 计算商品价格
        calculateProductPrice(product);
        Product savedProduct = productRepository.save(product);
        return ResponseEntity.ok(savedProduct);
    }
    
    // 更新商品
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return productRepository.findById(id)
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
                        existingProduct.setLargeScenicAreas(product.getLargeScenicAreas());
                    }
                    // 计算商品价格
                    calculateProductPrice(existingProduct);
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
        if (product.getLargeScenicAreas() != null && !product.getLargeScenicAreas().isEmpty()) {
            List<LargeScenicArea> areas = product.getLargeScenicAreas();
            BigDecimal totalPrice = BigDecimal.ZERO;
            
            for (int i = 0; i < areas.size(); i++) {
                LargeScenicArea area = areas.get(i);
                if (area.getPrice() != null) {
                    if (i < 3) {
                        // 前3个景区按原价
                        totalPrice = totalPrice.add(area.getPrice());
                    } else {
                        // 之后的景区优惠5%
                        BigDecimal discountedPrice = area.getPrice().multiply(new BigDecimal(0.95));
                        totalPrice = totalPrice.add(discountedPrice);
                    }
                }
            }
            
            // 总价格优惠1%
            if (areas.size() > 3) {
                totalPrice = totalPrice.multiply(new BigDecimal(0.99));
            }
            
            // 价格取整数
            product.setPrice(totalPrice.setScale(0, BigDecimal.ROUND_HALF_UP));
        }
    }
}
