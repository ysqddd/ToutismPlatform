package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.CartItem;
import org.example.toutismplatform.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartItemController {
    @Autowired
    private CartItemRepository cartItemRepository;
    
    // 获取当前用户的购物车列表
    @GetMapping
    public ResponseEntity<List<CartItem>> getCartItems(@RequestParam Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        return ResponseEntity.ok(cartItems);
    }
    
    // 添加商品到购物车
    @PostMapping
    public ResponseEntity<CartItem> addToCart(@RequestBody CartItem cartItem) {
        CartItem savedItem = cartItemRepository.save(cartItem);
        return ResponseEntity.ok(savedItem);
    }
    
    // 从购物车移除商品
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromCart(@RequestParam Long userId, @PathVariable Long id) {
        cartItemRepository.deleteByUserIdAndId(userId, id);
        return ResponseEntity.ok().build();
    }
    
    // 清空购物车
    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestParam Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        cartItemRepository.deleteAll(cartItems);
        return ResponseEntity.ok().build();
    }
}
