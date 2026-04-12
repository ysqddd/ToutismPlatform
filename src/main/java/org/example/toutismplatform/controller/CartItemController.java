package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.CartItem;
import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.entity.Product;
import org.example.toutismplatform.entity.User;
import org.example.toutismplatform.repository.CartItemRepository;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.example.toutismplatform.repository.ProductRepository;
import org.example.toutismplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private UserRepository userRepository;

    /**
     * 兼容旧前端：/api/cart?userId=1
     */
    @GetMapping(params = "userId")
    public ResponseEntity<List<CartItemResponse>> getCartItemsByUserId(@RequestParam Long userId) {
        return ResponseEntity.ok(toResponseList(loadCartItems(userId)));
    }

    /**
     * 兼容前端：/api/cart/user/1
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CartItemResponse>> getCartItemsByPath(@PathVariable Long userId) {
        return ResponseEntity.ok(toResponseList(loadCartItems(userId)));
    }

    /**
     * 新版用户中心推荐使用：/api/cart/me
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyCart(Authentication authentication) {
        Optional<Long> currentUserId = resolveCurrentUserId(authentication);
        if (currentUserId.isEmpty()) {
            return unauthorized();
        }
        return ResponseEntity.ok(toResponseList(loadCartItems(currentUserId.get())));
    }

    /**
     * 给前端一个别名，兼容 /api/cart/my
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyCartAlias(Authentication authentication) {
        return getMyCart(authentication);
    }

    /**
     * 兼容旧逻辑：按 requestParam 加商品
     */
    @PostMapping("/product")
    public ResponseEntity<?> addProductToCart(@RequestParam(required = false) Long userId,
                                              @RequestParam Long productId,
                                              Authentication authentication) {
        Long targetUserId = resolveTargetUserId(userId, authentication);
        if (targetUserId == null) {
            return unauthorized();
        }

        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(buildMessage(false, "商品不存在"));
        }

        CartItem savedItem = saveOrReuseProductCartItem(targetUserId, productOpt.get());
        return ResponseEntity.ok(toResponse(savedItem));
    }

    /**
     * 兼容旧逻辑：按 requestParam 加景区
     */
    @PostMapping("/scenic")
    public ResponseEntity<?> addScenicAreaToCart(@RequestParam(required = false) Long userId,
                                                 @RequestParam Long scenicAreaId,
                                                 Authentication authentication) {
        Long targetUserId = resolveTargetUserId(userId, authentication);
        if (targetUserId == null) {
            return unauthorized();
        }

        Optional<LargeScenicArea> scenicOpt = largeScenicAreaRepository.findById(scenicAreaId);
        if (scenicOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(buildMessage(false, "景区不存在"));
        }

        CartItem savedItem = saveOrReuseScenicCartItem(targetUserId, scenicOpt.get());
        return ResponseEntity.ok(toResponse(savedItem));
    }

    /**
     * 通用新增接口，适合后续扩展。
     * body 示例：
     * {
     *   "userId": 1,
     *   "itemType": "PRODUCT",
     *   "itemId": 10,
     *   "quantity": 2
     * }
     */
    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody AddCartItemRequest request,
                                       Authentication authentication) {
        Long targetUserId = resolveTargetUserId(request.getUserId(), authentication);
        if (targetUserId == null) {
            return unauthorized();
        }
        if (request.getItemType() == null || request.getItemId() == null) {
            return ResponseEntity.badRequest().body(buildMessage(false, "缺少 itemType 或 itemId"));
        }

        String itemType = request.getItemType().trim().toUpperCase();
        Integer quantity = request.getQuantity() == null || request.getQuantity() <= 0 ? 1 : request.getQuantity();

        if ("PRODUCT".equals(itemType) || "PACKAGE".equals(itemType)) {
            Optional<Product> productOpt = productRepository.findById(request.getItemId());
            if (productOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(buildMessage(false, "商品不存在"));
            }
            CartItem savedItem = saveOrReuseProductCartItem(targetUserId, productOpt.get(), quantity);
            return ResponseEntity.ok(toResponse(savedItem));
        }

        if ("SCENIC_AREA".equals(itemType) || "SCENIC".equals(itemType)) {
            Optional<LargeScenicArea> scenicOpt = largeScenicAreaRepository.findById(request.getItemId());
            if (scenicOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(buildMessage(false, "景区不存在"));
            }
            CartItem savedItem = saveOrReuseScenicCartItem(targetUserId, scenicOpt.get(), quantity);
            return ResponseEntity.ok(toResponse(savedItem));
        }

        return ResponseEntity.badRequest().body(buildMessage(false, "itemType 仅支持 PRODUCT、PACKAGE 或 SCENIC_AREA"));
    }

    @PutMapping("/{id}/quantity")
    public ResponseEntity<?> updateQuantity(@PathVariable Long id,
                                            @RequestParam Integer quantity,
                                            @RequestParam(required = false) Long userId,
                                            Authentication authentication) {
        if (quantity == null || quantity <= 0) {
            return ResponseEntity.badRequest().body(buildMessage(false, "quantity 必须大于 0"));
        }

        Optional<CartItem> cartItemOpt = cartItemRepository.findById(id);
        if (cartItemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CartItem cartItem = cartItemOpt.get();
        if (!canOperateCartItem(cartItem, userId, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildMessage(false, "无权操作该购物车项"));
        }

        cartItem.setQuantity(quantity);
        CartItem saved = cartItemRepository.save(cartItem);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long id,
                                            @RequestParam(required = false) Long userId,
                                            Authentication authentication) {
        Optional<CartItem> cartItemOpt = cartItemRepository.findById(id);
        if (cartItemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CartItem cartItem = cartItemOpt.get();
        if (!canOperateCartItem(cartItem, userId, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildMessage(false, "无权删除该购物车项"));
        }

        cartItemRepository.deleteById(id);
        return ResponseEntity.ok(buildMessage(true, "删除成功"));
    }

    /**
     * 兼容旧风格：DELETE /api/cart?userId=1
     */
    @DeleteMapping(params = "userId")
    public ResponseEntity<?> clearCartByUserId(@RequestParam Long userId,
                                               Authentication authentication) {
        Long currentUserId = resolveTargetUserId(userId, authentication);
        if (currentUserId == null) {
            return unauthorized();
        }
        clearUserCart(currentUserId);
        return ResponseEntity.ok(buildMessage(true, "购物车已清空"));
    }

    /**
     * 新版推荐：DELETE /api/cart/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<?> clearMyCart(Authentication authentication) {
        Optional<Long> currentUserId = resolveCurrentUserId(authentication);
        if (currentUserId.isEmpty()) {
            return unauthorized();
        }
        clearUserCart(currentUserId.get());
        return ResponseEntity.ok(buildMessage(true, "购物车已清空"));
    }

    @DeleteMapping("/my")
    public ResponseEntity<?> clearMyCartAlias(Authentication authentication) {
        return clearMyCart(authentication);
    }

    private List<CartItem> loadCartItems(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        return cartItems == null ? Collections.emptyList() : cartItems;
    }

    private void clearUserCart(Long userId) {
        List<CartItem> cartItems = loadCartItems(userId);
        cartItemRepository.deleteAll(cartItems);
    }

    private boolean canOperateCartItem(CartItem cartItem, Long requestUserId, Authentication authentication) {
        if (cartItem == null || cartItem.getUserId() == null) {
            return false;
        }
        if (requestUserId != null) {
            return requestUserId.equals(cartItem.getUserId());
        }
        Optional<Long> currentUserId = resolveCurrentUserId(authentication);
        return currentUserId.isPresent() && currentUserId.get().equals(cartItem.getUserId());
    }

    private Long resolveTargetUserId(Long requestUserId, Authentication authentication) {
        if (requestUserId != null) {
            return requestUserId;
        }
        return resolveCurrentUserId(authentication).orElse(null);
    }

    private Optional<Long> resolveCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principalObject = authentication.getPrincipal();
        if (principalObject != null && !Objects.equals(principalObject, "anonymousUser")) {
            try {
                Object idValue = principalObject.getClass().getMethod("getId").invoke(principalObject);
                if (idValue instanceof Number) {
                    return Optional.of(((Number) idValue).longValue());
                }
            } catch (Exception ignored) {
            }
        }

        String principal = authentication.getName();
        if (principal == null || principal.trim().isEmpty() || Objects.equals(principal, "anonymousUser")) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(principal));
        } catch (NumberFormatException ignored) {
        }

        List<User> allUsers = userRepository.findAll();
        Optional<User> byUsername = allUsers.stream()
                .filter(user -> principal.equals(user.getUsername()))
                .findFirst();
        return byUsername.map(User::getId);
    }

    private CartItem saveOrReuseProductCartItem(Long userId, Product product) {
        return saveOrReuseProductCartItem(userId, product, 1);
    }

    private CartItem saveOrReuseProductCartItem(Long userId, Product product, Integer quantity) {
        CartItem existing = findExistingCartItem(userId, "PRODUCT", product.getId());
        if (existing != null) {
            int mergedQuantity = safeInt(existing.getQuantity()) + safeInt(quantity);
            existing.setQuantity(mergedQuantity <= 0 ? 1 : mergedQuantity);
            return cartItemRepository.save(existing);
        }

        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setItemType("PRODUCT");
        cartItem.setItemId(product.getId());
        cartItem.setItemName(product.getName());
        cartItem.setPrice(product.getPrice());
        cartItem.setImageUrl(product.getImageUrl());
        cartItem.setFeatures(product.getDescription());
        cartItem.setQuantity(safeInt(quantity) <= 0 ? 1 : quantity);
        return cartItemRepository.save(cartItem);
    }

    private CartItem saveOrReuseScenicCartItem(Long userId, LargeScenicArea scenic) {
        return saveOrReuseScenicCartItem(userId, scenic, 1);
    }

    private CartItem saveOrReuseScenicCartItem(Long userId, LargeScenicArea scenic, Integer quantity) {
        CartItem existing = findExistingCartItem(userId, "SCENIC_AREA", scenic.getId());
        if (existing != null) {
            int mergedQuantity = safeInt(existing.getQuantity()) + safeInt(quantity);
            existing.setQuantity(mergedQuantity <= 0 ? 1 : mergedQuantity);
            return cartItemRepository.save(existing);
        }

        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setItemType("SCENIC_AREA");
        cartItem.setItemId(scenic.getId());
        cartItem.setItemName(scenic.getName());
        cartItem.setPrice(scenic.getPrice());
        cartItem.setImageUrl(scenic.getImageUrl());
        cartItem.setFeatures(scenic.getDescription());
        cartItem.setQuantity(safeInt(quantity) <= 0 ? 1 : quantity);
        return cartItemRepository.save(cartItem);
    }

    private CartItem findExistingCartItem(Long userId, String itemType, Long itemId) {
        for (CartItem cartItem : loadCartItems(userId)) {
            if (cartItem != null
                    && userId.equals(cartItem.getUserId())
                    && itemId.equals(cartItem.getItemId())
                    && itemType.equalsIgnoreCase(String.valueOf(cartItem.getItemType()))) {
                return cartItem;
            }
        }
        return null;
    }

    private List<CartItemResponse> toResponseList(List<CartItem> items) {
        return items.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private CartItemResponse toResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setUserId(item.getUserId());
        String itemType = item.getItemType() == null ? null : String.valueOf(item.getItemType());
        response.setItemType(itemType);
        response.setItemId(item.getItemId());
        response.setName(item.getItemName());
        response.setPrice(safeDecimal(item.getPrice()));
        response.setImage(normalizeImagePath(item.getImageUrl()));
        response.setFeatures(item.getFeatures());
        response.setQuantity(safeInt(item.getQuantity()) <= 0 ? 1 : item.getQuantity());

        if ((response.getImage() == null || response.getImage().trim().isEmpty()) && item.getItemId() != null) {
            String upperType = itemType == null ? "" : itemType.trim().toUpperCase();
            if ("PRODUCT".equals(upperType) || "PACKAGE".equals(upperType)) {
                productRepository.findById(item.getItemId()).ifPresent(product -> {
                    if (response.getName() == null || response.getName().trim().isEmpty()) {
                        response.setName(product.getName());
                    }
                    response.setImage(normalizeImagePath(product.getImageUrl()));
                    if (response.getPrice() == null || BigDecimal.ZERO.compareTo(response.getPrice()) == 0) {
                        response.setPrice(safeDecimal(product.getPrice()));
                    }
                });
            } else if ("SCENIC_AREA".equals(upperType) || "SCENIC".equals(upperType)) {
                largeScenicAreaRepository.findById(item.getItemId()).ifPresent(scenic -> {
                    if (response.getName() == null || response.getName().trim().isEmpty()) {
                        response.setName(scenic.getName());
                    }
                    response.setImage(normalizeImagePath(scenic.getImageUrl()));
                    if (response.getPrice() == null || BigDecimal.ZERO.compareTo(response.getPrice()) == 0) {
                        response.setPrice(safeDecimal(scenic.getPrice()));
                    }
                });
            }
        }

        response.setSubtotal(response.getPrice().multiply(BigDecimal.valueOf(response.getQuantity())));
        return response;
    }

    private String normalizeImagePath(String path) {
        if (path == null) {
            return null;
        }
        String value = path.trim();
        if (value.isEmpty()) {
            return value;
        }
        value = value.replace('\\', '/');
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        if (value.startsWith("classpath:")) {
            value = value.substring("classpath:".length());
        }
        if (value.startsWith("src/main/")) {
            value = value.substring("src/main/".length());
        }
        if (value.startsWith("resources/")) {
            value = value.substring("resources/".length());
        }
        if (value.startsWith("static/")) {
            value = value.substring("static/".length());
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        return value;
    }

    private BigDecimal safeDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private ResponseEntity<ApiMessage> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiMessage(false, "未登录或无法识别当前用户"));
    }

    private ApiMessage buildMessage(boolean success, String message) {
        return new ApiMessage(success, message);
    }

    public static class AddCartItemRequest {
        private Long userId;
        private String itemType;
        private Long itemId;
        private Integer quantity;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public static class CartItemResponse {
        private Long id;
        private Long userId;
        private String itemType;
        private Long itemId;
        private String name;
        private BigDecimal price;
        private String image;
        private String features;
        private Integer quantity;
        private BigDecimal subtotal;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getFeatures() {
            return features;
        }

        public void setFeatures(String features) {
            this.features = features;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
        }
    }

    public static class ApiMessage {
        private boolean success;
        private String message;

        public ApiMessage() {
        }

        public ApiMessage(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
