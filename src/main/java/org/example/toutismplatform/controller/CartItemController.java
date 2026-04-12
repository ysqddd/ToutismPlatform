package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.CartItem;
import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.entity.Product;
import org.example.toutismplatform.repository.CartItemRepository;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.example.toutismplatform.repository.ProductRepository;
import org.example.toutismplatform.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private RagService ragService;

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
        CartItem savedItem = saveOrReuseProductCartItem(userId, productOpt.get());
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
        CartItem savedItem = saveOrReuseScenicCartItem(userId, scenicOpt.get());
        return ResponseEntity.ok(savedItem);
    }

    @PostMapping("/ai-confirm")
    public ResponseEntity<?> addAiRouteToCart(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        String query = body.get("query") == null ? null : String.valueOf(body.get("query"));
        String addType = normalizeAddType(body.get("addType"));
        if (addType.isEmpty()) {
            addType = "AUTO";
        }

        if (userId == null || query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(buildMessage("缺少 userId 或 query，无法根据 AI 方案加入购物车。"));
        }
        if (!"PACKAGE".equals(addType) && !"SCENIC".equals(addType) && !"AUTO".equals(addType)) {
            return ResponseEntity.badRequest().body(buildMessage("addType 仅支持 AUTO、PACKAGE 或 SCENIC。"));
        }

        Map<String, Object> routeContext = ragService.buildRouteCartContext(query);
        if (!Boolean.TRUE.equals(routeContext.get("success"))) {
            return ResponseEntity.badRequest().body(buildMessage(String.valueOf(routeContext.getOrDefault("answer", "暂时无法识别该方案。"))));
        }

        List<CartItem> processedItems = new ArrayList<>();
        List<Long> scenicAreaIds = extractLongList(routeContext.get("scenicAreaIds"));
        @SuppressWarnings("unchecked")
        Map<String, Object> matchedProduct = routeContext.get("matchedProduct") instanceof Map
                ? (Map<String, Object>) routeContext.get("matchedProduct")
                : new LinkedHashMap<>();

        if ("PACKAGE".equals(addType) || ("AUTO".equals(addType) && !matchedProduct.isEmpty())) {
            Long productId = toLong(matchedProduct.get("productId"));
            if (productId == null) {
                if ("PACKAGE".equals(addType)) {
                    return ResponseEntity.badRequest().body(buildMessage("当前方案在系统中没有完全匹配的现成套餐。"));
                }
            } else {
                Optional<Product> productOpt = productRepository.findById(productId);
                if (productOpt.isPresent()) {
                    processedItems.add(saveOrReuseProductCartItem(userId, productOpt.get()));
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("success", true);
                    response.put("message", "系统中已有与你方案完全匹配的套餐，已直接加入购物车。");
                    response.put("addType", "PACKAGE");
                    response.put("query", query);
                    response.put("processedCount", processedItems.size());
                    response.put("items", processedItems);
                    response.put("routeContext", routeContext);
                    return ResponseEntity.ok(response);
                }
            }
        }

        if ("PACKAGE".equals(addType)) {
            return ResponseEntity.badRequest().body(buildMessage("当前方案在系统中没有完全匹配的现成套餐。"));
        }

        for (Long scenicAreaId : scenicAreaIds) {
            Optional<LargeScenicArea> scenicOpt = largeScenicAreaRepository.findById(scenicAreaId);
            if (scenicOpt.isPresent()) {
                processedItems.add(saveOrReuseScenicCartItem(userId, scenicOpt.get()));
            }
        }

        if (processedItems.isEmpty()) {
            return ResponseEntity.badRequest().body(buildMessage("当前方案没有可加入购物车的内容。"));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "系统中没有完全匹配的现成套餐，已将方案中的景区加入购物车。");
        response.put("addType", "SCENIC");
        response.put("query", query);
        response.put("processedCount", processedItems.size());
        response.put("items", processedItems);
        response.put("routeContext", routeContext);
        return ResponseEntity.ok(response);
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

    private CartItem saveOrReuseProductCartItem(Long userId, Product product) {
        CartItem existing = findExistingCartItem(userId, "PRODUCT", product.getId());
        if (existing != null) {
            return existing;
        }
        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setItemType("PRODUCT");
        cartItem.setItemId(product.getId());
        cartItem.setItemName(product.getName());
        cartItem.setPrice(product.getPrice());
        cartItem.setImageUrl(product.getImageUrl());
        cartItem.setFeatures(product.getDescription());
        cartItem.setQuantity(1);
        return cartItemRepository.save(cartItem);
    }

    private CartItem saveOrReuseScenicCartItem(Long userId, LargeScenicArea scenic) {
        CartItem existing = findExistingCartItem(userId, "SCENIC_AREA", scenic.getId());
        if (existing != null) {
            return existing;
        }
        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setItemType("SCENIC_AREA");
        cartItem.setItemId(scenic.getId());
        cartItem.setItemName(scenic.getName());
        cartItem.setPrice(scenic.getPrice());
        cartItem.setImageUrl(scenic.getImageUrl());
        cartItem.setFeatures(scenic.getDescription());
        cartItem.setQuantity(1);
        return cartItemRepository.save(cartItem);
    }

    private CartItem findExistingCartItem(Long userId, String itemType, Long itemId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        for (CartItem cartItem : cartItems) {
            if (cartItem != null
                    && userId.equals(cartItem.getUserId())
                    && itemType.equalsIgnoreCase(String.valueOf(cartItem.getItemType()))
                    && itemId.equals(cartItem.getItemId())) {
                return cartItem;
            }
        }
        return null;
    }

    private List<Long> extractLongList(Object value) {
        List<Long> result = new ArrayList<>();
        if (!(value instanceof List)) {
            return result;
        }
        for (Object item : (List<?>) value) {
            Long parsed = toLong(item);
            if (parsed != null) {
                result.add(parsed);
            }
        }
        return result;
    }

    private String normalizeAddType(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim().toUpperCase();
    }

    private Map<String, Object> buildMessage(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
