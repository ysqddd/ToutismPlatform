package org.example.toutismplatform.service;

import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CartPlanService {

    @Autowired
    private LargeScenicAreaRepository largeScenicAreaRepository;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    public CartPlanService() {
    }

    CartPlanService(LargeScenicAreaRepository largeScenicAreaRepository, JdbcTemplate jdbcTemplate) {
        this.largeScenicAreaRepository = largeScenicAreaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> buildCheapestCartPlan(List<Long> scenicAreaIds, Long userId) {
        return buildCheapestCartPlan(scenicAreaIds, userId, null);
    }

    public Map<String, Object> buildCheapestCartPlan(List<Long> scenicAreaIds,
                                                     Long userId,
                                                     List<LargeScenicArea> availableAreas) {
        Map<String, Object> result = new LinkedHashMap<>();
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        if (scenicAreaIds != null) {
            uniqueIds.addAll(scenicAreaIds);
        }
        List<Long> orderedIds = new ArrayList<>(uniqueIds);
        if (orderedIds.isEmpty()) {
            result.put("success", false);
            result.put("message", "没有可用于加入购物车的景区。");
            result.put("totalCost", 0.0);
            result.put("selectedProducts", Collections.emptyList());
            result.put("selectedScenicAreas", Collections.emptyList());
            result.put("alreadyCoveredScenicAreaIds", Collections.emptyList());
            result.put("coveredScenicAreaIds", Collections.emptyList());
            result.put("combinationDescription", "");
            return result;
        }

        Map<Long, LargeScenicArea> scenicMap = new LinkedHashMap<>();
        List<LargeScenicArea> areasForLookup = availableAreas == null ? loadAllAreasSafely() : availableAreas;
        for (LargeScenicArea area : areasForLookup) {
            if (area != null && area.getId() != null) {
                scenicMap.put(area.getId(), area);
            }
        }

        int scenicCount = orderedIds.size();
        if (scenicCount > 20) {
            orderedIds = orderedIds.subList(0, 20);
            scenicCount = orderedIds.size();
        }
        int fullMask = (1 << scenicCount) - 1;

        Map<String, Object> cartCoverage = loadCartCoverageInfo(userId, orderedIds);
        int initialMask = safeParseInt(String.valueOf(cartCoverage.getOrDefault("coveredMask", 0)), 0);

        List<CartCandidateOption> candidates = new ArrayList<>();
        candidates.addAll(loadProductCandidates(orderedIds));
        for (int i = 0; i < orderedIds.size(); i++) {
            Long scenicAreaId = orderedIds.get(i);
            LargeScenicArea scenic = scenicMap.get(scenicAreaId);
            if (scenic == null) {
                continue;
            }
            candidates.add(CartCandidateOption.scenic(
                    scenicAreaId,
                    scenic.getName(),
                    safeDecimal(scenic.getPrice()),
                    scenic.getImageUrl(),
                    scenic.getDescription(),
                    1 << i
            ));
        }

        double[] dp = new double[1 << scenicCount];
        int[] prevMask = new int[1 << scenicCount];
        int[] prevOptionIndex = new int[1 << scenicCount];
        Arrays.fill(dp, Double.POSITIVE_INFINITY);
        Arrays.fill(prevMask, -1);
        Arrays.fill(prevOptionIndex, -1);
        dp[initialMask] = 0.0;

        for (int mask = 0; mask <= fullMask; mask++) {
            if (Double.isInfinite(dp[mask])) {
                continue;
            }
            for (int i = 0; i < candidates.size(); i++) {
                CartCandidateOption option = candidates.get(i);
                int nextMask = mask | option.coverMask;
                if (nextMask == mask) {
                    continue;
                }
                double nextCost = dp[mask] + option.price;
                if (nextCost + 1e-9 < dp[nextMask]) {
                    dp[nextMask] = nextCost;
                    prevMask[nextMask] = mask;
                    prevOptionIndex[nextMask] = i;
                }
            }
        }

        if (Double.isInfinite(dp[fullMask])) {
            result.put("success", false);
            result.put("message", "当前无法计算出完整覆盖所选景区的最省钱购物车方案。");
            result.put("totalCost", 0.0);
            result.put("selectedProducts", Collections.emptyList());
            result.put("selectedScenicAreas", Collections.emptyList());
            result.put("alreadyCoveredScenicAreaIds", cartCoverage.getOrDefault("alreadyCoveredScenicAreaIds", Collections.emptyList()));
            result.put("coveredScenicAreaIds", orderedIds);
            result.put("combinationDescription", "");
            return result;
        }

        List<CartCandidateOption> chosenOptions = new ArrayList<>();
        int mask = fullMask;
        while (mask != initialMask && mask >= 0 && prevOptionIndex[mask] >= 0) {
            CartCandidateOption option = candidates.get(prevOptionIndex[mask]);
            chosenOptions.add(option);
            mask = prevMask[mask];
        }
        Collections.reverse(chosenOptions);

        List<Map<String, Object>> selectedProducts = new ArrayList<>();
        List<Map<String, Object>> selectedScenicAreas = new ArrayList<>();
        for (CartCandidateOption option : chosenOptions) {
            if (option.product) {
                selectedProducts.add(option.toMap());
            } else {
                selectedScenicAreas.add(option.toMap());
            }
        }

        result.put("success", true);
        result.put("totalCost", dp[fullMask]);
        result.put("selectedProducts", selectedProducts);
        result.put("selectedScenicAreas", selectedScenicAreas);
        result.put("alreadyCoveredScenicAreaIds", cartCoverage.getOrDefault("alreadyCoveredScenicAreaIds", Collections.emptyList()));
        result.put("coveredScenicAreaIds", orderedIds);
        result.put("combinationDescription", buildCartPlanDescription(selectedProducts, selectedScenicAreas));
        result.put("allCoveredByCart", initialMask == fullMask);
        result.put("hasExactPackage", selectedProducts.size() == 1 && selectedScenicAreas.isEmpty()
                && safeParseInt(String.valueOf(selectedProducts.get(0).getOrDefault("coverMask", 0)), 0) == fullMask);
        return result;
    }

    public String appendCartPrompt(String answer, List<Long> scenicAreaIds, Map<String, Object> cartPlan) {
        StringBuilder builder = new StringBuilder(answer == null ? "" : answer.trim());
        if (cartPlan == null || !Boolean.TRUE.equals(cartPlan.get("success")) || scenicAreaIds == null || scenicAreaIds.isEmpty()) {
            return builder.toString();
        }
        builder.append("\n\n按当前套餐与景区价格计算，更省钱的加入方式是：")
                .append(defaultText(String.valueOf(cartPlan.getOrDefault("combinationDescription", ""))));
        builder.append("，预计新增花费 ")
                .append(String.format(Locale.ROOT, "%.2f", getNumber(cartPlan.get("totalCost"))))
                .append(" 元。");
        builder.append("这套更省钱的组合可以先作为参考；你要是之后想继续加入购物车，直接回复“将你方案放入购物车”或“加入购物车”就行，我再帮你接着处理。");
        return builder.toString();
    }

    public Map<String, Object> addPendingPlanToCart(Long userId, Map<String, Object> pendingContext) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        if (userId == null) {
            result.put("message", "当前未识别到有效用户，暂时无法加入购物车。");
            return result;
        }
        if (pendingContext == null || pendingContext.isEmpty()) {
            result.put("message", "当前没有可加入购物车的推荐方案。");
            return result;
        }

        List<Long> scenicAreaIds = extractLongList(pendingContext.get("scenicAreaIds"));
        Map<String, Object> cartPlan = buildCheapestCartPlan(scenicAreaIds, userId);
        if (!Boolean.TRUE.equals(cartPlan.get("success"))) {
            result.put("message", String.valueOf(cartPlan.getOrDefault("message", "暂时无法计算最省钱的加购方案。")));
            return result;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selectedProducts = cartPlan.get("selectedProducts") instanceof List
                ? (List<Map<String, Object>>) cartPlan.get("selectedProducts")
                : Collections.emptyList();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selectedScenicAreas = cartPlan.get("selectedScenicAreas") instanceof List
                ? (List<Map<String, Object>>) cartPlan.get("selectedScenicAreas")
                : Collections.emptyList();

        List<String> added = new ArrayList<>();
        List<String> alreadyInCart = new ArrayList<>();
        addCartItems(userId, "PRODUCT", selectedProducts, added, alreadyInCart);
        addCartItems(userId, "SCENIC_AREA", selectedScenicAreas, added, alreadyInCart);

        result.put("success", true);
        result.put("addedItems", added);
        result.put("alreadyInCartItems", alreadyInCart);
        if (added.isEmpty()) {
            result.put("message", "按最低花费计算，这次所需的套餐/景区原本就在购物车中，无需重复加入。");
        } else {
            result.put("message", "已按最低花费方案加入购物车："
                    + String.valueOf(cartPlan.getOrDefault("combinationDescription", ""))
                    + "，预计新增花费 "
                    + String.format(Locale.ROOT, "%.2f", getNumber(cartPlan.get("totalCost")))
                    + " 元。");
        }
        return result;
    }

    private void addCartItems(Long userId,
                              String itemType,
                              List<Map<String, Object>> items,
                              List<String> added,
                              List<String> alreadyInCart) {
        for (Map<String, Object> item : items) {
            Long itemId = toLong(item.get("id"));
            String itemName = String.valueOf(item.getOrDefault("name", "PRODUCT".equals(itemType) ? "套餐" : "景区"));
            if (itemId == null) {
                continue;
            }
            if (cartItemExists(userId, itemType, itemId)) {
                alreadyInCart.add(itemName);
                continue;
            }
            insertCartItem(userId, itemType, itemId, itemName, item.get("price"), item.get("imageUrl"), item.get("description"));
            added.add(itemName);
        }
    }

    private Map<String, Object> loadCartCoverageInfo(Long userId, List<Long> orderedIds) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("coveredMask", 0);
        result.put("alreadyCoveredScenicAreaIds", new ArrayList<Long>());
        if (jdbcTemplate == null || userId == null || orderedIds == null || orderedIds.isEmpty()) {
            return result;
        }

        LinkedHashSet<Long> coveredIds = new LinkedHashSet<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT item_type, item_id FROM shopping_cart WHERE user_id = ?",
                    userId
            );
            for (Map<String, Object> row : rows) {
                String itemType = String.valueOf(row.get("item_type"));
                Long itemId = toLong(row.get("item_id"));
                if (itemId == null) {
                    continue;
                }
                if ("SCENIC_AREA".equalsIgnoreCase(itemType)) {
                    coveredIds.add(itemId);
                } else if ("PRODUCT".equalsIgnoreCase(itemType)) {
                    coveredIds.addAll(loadProductScenicAreaIds(itemId));
                }
            }
        } catch (Exception ignored) {
            return result;
        }

        List<Long> alreadyCovered = new ArrayList<>();
        for (Long id : orderedIds) {
            if (coveredIds.contains(id)) {
                alreadyCovered.add(id);
            }
        }
        result.put("coveredMask", buildCoverageMask(orderedIds, coveredIds));
        result.put("alreadyCoveredScenicAreaIds", alreadyCovered);
        return result;
    }

    private List<CartCandidateOption> loadProductCandidates(List<Long> scenicAreaIds) {
        List<CartCandidateOption> options = new ArrayList<>();
        if (jdbcTemplate == null || scenicAreaIds == null || scenicAreaIds.isEmpty()) {
            return options;
        }
        try {
            String placeholders = String.join(",", Collections.nCopies(scenicAreaIds.size(), "?"));
            String sql = "SELECT p.id, p.name, p.price, p.image_url, p.description, pla.large_scenic_area_id " +
                    "FROM product p " +
                    "JOIN product_large_scenic_area pla ON p.id = pla.product_id " +
                    "WHERE p.status = 'ON_SALE' AND pla.large_scenic_area_id IN (" + placeholders + ") " +
                    "ORDER BY p.id ASC";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, scenicAreaIds.toArray());

            Map<Long, LinkedHashSet<Long>> productCoverage = new LinkedHashMap<>();
            Map<Long, Map<String, Object>> productMeta = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Long productId = toLong(row.get("id"));
                Long scenicAreaId = toLong(row.get("large_scenic_area_id"));
                if (productId == null || scenicAreaId == null) {
                    continue;
                }
                productCoverage.computeIfAbsent(productId, key -> new LinkedHashSet<>()).add(scenicAreaId);
                productMeta.putIfAbsent(productId, row);
            }

            for (Map.Entry<Long, LinkedHashSet<Long>> entry : productCoverage.entrySet()) {
                Long productId = entry.getKey();
                int coverMask = buildCoverageMask(scenicAreaIds, entry.getValue());
                if (coverMask == 0) {
                    continue;
                }
                Map<String, Object> row = productMeta.get(productId);
                options.add(CartCandidateOption.product(
                        productId,
                        String.valueOf(row.get("name")),
                        getNumber(row.get("price")),
                        row.get("image_url") == null ? null : String.valueOf(row.get("image_url")),
                        row.get("description") == null ? null : String.valueOf(row.get("description")),
                        coverMask,
                        new ArrayList<>(entry.getValue())
                ));
            }
        } catch (Exception ignored) {
            return options;
        }
        return options;
    }

    private List<Long> loadProductScenicAreaIds(Long productId) {
        List<Long> ids = new ArrayList<>();
        if (jdbcTemplate == null || productId == null) {
            return ids;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT large_scenic_area_id FROM product_large_scenic_area WHERE product_id = ? ORDER BY large_scenic_area_id ASC",
                    productId
            );
            for (Map<String, Object> row : rows) {
                Long id = toLong(row.get("large_scenic_area_id"));
                if (id != null) {
                    ids.add(id);
                }
            }
        } catch (Exception ignored) {
            return ids;
        }
        return ids;
    }

    private boolean cartItemExists(Long userId, String itemType, Long itemId) {
        if (jdbcTemplate == null || userId == null || itemId == null) {
            return false;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id FROM shopping_cart WHERE user_id = ? AND item_type = ? AND item_id = ? LIMIT 1",
                    userId, itemType, itemId
            );
            return !rows.isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void insertCartItem(Long userId,
                                String itemType,
                                Long itemId,
                                String itemName,
                                Object price,
                                Object imageUrl,
                                Object features) {
        if (jdbcTemplate == null || userId == null || itemId == null) {
            return;
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO shopping_cart (user_id, item_type, item_id, item_name, price, image_url, features, quantity) VALUES (?, ?, ?, ?, ?, ?, ?, 1)",
                    userId,
                    itemType,
                    itemId,
                    itemName,
                    safeBigDecimal(price),
                    imageUrl == null ? null : String.valueOf(imageUrl),
                    features == null ? null : String.valueOf(features)
            );
        } catch (Exception ignored) {
        }
    }

    private List<LargeScenicArea> loadAllAreasSafely() {
        try {
            return largeScenicAreaRepository == null ? Collections.emptyList() : largeScenicAreaRepository.findAll();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String buildCartPlanDescription(List<Map<String, Object>> selectedProducts,
                                            List<Map<String, Object>> selectedScenicAreas) {
        List<String> parts = new ArrayList<>();
        for (Map<String, Object> product : selectedProducts) {
            parts.add("套餐“" + product.get("name") + "”");
        }
        for (Map<String, Object> scenic : selectedScenicAreas) {
            parts.add("景区“" + scenic.get("name") + "”");
        }
        return parts.isEmpty() ? "当前所需内容原本就在购物车中" : String.join(" + ", parts);
    }

    private int buildCoverageMask(List<Long> orderedIds, Collection<Long> coveredIds) {
        int mask = 0;
        if (orderedIds == null || coveredIds == null || orderedIds.isEmpty() || coveredIds.isEmpty()) {
            return mask;
        }
        Set<Long> coveredSet = coveredIds instanceof Set ? (Set<Long>) coveredIds : new HashSet<>(coveredIds);
        for (int i = 0; i < orderedIds.size(); i++) {
            if (coveredSet.contains(orderedIds.get(i))) {
                mask |= (1 << i);
            }
        }
        return mask;
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

    private BigDecimal safeBigDecimal(Object value) {
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
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
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

    private double getNumber(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int safeParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String defaultText(String value) {
        return value == null || value.trim().isEmpty() ? "暂无信息" : value;
    }

    private double safeDecimal(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private static class CartCandidateOption {
        private final boolean product;
        private final Long id;
        private final String name;
        private final double price;
        private final String imageUrl;
        private final String description;
        private final int coverMask;
        private final List<Long> coveredScenicAreaIds;

        private CartCandidateOption(boolean product,
                                    Long id,
                                    String name,
                                    double price,
                                    String imageUrl,
                                    String description,
                                    int coverMask,
                                    List<Long> coveredScenicAreaIds) {
            this.product = product;
            this.id = id;
            this.name = name;
            this.price = price;
            this.imageUrl = imageUrl;
            this.description = description;
            this.coverMask = coverMask;
            this.coveredScenicAreaIds = coveredScenicAreaIds == null ? Collections.emptyList() : coveredScenicAreaIds;
        }

        private static CartCandidateOption product(Long id,
                                                   String name,
                                                   double price,
                                                   String imageUrl,
                                                   String description,
                                                   int coverMask,
                                                   List<Long> coveredScenicAreaIds) {
            return new CartCandidateOption(true, id, name, price, imageUrl, description, coverMask, coveredScenicAreaIds);
        }

        private static CartCandidateOption scenic(Long id,
                                                  String name,
                                                  double price,
                                                  String imageUrl,
                                                  String description,
                                                  int coverMask) {
            return new CartCandidateOption(false, id, name, price, imageUrl, description, coverMask, Collections.singletonList(id));
        }

        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("name", name);
            map.put("price", price);
            map.put("imageUrl", imageUrl);
            map.put("description", description);
            map.put("coverMask", coverMask);
            map.put("coveredScenicAreaIds", new ArrayList<>(coveredScenicAreaIds));
            map.put("type", product ? "PRODUCT" : "SCENIC_AREA");
            return map;
        }
    }
}
