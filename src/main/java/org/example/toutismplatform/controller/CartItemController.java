package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.CartItem;
import org.example.toutismplatform.entity.Product;
import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.repository.CartItemRepository;
import org.example.toutismplatform.repository.ProductRepository;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
public class CartItemController {
    @Autowired
    private CartItemRepository cartItemRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private LargeScenicAreaRepository largeScenicAreaRepository;
    
    @GetMapping
    public ResponseEntity<List<CartItem>> getCartItems(@RequestParam Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        return ResponseEntity.ok(cartItems);
    }
    
    @PostMapping("/product")
    public ResponseEntity<CartItem> addProductToCart(
            @RequestParam Long userId,
            @RequestParam Long productId
    ) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (!productOpt.isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        
        Product product = productOpt.get();
        
        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setItemType("PRODUCT");
        cartItem.setItemId(productId);
        cartItem.setItemName(product.getName());
        cartItem.setPrice(product.getPrice());
        cartItem.setImageUrl(product.getImageUrl());
        cartItem.setFeatures(product.getDescription());
        cartItem.setQuantity(1);
        
        CartItem savedItem = cartItemRepository.save(cartItem);
        return ResponseEntity.ok(savedItem);
    }
    
    @PostMapping("/scenic")
    public ResponseEntity<CartItem> addScenicAreaToCart(
            @RequestParam Long userId,
            @RequestParam Long scenicAreaId
    ) {
        Optional<LargeScenicArea> scenicOpt = largeScenicAreaRepository.findById(scenicAreaId);
        if (!scenicOpt.isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        
        LargeScenicArea scenic = scenicOpt.get();
        
        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setItemType("SCENIC_AREA");
        cartItem.setItemId(scenicAreaId);
        cartItem.setItemName(scenic.getName());
        cartItem.setPrice(scenic.getPrice());
        cartItem.setImageUrl(scenic.getImageUrl());
        cartItem.setFeatures(scenic.getDescription());
        cartItem.setQuantity(1);
        
        CartItem savedItem = cartItemRepository.save(cartItem);
        return ResponseEntity.ok(savedItem);
    }
    
    @PostMapping
    public ResponseEntity<CartItem> addToCart(@RequestBody CartItem cartItem) {
        CartItem savedItem = cartItemRepository.save(cartItem);
        return ResponseEntity.ok(savedItem);
    }
    
    @PutMapping("/{id}/quantity")
    public ResponseEntity<CartItem> updateQuantity(
            @PathVariable Long id,
            @RequestParam Integer quantity
    ) {
        Optional<CartItem> cartItemOpt = cartItemRepository.findById(id);
        if (!cartItemOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        CartItem cartItem = cartItemOpt.get();
        cartItem.setQuantity(quantity);
        CartItem savedItem = cartItemRepository.save(cartItem);
        return ResponseEntity.ok(savedItem);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromCart(@RequestParam Long userId, @PathVariable Long id) {
        Optional<CartItem> cartItemOpt = cartItemRepository.findById(id);
        if (!cartItemOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        CartItem cartItem = cartItemOpt.get();
        if (!cartItem.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        
        cartItemRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestParam Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        cartItemRepository.deleteAll(cartItems);
        return ResponseEntity.ok().build();
    }
}
