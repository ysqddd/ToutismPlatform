package org.example.toutismplatform.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.entity.Product;
import org.example.toutismplatform.entity.SmallScenicSpot;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.example.toutismplatform.repository.ProductRepository;
import org.example.toutismplatform.repository.SmallScenicSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RagService {

    private static final String MODE_DISTANCE = "distance";
    private static final String MODE_DURATION = "duration";
    private static final String MODE_PERSONALIZED = "personalized";
    private static final int ALL_SCENIC_STOPS = Integer.MAX_VALUE;
    private static final String UNKNOWN_SCENIC_REPLY = "开封目前没有这个景点，或者当前系统还没有录入这个景点。";
    private static final String NEED_TOURISM_QUERY_REPLY = "请告诉我你想了解的开封景点、路线或游玩偏好，我可以帮你介绍景点或规划路线。";
    private static final Pattern SCENIC_NAME_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5A-Za-z]{2,20}(?:景区|景点|公园|寺|塔|桥|码头|门|楼|府|祠|台|城|站|中心|夜市|街|馆|园|林|树林)");
    private static final Map<String, Map<String, Object>> PENDING_CART_CONTEXT_BY_USER = new ConcurrentHashMap<>();
    private static final ThreadLocal<Long> EXPLICIT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> EXPLICIT_USERNAME = new ThreadLocal<>();

    @Autowired
    private ChatLanguageModel chatModel;

    @Autowired
    private LargeScenicAreaRepository largeScenicAreaRepository;

    @Autowired
    private SmallScenicSpotRepository smallScenicSpotRepository;

    @Autowired
    private PathService pathService;

    @Autowired(required = false)
    private ProductRepository productRepository;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;


    public String generateAnswer(String query, Long userId, String username) {
        EXPLICIT_USER_ID.set(userId);
        EXPLICIT_USERNAME.set(username);
        try {
            return generateAnswer(query);
        } finally {
            EXPLICIT_USER_ID.remove();
            EXPLICIT_USERNAME.remove();
        }
    }

    public String generateAnswer(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "请先告诉我你的问题，我可以为你介绍景点或规划路线。";
        }

        String currentUserKey = buildConversationUserKey(EXPLICIT_USER_ID.get(), EXPLICIT_USERNAME.get());

        if (isCartConfirmationQuery(query, currentUserKey)) {
            return sanitizeAiAnswer(handleCartConfirmationQuery(currentUserKey));
        }

        List<LargeScenicArea> largeAreas = largeScenicAreaRepository.findAll();
        List<SmallScenicSpot> smallSpots = smallScenicSpotRepository.findAll();
        LinkedHashSet<String> allowedNames = buildAllowedScenicNameWhitelist(largeAreas, smallSpots);
        TourismIntentClassification modelIntent = classifyTourismIntent(query, allowedNames);

        if (isUnrecordedSpecificScenicQuery(query, allowedNames)) {
            return UNKNOWN_SCENIC_REPLY;
        }

        boolean packageRecommendationQuery = isPackageRecommendationQuery(query);
        boolean pathPlanningQuery = isPathPlanningQuery(query);
        boolean foodRecommendationQuery = isFoodRecommendationQuery(query);
        boolean generalScenicListQuery = isGeneralScenicListQuery(query);

        if (packageRecommendationQuery || modelIntent.is(TourismIntentType.PACKAGE_RECOMMENDATION)) {
            return polishFactAnswer(query, buildPackageRecommendationFacts(), allowedNames);
        }

        if (pathPlanningQuery || modelIntent.is(TourismIntentType.ROUTE_PLAN)) {
            Map<String, Object> routeCartContext = buildRouteCartContext(query, modelIntent);
            rememberPendingCartContext(currentUserKey, routeCartContext);
            return sanitizeAiAnswer(String.valueOf(routeCartContext.getOrDefault("answer", "暂时无法生成路线。")));
        }

        if (foodRecommendationQuery || modelIntent.is(TourismIntentType.FOOD_RECOMMENDATION)) {
            return polishFactAnswer(query, buildFoodRecommendationFacts(largeAreas), allowedNames);
        }

        if (generalScenicListQuery || modelIntent.is(TourismIntentType.GENERAL_SCENIC_LIST)) {
            // 有明确偏好标签（如老人友好）的信息查询，直接返回景点介绍，不附加购物车方案
            Set<String> preferenceTags = detectRelevantTags(query);
            if (!preferenceTags.isEmpty()) {
                String factAnswer = buildGeneralScenicRecommendationFacts(largeAreas, query);
                return polishFactAnswer(query, factAnswer, allowedNames);
            }
            Map<String, Object> recommendationContext = buildGeneralRecommendationCartContext(query, largeAreas);
            rememberPendingCartContext(currentUserKey, recommendationContext);
            String factAnswer = String.valueOf(recommendationContext.getOrDefault("answer", buildGeneralScenicRecommendationFacts(largeAreas, query)));
            return polishFactAnswer(query, factAnswer, allowedNames);
        }

        if (isInsufficientOrUnsupportedTourismQuery(query, allowedNames)
                && !modelIntent.is(TourismIntentType.SCENIC_DETAIL)
                && !modelIntent.is(TourismIntentType.ROUTE_PLAN)) {
            return NEED_TOURISM_QUERY_REPLY;
        }
        String factAnswer = buildKnowledgeOnlyFallbackAnswer(query, largeAreas, smallSpots);
        return polishFactAnswer(query, factAnswer, allowedNames);
    }




    private String buildConversationUserKey(Long userId, String username) {
        if (userId != null) {
            return String.valueOf(userId);
        }
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        return null;
    }

    private TourismIntentClassification classifyTourismIntent(String query, Set<String> allowedNames) {
        if (chatModel == null || query == null || query.isBlank()) {
            return TourismIntentClassification.unknown();
        }
        try {
            String response = chatModel.generate(buildIntentClassificationPrompt(query, allowedNames));
            return parseTourismIntentClassification(response);
        } catch (Exception ignored) {
            return TourismIntentClassification.unknown();
        }
    }

    private String buildIntentClassificationPrompt(String query, Set<String> allowedNames) {
        return "你只负责识别旅游问答意图，不回答问题，不编造事实。\n"
                + "请从这些意图中选择一个：ROUTE_PLAN、SCENIC_DETAIL、PACKAGE_RECOMMENDATION、FOOD_RECOMMENDATION、GENERAL_SCENIC_LIST、UNSUPPORTED、UNKNOWN。\n"
                + "如果用户要路线、游玩方案、如何游玩、几个景区游览，选 ROUTE_PLAN。\n"
                + "如果用户问单个景区介绍、门票、开放时间、园内怎么玩，选 SCENIC_DETAIL 或 ROUTE_PLAN，涉及顺序时优先 ROUTE_PLAN。\n"
                + "如果用户问套餐、套票、组合票、省钱组合，选 PACKAGE_RECOMMENDATION。\n"
                + "如果用户问美食、饭店、小吃、吃什么，选 FOOD_RECOMMENDATION。\n"
                + "如果用户泛问有哪些景点、更多景区、推荐景点，选 GENERAL_SCENIC_LIST 或 ROUTE_PLAN，要求方案时选 ROUTE_PLAN。\n"
                + "只输出四行，不要解释：\n"
                + "intent=意图\n"
                + "scenicName=如果有明确且已知景区名就输出，否则留空\n"
                + "maxStops=如果用户要求景区数量就输出数字，否则输出0\n"
                + "reason=不超过15字\n\n"
                + "已知景区和点位名称：" + buildAllowedNameSummary(allowedNames) + "\n"
                + "用户问题：" + query;
    }

    private TourismIntentClassification parseTourismIntentClassification(String response) {
        if (response == null || response.isBlank()) {
            return TourismIntentClassification.unknown();
        }
        TourismIntentType intentType = parseIntentType(extractIntentField(response, "intent"));
        String scenicName = extractIntentField(response, "scenicName");
        int maxStops = safeParseInt(extractIntentField(response, "maxStops"), 0);
        return new TourismIntentClassification(intentType, scenicName, maxStops);
    }

    private String extractIntentField(String response, String fieldName) {
        Matcher matcher = Pattern.compile("(?im)^\\s*\"?" + Pattern.quote(fieldName) + "\"?\\s*[:=]\\s*\"?([^\"\\r\\n,}]+)").matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private TourismIntentType parseIntentType(String value) {
        if (value == null || value.isBlank()) {
            return TourismIntentType.UNKNOWN;
        }
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        for (TourismIntentType type : TourismIntentType.values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        if (containsAny(normalized, "路线", "方案", "游玩", "行程")) {
            return TourismIntentType.ROUTE_PLAN;
        }
        if (containsAny(normalized, "套餐", "套票", "组合")) {
            return TourismIntentType.PACKAGE_RECOMMENDATION;
        }
        if (containsAny(normalized, "美食", "餐饮", "吃")) {
            return TourismIntentType.FOOD_RECOMMENDATION;
        }
        if (containsAny(normalized, "景点列表", "推荐景点", "更多景区")) {
            return TourismIntentType.GENERAL_SCENIC_LIST;
        }
        if (containsAny(normalized, "详情", "介绍", "门票", "开放")) {
            return TourismIntentType.SCENIC_DETAIL;
        }
        if (containsAny(normalized, "不支持", "无关")) {
            return TourismIntentType.UNSUPPORTED;
        }
        return TourismIntentType.UNKNOWN;
    }

    private boolean isCartConfirmationQuery(String query, String currentUserKey) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return false;
        }
        boolean strongConfirm = containsAny(normalized,
                "认可", "同意", "接受", "喜欢这个方案", "喜欢这份方案", "就按这个", "按这个来",
                "加入购物车", "放入购物车", "加到购物车", "将你方案放入购物车", "将方案放入购物车", "确认加入", "帮我加入", "帮我放入");
        boolean shortConfirm = normalized.length() <= 6 && containsAny(normalized,
                "可以", "可以的", "好的", "好", "行", "行的", "没问题", "就这样", "就这个", "认可");
        if (!strongConfirm && !shortConfirm) {
            return false;
        }
        if (isPathPlanningQuery(query)) {
            return false;
        }
        if (shortConfirm) {
            if (currentUserKey == null || currentUserKey.isBlank()) {
                return false;
            }
            Map<String, Object> pendingContext = PENDING_CART_CONTEXT_BY_USER.get(currentUserKey);
            return pendingContext != null && !pendingContext.isEmpty();
        }
        return true;
    }

    private void rememberPendingCartContext(String currentUserKey, Map<String, Object> context) {
        if (currentUserKey == null || currentUserKey.isBlank()) {
            return;
        }
        if (context == null || !Boolean.TRUE.equals(context.get("canAddToCart"))) {
            PENDING_CART_CONTEXT_BY_USER.remove(currentUserKey);
            return;
        }
        PENDING_CART_CONTEXT_BY_USER.put(currentUserKey, new LinkedHashMap<>(context));
    }

    private String handleCartConfirmationQuery(String currentUserKey) {
        if (currentUserKey == null || currentUserKey.isBlank()) {
            return "谢谢你的认可，这份推荐我已经先帮你记下了。当前还不能直接替你完成加购，请在请求里带上 userId 或 username 后再确认一次。";
        }
        Map<String, Object> pendingContext = PENDING_CART_CONTEXT_BY_USER.remove(currentUserKey);
        if (pendingContext == null || pendingContext.isEmpty()) {
            return "谢谢你的认可。不过我暂时没有找到你刚刚确认的方案，请先让我为你推荐一条路线或一组景点。";
        }
        Long userId = resolveUserIdFromConversationUserKey(currentUserKey);
        if (userId == null) {
            return "谢谢你的认可，这份推荐我已经先帮你记下了。当前还不能直接替你完成加购，请在请求里补充 userId 或确保登录状态有效后再试。";
        }
        Map<String, Object> addResult = addPendingPlanToCart(userId, pendingContext);
        if (!Boolean.TRUE.equals(addResult.get("success"))) {
            return String.valueOf(addResult.getOrDefault("message", "谢谢你的认可，但暂时无法将该方案加入购物车。"));
        }
        PENDING_CART_CONTEXT_BY_USER.remove(currentUserKey);
        return String.valueOf(addResult.get("message"));
    }

    private Long resolveUserIdFromConversationUserKey(String currentUserKey) {
        Long explicitUserId = EXPLICIT_USER_ID.get();
        if (explicitUserId != null) {
            return explicitUserId;
        }
        if (currentUserKey == null || currentUserKey.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(currentUserKey);
        } catch (NumberFormatException ignored) {
        }
        if (jdbcTemplate == null) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id FROM users WHERE username = ? LIMIT 1",
                    currentUserKey
            );
            if (rows.isEmpty()) {
                return null;
            }
            return toLong(rows.get(0).get("id"));
        } catch (Exception ignored) {
            return null;
        }
    }


    private Map<String, Object> buildGeneralRecommendationCartContext(String query, List<LargeScenicArea> largeAreas) {
        Map<String, Object> context = new LinkedHashMap<>();
        String answer = buildGeneralScenicRecommendationFacts(largeAreas, query);
        List<LargeScenicArea> scenicAreas = new ArrayList<>();
        if (largeAreas != null) {
            for (LargeScenicArea area : largeAreas) {
                if (area != null && safeInt(area.getIsAreaType()) == 0) {
                    scenicAreas.add(area);
                }
            }
        }
        if (scenicAreas.isEmpty() && largeAreas != null) {
            scenicAreas.addAll(largeAreas);
        }
        Set<String> relevantTags = detectRelevantTags(query);
        if (relevantTags.isEmpty()) {
            scenicAreas.sort(Comparator
                    .comparingDouble((LargeScenicArea area) -> safeDecimal(area.getPopularityScore())).reversed()
                    .thenComparingInt(area -> safeInt(area.getRecommendedVisitDuration())).reversed());
        } else {
            scenicAreas.sort((a, b) -> {
                int tagCompare = Integer.compare(
                        countMatchingTags(b.getTags(), relevantTags),
                        countMatchingTags(a.getTags(), relevantTags)
                );
                if (tagCompare != 0) return tagCompare;
                return Double.compare(
                        safeDecimal(b.getPopularityScore()),
                        safeDecimal(a.getPopularityScore())
                );
            });
        }

        List<Long> scenicAreaIds = new ArrayList<>();
        for (int i = 0; i < Math.min(6, scenicAreas.size()); i++) {
            if (scenicAreas.get(i).getId() != null) {
                scenicAreaIds.add(scenicAreas.get(i).getId());
            }
        }

        Map<String, Object> cheapestPlan = buildCheapestCartPlan(scenicAreaIds, null);
        context.put("success", true);
        context.put("routeResult", Collections.emptyMap());
        context.put("scenicAreaIds", scenicAreaIds);
        context.put("cartPlan", cheapestPlan);
        context.put("canAddToCart", !scenicAreaIds.isEmpty());
        context.put("answer", appendCartPrompt(answer, scenicAreaIds, cheapestPlan));
        return context;
    }

    public Map<String, Object> buildRouteCartContext(String query) {
        return buildRouteCartContext(query, TourismIntentClassification.unknown());
    }

    private Map<String, Object> buildRouteCartContext(String query, TourismIntentClassification modelIntent) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("success", false);
        context.put("query", query);
        context.put("answer", "请先告诉我你的路线需求，我可以帮你规划并推荐加入购物车。");
        context.put("scenicAreaIds", Collections.emptyList());
        context.put("cartPlan", Collections.emptyMap());
        context.put("routeResult", Collections.emptyMap());
        context.put("canAddToCart", false);

        if (query == null || query.trim().isEmpty()) {
            return context;
        }

        List<LargeScenicArea> allAreas = largeScenicAreaRepository.findAll();
        if (allAreas.isEmpty()) {
            context.put("answer", "当前还没有可用于路线规划的景区数据。");
            return context;
        }

        Map<String, String> locations = extractLocations(query, allAreas);
        LargeScenicArea startArea = findAreaByName(locations.get("start"), allAreas);
        LargeScenicArea endArea = findAreaByName(locations.get("end"), allAreas);
        List<LargeScenicArea> mentionedAreas = extractMentionedAreas(query, allAreas);
        LargeScenicArea modelMentionedArea = findAreaByName(modelIntent.scenicName, allAreas);
        if (modelMentionedArea != null && mentionedAreas.stream().noneMatch(area -> Objects.equals(area.getId(), modelMentionedArea.getId()))) {
            mentionedAreas.add(modelMentionedArea);
        }

        if (startArea == null && !mentionedAreas.isEmpty() && normalize(query).contains("从")) {
            startArea = mentionedAreas.get(0);
        }
        if (endArea == null && mentionedAreas.size() >= 2) {
            endArea = mentionedAreas.get(mentionedAreas.size() - 1);
        }

        Map<String, Double> preferenceWeights = extractPreferenceWeights(query);
        String routeMode = extractRouteMode(query, preferenceWeights);
        int maxStops = resolveMaxStops(query, modelIntent);
        boolean multiStopRoute = isMultiStopRouteQuery(query, maxStops);

        Map<String, Object> routeResult;
        String answer;

        LargeScenicArea singleArea = resolveSingleAreaRouteTarget(startArea, endArea, mentionedAreas);
        if (singleArea != null && isSingleAreaTourIntent(query, startArea, endArea, mentionedAreas, singleArea)) {
            answer = buildSingleAreaTourAnswer(singleArea, query, preferenceWeights);
            routeResult = new LinkedHashMap<>();
            routeResult.put("success", true);
            routeResult.put("recommendedAreaIds", Collections.singletonList(singleArea.getId()));
            routeResult.put("recommendedScenicAreaIds", Collections.singletonList(singleArea.getId()));
            List<Map<String, Object>> pathDetails = new ArrayList<>();
            Map<String, Object> pathDetail = new LinkedHashMap<>();
            pathDetail.put("id", singleArea.getId());
            pathDetail.put("name", singleArea.getName());
            pathDetail.put("isAreaType", singleArea.getIsAreaType());
            pathDetail.put("location", singleArea.getLocation());
            pathDetail.put("recommendedVisitDuration", singleArea.getRecommendedVisitDuration());
            pathDetails.add(pathDetail);
            routeResult.put("pathDetails", pathDetails);
        } else if (startArea != null && endArea != null && !Objects.equals(startArea.getId(), endArea.getId()) && !multiStopRoute) {
            Map<String, Object> selectedPath = selectRouteResult(startArea.getId(), endArea.getId(), routeMode, preferenceWeights);
            if (!Boolean.TRUE.equals(selectedPath.get("success"))) {
                context.put("answer", String.valueOf(selectedPath.getOrDefault("message", "暂时无法规划该路线。")));
                return context;
            }
            routeResult = selectedPath;
            Map<String, Object> distancePath = pathService.calculateShortestPath(startArea.getId(), endArea.getId(), MODE_DISTANCE);
            Map<String, Object> timePath = pathService.calculateShortestPath(startArea.getId(), endArea.getId(), MODE_DURATION);
            answer = buildSingleRouteAnswer(startArea, endArea, preferenceWeights, routeMode, selectedPath, distancePath, timePath);
        } else {
            Long preferredStartId = startArea == null ? null : startArea.getId();
            Long preferredEndId = endArea == null ? null : endArea.getId();
            Map<String, Object> cityRoute = pathService.recommendCityRoute(preferredStartId, preferredEndId, preferenceWeights, routeMode, maxStops);
            if (!Boolean.TRUE.equals(cityRoute.get("success"))) {
                context.put("answer", String.valueOf(cityRoute.getOrDefault("message", "暂时无法生成城市内景区推荐路线。")));
                return context;
            }
            routeResult = cityRoute;
            answer = buildCityRouteAnswer(startArea, endArea, preferenceWeights, routeMode, cityRoute, maxStops);
        }

        List<Long> scenicAreaIds = extractRecommendedScenicAreaIds(routeResult);
        Map<String, Object> cheapestPlan = buildCheapestCartPlan(scenicAreaIds, null);

        context.put("success", true);
        context.put("routeResult", routeResult);
        context.put("scenicAreaIds", scenicAreaIds);
        context.put("cartPlan", cheapestPlan);
        context.put("canAddToCart", !scenicAreaIds.isEmpty());
        answer = sanitizeAiAnswer(answer);
        context.put("answer", appendCartPrompt(answer, scenicAreaIds, cheapestPlan));
        return context;
    }

    private List<Long> extractRecommendedScenicAreaIds(Map<String, Object> routeResult) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (routeResult == null || routeResult.isEmpty()) {
            return new ArrayList<>();
        }

        Object recommendedIds = routeResult.get("recommendedScenicAreaIds");
        if (recommendedIds instanceof List) {
            for (Object obj : (List<?>) recommendedIds) {
                Long id = toLong(obj);
                if (id != null) {
                    ids.add(id);
                }
            }
        }

        List<Map<String, Object>> pathDetails = getRecommendedPathDetails(routeResult);
        for (Map<String, Object> detail : pathDetails) {
            if (detail == null) {
                continue;
            }
            Long id = toLong(detail.get("id"));
            int isAreaType = safeParseInt(String.valueOf(detail.getOrDefault("isAreaType", 0)), 0);
            if (id != null && isAreaType == 0) {
                ids.add(id);
            }
        }

        List<Map<String, Object>> visitDetails = getVisitDetails(routeResult);
        for (Map<String, Object> detail : visitDetails) {
            if (detail == null) {
                continue;
            }
            Long areaId = toLong(detail.get("areaId"));
            if (areaId != null) {
                ids.add(areaId);
            }
        }

        return new ArrayList<>(ids);
    }

    private Map<String, Object> buildCheapestCartPlan(List<Long> scenicAreaIds, Long userId) {
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
        for (LargeScenicArea area : largeScenicAreaRepository.findAll()) {
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
            int coverMask = 1 << i;
            candidates.add(CartCandidateOption.scenic(
                    scenicAreaId,
                    scenic.getName(),
                    safeDecimal(scenic.getPrice()),
                    scenic.getImageUrl(),
                    scenic.getDescription(),
                    coverMask
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
        result.put("combinationDescription", buildCartPlanDescription(selectedProducts, selectedScenicAreas, dp[fullMask]));
        result.put("allCoveredByCart", initialMask == fullMask);
        result.put("hasExactPackage", selectedProducts.size() == 1 && selectedScenicAreas.isEmpty()
                && safeParseInt(String.valueOf(selectedProducts.get(0).getOrDefault("coverMask", 0)), 0) == fullMask);
        return result;
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

        int coveredMask = buildCoverageMask(orderedIds, coveredIds);
        List<Long> alreadyCovered = new ArrayList<>();
        for (Long id : orderedIds) {
            if (coveredIds.contains(id)) {
                alreadyCovered.add(id);
            }
        }
        result.put("coveredMask", coveredMask);
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

    private String appendCartPrompt(String answer, List<Long> scenicAreaIds, Map<String, Object> cartPlan) {
        StringBuilder builder = new StringBuilder(answer == null ? "" : answer.trim());
        if (cartPlan == null || !Boolean.TRUE.equals(cartPlan.get("success")) || scenicAreaIds == null || scenicAreaIds.isEmpty()) {
            return builder.toString();
        }
        builder.append("\n\n按当前套餐与景区价格计算，更省钱的加入方式是：")
                .append(defaultText(String.valueOf(cartPlan.getOrDefault("combinationDescription", ""))));
        Object totalCost = cartPlan.get("totalCost");
        builder.append("，预计新增花费 ")
                .append(String.format(Locale.ROOT, "%.2f", getNumber(totalCost)))
                .append(" 元。");
        builder.append("这套更省钱的组合可以先作为参考；你要是之后想继续加入购物车，直接回复“将你方案放入购物车”或“加入购物车”就行，我再帮你接着处理。");
        return builder.toString();
    }

    private String buildCartPlanDescription(List<Map<String, Object>> selectedProducts,
                                            List<Map<String, Object>> selectedScenicAreas,
                                            double totalCost) {
        List<String> parts = new ArrayList<>();
        for (Map<String, Object> product : selectedProducts) {
            parts.add("套餐“" + product.get("name") + "”");
        }
        for (Map<String, Object> scenic : selectedScenicAreas) {
            parts.add("景区“" + scenic.get("name") + "”");
        }
        if (parts.isEmpty()) {
            return "当前所需内容原本就在购物车中";
        }
        return String.join(" + ", parts);
    }

    private Map<String, Object> addPendingPlanToCart(Long userId, Map<String, Object> pendingContext) {
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

        for (Map<String, Object> product : selectedProducts) {
            Long productId = toLong(product.get("id"));
            String productName = String.valueOf(product.getOrDefault("name", "套餐"));
            if (productId == null) {
                continue;
            }
            if (cartItemExists(userId, "PRODUCT", productId)) {
                alreadyInCart.add(productName);
                continue;
            }
            insertCartItem(
                    userId,
                    "PRODUCT",
                    productId,
                    productName,
                    product.get("price"),
                    product.get("imageUrl"),
                    product.get("description")
            );
            added.add(productName);
        }

        for (Map<String, Object> scenic : selectedScenicAreas) {
            Long scenicAreaId = toLong(scenic.get("id"));
            String scenicName = String.valueOf(scenic.getOrDefault("name", "景区"));
            if (scenicAreaId == null) {
                continue;
            }
            if (cartItemExists(userId, "SCENIC_AREA", scenicAreaId)) {
                alreadyInCart.add(scenicName);
                continue;
            }
            insertCartItem(
                    userId,
                    "SCENIC_AREA",
                    scenicAreaId,
                    scenicName,
                    scenic.get("price"),
                    scenic.get("imageUrl"),
                    scenic.get("description")
            );
            added.add(scenicName);
        }

        result.put("success", true);
        result.put("addedItems", added);
        result.put("alreadyInCartItems", alreadyInCart);

        double totalCost = getNumber(cartPlan.get("totalCost"));
        String combinationDescription = String.valueOf(cartPlan.getOrDefault("combinationDescription", ""));
        if (added.isEmpty()) {
            result.put("message", "按最低花费计算，这次所需的套餐/景区原本就在购物车中，无需重复加入。");
        } else {
            result.put("message", "已按最低花费方案加入购物车："
                    + combinationDescription
                    + "，预计新增花费 "
                    + String.format(Locale.ROOT, "%.2f", totalCost)
                    + " 元。");
        }
        return result;
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

    private boolean isGeneralScenicListQuery(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return false;
        }
        if (containsAny(normalized, "路线", "路径", "怎么走", "怎么去", "从", "到", "规划", "行程", "顺序")) {
            return false;
        }
        return containsAny(normalized,
                "有哪些知名景点", "知名景点", "著名景点", "热门景点", "有哪些景点", "有什么景点",
                "有哪些好玩的地方", "好玩的地方", "值得去的地方", "推荐景点", "必去景点", "开封去哪玩",
                "去的地方", "适合老人", "适合带老人", "适合老年人", "适合亲子", "亲子游玩",
                "适合儿童", "适合孩子", "适合小孩", "适合家庭", "推荐去哪里", "去哪里玩", "去哪儿玩");
    }

    private String buildGeneralScenicRecommendationFacts(List<LargeScenicArea> largeAreas, String query) {
        List<LargeScenicArea> scenicAreas = new ArrayList<>();
        if (largeAreas != null) {
            for (LargeScenicArea area : largeAreas) {
                if (area != null && safeInt(area.getIsAreaType()) == 0) {
                    scenicAreas.add(area);
                }
            }
        }
        if (scenicAreas.isEmpty() && largeAreas != null) {
            scenicAreas = new ArrayList<>(largeAreas);
        }
        if (scenicAreas.isEmpty()) {
            return "目前还没有查到可用于介绍的景点信息。";
        }

        Set<String> relevantTags = detectRelevantTags(query);
        if (relevantTags.isEmpty()) {
            scenicAreas.sort(Comparator
                    .comparingDouble((LargeScenicArea area) -> safeDecimal(area.getPopularityScore())).reversed()
                    .thenComparingInt(area -> safeInt(area.getRecommendedVisitDuration())).reversed());
        } else {
            scenicAreas.sort((a, b) -> {
                int tagCompare = Integer.compare(
                        countMatchingTags(b.getTags(), relevantTags),
                        countMatchingTags(a.getTags(), relevantTags)
                );
                if (tagCompare != 0) return tagCompare;
                return Double.compare(
                        safeDecimal(b.getPopularityScore()),
                        safeDecimal(a.getPopularityScore())
                );
            });
            // 有明确标签偏好时，只推荐至少匹配一个标签的景点，避免推荐不相关景点
            List<LargeScenicArea> filtered = new ArrayList<>();
            for (LargeScenicArea area : scenicAreas) {
                if (countMatchingTags(area.getTags(), relevantTags) > 0) {
                    filtered.add(area);
                }
            }
            if (!filtered.isEmpty()) {
                scenicAreas = filtered;
            }
        }

        StringBuilder answer = new StringBuilder(buildGeneralRecommendationOpening(query));
        int limit = Math.min(6, scenicAreas.size());
        for (int i = 0; i < limit; i++) {
            LargeScenicArea area = scenicAreas.get(i);
            answer.append("\n\n").append(i + 1).append(". ").append(area.getName()).append("：");
            String description = trimToSentence(defaultText(area.getDescription()));
            if (!description.isBlank() && !"暂无信息".equals(description)) {
                answer.append(ensureSentenceEnding(description));
            }
            String suitability = buildSuitabilityReason(area, query);
            if (!suitability.isBlank()) {
                answer.append(suitability);
            }
        }
        return sanitizeAiAnswer(answer.toString());
    }

    private String buildGeneralRecommendationOpening(String query) {
        String normalized = normalize(query);
        if (containsAny(normalized, "老人", "适合老人", "老人家", "老年人", "长辈", "爸妈")) {
            return "开封市有很多适合带老人去的地方，以下是一些推荐：";
        }
        if (containsAny(normalized, "亲子", "小孩", "孩子", "儿童", "家庭", "带娃")) {
            return "开封市有很多适合亲子游玩的地方，以下是一些推荐：";
        }
        if (containsAny(normalized, "拍照", "摄影", "打卡", "拍摄")) {
            return "开封市有很多适合拍照打卡的地方，以下是一些推荐：";
        }
        if (containsAny(normalized, "历史", "古迹", "古建", "文物", "古塔", "北宋")) {
            return "开封市有很多适合历史文化游的地方，以下是一些推荐：";
        }
        if (containsAny(normalized, "寺庙", "佛教", "烧香", "拜佛")) {
            return "开封市有很多适合寺庙人文游的地方，以下是一些推荐：";
        }
        if (containsAny(normalized, "园林", "公园", "花园")) {
            return "开封市有很多适合园林休闲游的地方，以下是一些推荐：";
        }
        if (containsAny(normalized, "热闹", "表演", "演艺", "演出", "节目")) {
            return "开封市有很多适合看演艺和热闹游玩的地方，以下是一些推荐：";
        }
        if (containsAny(normalized, "文化", "书法", "碑林", "人文")) {
            return "开封市有很多适合人文文化游的地方，以下是一些推荐：";
        }
        if (containsAny(normalized, "散步", "休闲", "放松", "悠闲")) {
            return "开封市有很多适合休闲散步的地方，以下是一些推荐：";
        }
        return "开封有不少值得游览的景区，以下是一些推荐：";
    }

    private String buildSuitabilityReason(LargeScenicArea area, String query) {
        String combined = normalize(defaultText(area == null ? null : area.getTags()));
        String normalizedQuery = normalize(query);
        if (combined.isBlank() || "暂无信息".equals(combined)) {
            return "";
        }
        String featureText = buildTagFeatureText(combined);
        if (featureText.isBlank()) {
            return "";
        }
        if (containsAny(normalizedQuery, "老人", "适合老人", "老人家", "老年人", "长辈", "爸妈")) {
            if (containsAny(combined, "老人友好", "静态游览", "休闲", "散步", "园林", "寺庙", "人文")) {
                return "作为以" + featureText + "为特色的景区，这里更适合老年人慢节奏参观" + buildElderlyPreferenceTail(area, combined);
            }
        }
        if (containsAny(normalizedQuery, "亲子", "小孩", "孩子", "儿童", "家庭", "带娃")) {
            if (containsAny(combined, "亲子", "儿童", "家庭", "演艺", "休闲", "园林", "散步")) {
                return "作为以" + featureText + "为特色的景区，这里适合亲子一起游览" + buildFamilyPreferenceTail(combined);
            }
        }
        if (containsAny(normalizedQuery, "拍照", "摄影", "打卡", "拍摄") && containsAny(combined, "拍照", "园林", "古塔", "古迹")) {
            return "作为以" + featureText + "为特色的景区，这里画面辨识度比较高，适合拍照打卡和慢慢观赏。";
        }
        if (containsAny(normalizedQuery, "历史", "古迹", "古建", "文物", "古塔", "北宋") && containsAny(combined, "历史", "古迹", "古塔", "古建", "文物", "北宋")) {
            return "作为以" + featureText + "为特色的景区，这里更适合想看历史遗存和城市文化的游客。";
        }
        if (containsAny(normalizedQuery, "寺庙", "佛教", "烧香", "拜佛") && containsAny(combined, "寺庙", "佛教", "人文", "静态游览")) {
            return "作为以" + featureText + "为特色的景区，这里适合以寺庙参观和人文感受为主，游览节奏相对安静。";
        }
        if (containsAny(normalizedQuery, "园林", "公园", "花园") && containsAny(combined, "园林", "历史公园", "散步", "休闲")) {
            return "作为以" + featureText + "为特色的景区，这里适合慢走观景，安排成轻松的园林休闲游。";
        }
        if (containsAny(normalizedQuery, "热闹", "表演", "演艺", "演出", "节目") && containsAny(combined, "演艺", "表演", "演出")) {
            return "作为以" + featureText + "为特色的景区，这里更适合想看节目、感受热闹氛围的游客。";
        }
        if (containsAny(normalizedQuery, "文化", "书法", "碑林", "人文") && containsAny(combined, "人文", "书法", "碑林", "历史")) {
            return "作为以" + featureText + "为特色的景区，这里适合把参观重点放在文化内容和人文体验上。";
        }
        if (containsAny(normalizedQuery, "散步", "休闲", "放松", "悠闲") && containsAny(combined, "散步", "休闲", "园林")) {
            return "作为以" + featureText + "为特色的景区，这里适合放慢节奏散步停留，整体安排不需要太赶。";
        }
        return "";
    }

    private String buildElderlyPreferenceTail(LargeScenicArea area, String tags) {
        String facilityText = buildConvenienceText(area);
        String tail;
        if (containsAny(tags, "老人友好", "静态游览")) {
            tail = "，参观方式偏静态，体力压力相对较小";
        } else if (containsAny(tags, "园林", "散步", "休闲", "历史公园")) {
            tail = "，步行和停留节奏比较容易安排";
        } else if (containsAny(tags, "寺庙", "佛教", "人文")) {
            tail = "，可以以人文参观为主，节奏不需要太赶";
        } else {
            tail = "，整体节奏适合慢慢游览";
        }
        if (!facilityText.isBlank()) {
            tail += "，" + facilityText;
        }
        return tail + "。";
    }

    private String buildFamilyPreferenceTail(String tags) {
        if (containsAny(tags, "亲子", "儿童", "家庭")) {
            return "，内容和停留节奏更贴近带孩子出行的需求。";
        }
        if (containsAny(tags, "演艺", "表演", "演出")) {
            return "，演艺内容更容易吸引孩子停留。";
        }
        if (containsAny(tags, "园林", "散步", "休闲", "历史公园")) {
            return "，空间节奏比较舒缓，方便家长带孩子边走边看。";
        }
        return "，整体安排起来比较轻松。";
    }

    private String buildConvenienceText(LargeScenicArea area) {
        if (area == null) {
            return "";
        }
        boolean foodConvenient = safeDecimal(area.getFoodConvenienceScore()) >= 4.0;
        boolean restroomConvenient = safeDecimal(area.getRestroomConvenienceScore()) >= 4.0;
        if (foodConvenient && restroomConvenient) {
            return "周边补给和卫生间条件相对方便";
        }
        if (foodConvenient) {
            return "周边餐饮补给相对方便";
        }
        if (restroomConvenient) {
            return "卫生间条件相对方便";
        }
        return "";
    }

    private String buildTagFeatureText(String normalizedTags) {
        List<String> features = new ArrayList<>();
        addFeatureIfPresent(features, normalizedTags, "寺庙人文", "寺庙", "佛教");
        addFeatureIfPresent(features, normalizedTags, "静态游览", "静态游览");
        addFeatureIfPresent(features, normalizedTags, "老人友好", "老人友好", "老人", "老年人");
        addFeatureIfPresent(features, normalizedTags, "亲子游玩", "亲子", "儿童", "家庭", "孩子");
        addFeatureIfPresent(features, normalizedTags, "演艺体验", "演艺", "表演", "演出");
        addFeatureIfPresent(features, normalizedTags, "园林景观", "园林", "历史公园", "公园");
        addFeatureIfPresent(features, normalizedTags, "休闲散步", "休闲", "散步");
        addFeatureIfPresent(features, normalizedTags, "拍照打卡", "拍照", "摄影", "打卡");
        addFeatureIfPresent(features, normalizedTags, "古塔古建", "古塔", "古建");
        addFeatureIfPresent(features, normalizedTags, "历史古迹", "历史", "古迹", "文物", "北宋");
        addFeatureIfPresent(features, normalizedTags, "书法碑刻", "书法", "碑林");
        addFeatureIfPresent(features, normalizedTags, "人文参观", "人文");
        if (features.isEmpty()) {
            return "";
        }
        int limit = Math.min(3, features.size());
        StringJoiner joiner = new StringJoiner("、");
        for (int i = 0; i < limit; i++) {
            joiner.add(features.get(i));
        }
        return joiner + (features.size() > limit ? "等" : "");
    }

    private void addFeatureIfPresent(List<String> features, String tags, String feature, String... keywords) {
        if (!features.contains(feature) && containsAny(tags, keywords)) {
            features.add(feature);
        }
    }

    private String ensureSentenceEnding(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = text.trim();
        if (cleaned.endsWith("。") || cleaned.endsWith("！") || cleaned.endsWith("？")) {
            return cleaned;
        }
        return cleaned + "。";
    }

    /**
     * 根据用户问题中的关键词，返回相关的景区标签集合。
     * 用于对景点按用户需求进行排序，使结果更贴近用户意图。
     */
    private Set<String> detectRelevantTags(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptySet();
        }
        String normalized = normalize(query);

        if (containsAny(normalized, "老人", "适合老人", "老人家", "老年人")) {
            return new HashSet<>(Arrays.asList("老人友好", "老人", "老年人", "休闲", "散步", "静态游览"));
        }
        if (containsAny(normalized, "拍照", "摄影", "打卡", "拍摄")) {
            return new HashSet<>(Arrays.asList("拍照"));
        }
        if (containsAny(normalized, "历史", "古迹", "古建", "文物", "古塔", "北宋")) {
            return new HashSet<>(Arrays.asList("历史", "古迹", "古塔", "古建", "文物", "北宋"));
        }
        if (containsAny(normalized, "寺庙", "佛教", "烧香", "拜佛")) {
            return new HashSet<>(Arrays.asList("寺庙", "人文", "静态游览"));
        }
        if (containsAny(normalized, "园林", "公园", "花园")) {
            return new HashSet<>(Arrays.asList("园林", "历史公园", "散步", "休闲"));
        }
        if (containsAny(normalized, "热闹", "表演", "演艺", "演出", "节目")) {
            return new HashSet<>(Arrays.asList("演艺"));
        }
        if (containsAny(normalized, "文化", "书法", "碑林", "人文")) {
            return new HashSet<>(Arrays.asList("人文", "书法", "碑林", "历史"));
        }
        if (containsAny(normalized, "散步", "休闲", "放松", "悠闲")) {
            return new HashSet<>(Arrays.asList("散步", "休闲", "园林"));
        }
        if (containsAny(normalized, "亲子", "小孩", "孩子", "儿童", "家庭")) {
            return new HashSet<>(Arrays.asList("亲子", "儿童", "家庭", "孩子", "休闲", "散步", "园林", "历史公园"));
        }

        return Collections.emptySet();
    }

    /**
     * 统计景区标签中命中的相关标签数量。
     */
    private int countMatchingTags(String tags, Set<String> relevantTags) {
        if (tags == null || tags.isBlank() || relevantTags == null || relevantTags.isEmpty()) {
            return 0;
        }
        String normalizedTags = normalize(tags);
        int count = 0;
        for (String tag : relevantTags) {
            if (normalizedTags.contains(tag)) {
                count++;
            }
        }
        return count;
    }

    private boolean isFoodRecommendationQuery(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return false;
        }
        if (containsAny(normalized, "路线", "路径", "怎么走", "怎么去", "从", "到", "规划", "行程", "顺序", "途径", "途经", "经过")) {
            return false;
        }
        return containsAny(normalized,
                "好吃", "吃饭", "用餐", "餐饮", "美食", "小吃", "饭店", "饭馆", "餐厅", "餐馆",
                "灌汤包", "锅贴", "桶子鸡", "夜市", "有什么吃的", "有哪些吃的", "吃什么");
    }

    private String buildFoodRecommendationFacts(List<LargeScenicArea> largeAreas) {
        List<LargeScenicArea> foodPlaces = new ArrayList<>();
        if (largeAreas != null) {
            for (LargeScenicArea area : largeAreas) {
                if (area != null && isFoodRecommendationPlace(area)) {
                    foodPlaces.add(area);
                }
            }
        }
        if (foodPlaces.isEmpty()) {
            return "目前还没有查到可用于推荐的开封餐饮地点。";
        }

        foodPlaces.sort(Comparator
                .comparingDouble((LargeScenicArea area) -> safeDecimal(area.getFoodConvenienceScore())).reversed()
                .thenComparingDouble(area -> safeDecimal(area.getPopularityScore())).reversed());

        StringBuilder answer = new StringBuilder();
        int limit = Math.min(6, foodPlaces.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                answer.append("、");
            }
            answer.append(foodPlaces.get(i).getName());
        }
        answer.append("。");

        for (int i = 0; i < Math.min(4, foodPlaces.size()); i++) {
            LargeScenicArea area = foodPlaces.get(i);
            String description = trimToSentence(defaultText(area.getDescription()));
            answer.append(area.getName()).append("：");
            if (!description.isBlank() && !"暂无信息".equals(description)) {
                answer.append(description).append("。");
            }
            answer.append(buildFoodPriceText(area)).append("。");
        }
        return answer.toString();
    }

    private boolean isFoodRecommendationPlace(LargeScenicArea area) {
        String combined = buildAreaKeywordText(area);
        return isFoodPlace(area) || containsAny(combined,
                "夜市", "美食街", "步行街", "小吃街", "餐饮", "美食", "用餐", "吃饭");
    }

    private String buildFoodPriceText(LargeScenicArea area) {
        BigDecimal price = area == null ? BigDecimal.ZERO : area.getPrice();
        BigDecimal safePrice = price == null ? BigDecimal.ZERO : price;
        if (isOpenConsumptionNode(area) && safePrice.compareTo(BigDecimal.ZERO) == 0) {
            return "免费开放，可按需消费";
        }
        if (safePrice.compareTo(BigDecimal.ZERO) > 0) {
            return "人均消费参考：" + safeDecimal(safePrice) + "元";
        }
        return "可按需消费";
    }

    private boolean isPackageRecommendationQuery(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return false;
        }
        return containsAny(normalized,
                "优惠套餐", "旅游套餐", "游玩套餐", "景区套餐", "套餐推荐", "推荐套餐",
                "有哪些套餐", "有什么套餐", "有啥套餐", "套餐有哪些", "套餐", "优惠票", "组合票", "套票");
    }

    private String buildPackageRecommendationFacts() {
        List<Product> products = loadOnSaleProducts();
        if (products.isEmpty()) {
            return "目前还没有查到在售的优惠套餐。";
        }
        products.sort(Comparator
                .comparing((Product product) -> safeDecimal(product.getPrice()))
                .thenComparing(product -> defaultText(product.getName())));

        StringBuilder answer = new StringBuilder();
        int limit = Math.min(6, products.size());
        for (int i = 0; i < limit; i++) {
            Product product = products.get(i);
            if (i > 0) {
                answer.append("、");
            }
            answer.append(defaultText(product.getName()))
                    .append("（")
                    .append(String.format(Locale.ROOT, "%.2f", safeDecimal(product.getPrice())))
                    .append("元）");
        }
        answer.append("。");

        for (int i = 0; i < Math.min(4, products.size()); i++) {
            Product product = products.get(i);
            answer.append(defaultText(product.getName()))
                    .append("：")
                    .append(trimToSentence(defaultText(product.getDescription())))
                    .append("，价格")
                    .append(String.format(Locale.ROOT, "%.2f", safeDecimal(product.getPrice())))
                    .append("元");
            String coveredNames = buildProductCoveredAreaNames(product);
            if (!coveredNames.isBlank()) {
                answer.append("，覆盖").append(coveredNames);
            }
            answer.append("。");
        }
        return answer.toString();
    }

    private List<Product> loadOnSaleProducts() {
        if (productRepository != null) {
            try {
                return new ArrayList<>(productRepository.findOnSaleWithScenicAreas("ON_SALE"));
            } catch (Exception ignored) {
            }
        }
        if (jdbcTemplate == null) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, name, description, price, image_url, status FROM product WHERE status = 'ON_SALE' ORDER BY price ASC"
            );
            List<Product> products = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Product product = new Product();
                product.setId(toLong(row.get("id")));
                product.setName(row.get("name") == null ? null : String.valueOf(row.get("name")));
                product.setDescription(row.get("description") == null ? null : String.valueOf(row.get("description")));
                product.setPrice(BigDecimal.valueOf(getNumber(row.get("price"))));
                product.setImageUrl(row.get("image_url") == null ? null : String.valueOf(row.get("image_url")));
                product.setStatus(row.get("status") == null ? null : String.valueOf(row.get("status")));
                products.add(product);
            }
            return products;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private String buildProductCoveredAreaNames(Product product) {
        if (product == null || product.getLargeScenicAreas() == null || product.getLargeScenicAreas().isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("、");
        int count = 0;
        for (LargeScenicArea area : product.getLargeScenicAreas()) {
            if (area != null && area.getName() != null && !area.getName().isBlank()) {
                joiner.add(area.getName().trim());
                count++;
                if (count >= 5) {
                    break;
                }
            }
        }
        return joiner.toString();
    }

    private boolean isPathPlanningQuery(String query) {
        String normalized = normalize(query).toLowerCase(Locale.ROOT);
        return normalized.contains("路线")
                || normalized.contains("路径")
                || normalized.contains("怎么去")
                || normalized.contains("怎么走")
                || normalized.contains("怎么玩")
                || normalized.contains("如何游玩")
                || normalized.contains("怎么游玩")
                || normalized.contains("怎么游")
                || normalized.contains("怎么逛")
                || normalized.contains("游玩建议")
                || normalized.contains("游览建议")
                || (normalized.contains("从") && (normalized.contains("到") || normalized.contains("去")))
                || (normalized.contains("从") && containsAny(normalized, "开始", "出发", "起点") && containsAny(normalized, "结束", "终点", "收尾"))
                || (containsAny(normalized, "途径", "途经", "经过") && containsAny(normalized, "景区", "景点", "地点"))
                || (normalized.contains("方案") && containsAny(normalized, "景区", "景点", "起点", "终点", "开始", "结束", "途径", "途经", "经过"))
                || (normalized.contains("方案") && containsAny(normalized, "开封", "游", "旅游", "游玩"))
                || normalized.contains("规划")
                || normalized.contains("安排路线")
                || normalized.contains("推荐路线")
                || normalized.contains("行程")
                || normalized.contains("游玩顺序")
                || normalized.contains("一日游")
                || normalized.contains("二日游")
                || normalized.contains("两日游")
                || normalized.contains("三日游");
    }

    private boolean isMultiStopRouteQuery(String query, int maxStops) {
        String text = normalize(query);
        if (text.isBlank()) {
            return false;
        }
        if (maxStops == ALL_SCENIC_STOPS) {
            return true;
        }
        Matcher countMatcher = Pattern.compile("(\\d+)个(?:景区|景点|地点)").matcher(text);
        if (countMatcher.find() && safeParseInt(countMatcher.group(1), 1) > 1) {
            return true;
        }
        boolean hasMultiStopWord = containsAny(text,
                "途径", "途经", "经过", "顺路", "串联", "多点", "多个景区", "多个景点",
                "几个景区", "几个景点", "两日游", "二日游", "三日游", "一日游", "行程",
                "游玩顺序", "安排路线", "推荐路线", "城市路线");
        if (!hasMultiStopWord && text.contains("方案")) {
            hasMultiStopWord = containsAny(text, "景区", "景点", "地点", "开始", "结束", "起点", "终点");
        }
        if (!hasMultiStopWord) {
            return false;
        }
        return maxStops > 1
                || containsAny(text, "两个景区", "两个景点", "三个景区", "三个景点", "四个景区", "四个景点", "五个景区", "五个景点");
    }

    private String handlePathPlanningQuery(String query) {
        List<LargeScenicArea> allAreas = largeScenicAreaRepository.findAll();
        if (allAreas.isEmpty()) {
            return "当前还没有可用于路线规划的景区数据。";
        }

        Map<String, String> locations = extractLocations(query, allAreas);
        LargeScenicArea startArea = findAreaByName(locations.get("start"), allAreas);
        LargeScenicArea endArea = findAreaByName(locations.get("end"), allAreas);
        List<LargeScenicArea> mentionedAreas = extractMentionedAreas(query, allAreas);

        if (startArea == null && !mentionedAreas.isEmpty() && normalize(query).contains("从")) {
            startArea = mentionedAreas.get(0);
        }
        if (endArea == null && mentionedAreas.size() >= 2) {
            endArea = mentionedAreas.get(mentionedAreas.size() - 1);
        }

        Map<String, Double> preferenceWeights = extractPreferenceWeights(query);
        String routeMode = extractRouteMode(query, preferenceWeights);
        int maxStops = extractMaxStops(query);
        boolean multiStopRoute = isMultiStopRouteQuery(query, maxStops);

        LargeScenicArea singleArea = resolveSingleAreaRouteTarget(startArea, endArea, mentionedAreas);
        if (singleArea != null && isSingleAreaTourIntent(query, startArea, endArea, mentionedAreas, singleArea)) {
            return buildSingleAreaTourAnswer(singleArea, query, preferenceWeights);
        }

        if (startArea != null && endArea != null && !Objects.equals(startArea.getId(), endArea.getId()) && !multiStopRoute) {
            Map<String, Object> selectedPath = selectRouteResult(startArea.getId(), endArea.getId(), routeMode, preferenceWeights);
            if (!Boolean.TRUE.equals(selectedPath.get("success"))) {
                return String.valueOf(selectedPath.getOrDefault("message", "暂时无法规划该路线。"));
            }

            Map<String, Object> distancePath = pathService.calculateShortestPath(startArea.getId(), endArea.getId(), MODE_DISTANCE);
            Map<String, Object> timePath = pathService.calculateShortestPath(startArea.getId(), endArea.getId(), MODE_DURATION);
            return buildSingleRouteAnswer(startArea, endArea, preferenceWeights, routeMode, selectedPath, distancePath, timePath);
        }

        Long preferredStartId = startArea == null ? null : startArea.getId();
        Long preferredEndId = endArea == null ? null : endArea.getId();
        Map<String, Object> cityRoute = pathService.recommendCityRoute(preferredStartId, preferredEndId, preferenceWeights, routeMode, maxStops);
        if (!Boolean.TRUE.equals(cityRoute.get("success"))) {
            return String.valueOf(cityRoute.getOrDefault("message", "暂时无法生成城市内景区推荐路线。"));
        }
        return buildCityRouteAnswer(startArea, endArea, preferenceWeights, routeMode, cityRoute, maxStops);
    }

    private LargeScenicArea resolveSingleAreaRouteTarget(LargeScenicArea startArea,
                                                         LargeScenicArea endArea,
                                                         List<LargeScenicArea> mentionedAreas) {
        Set<Long> ids = new LinkedHashSet<>();
        LargeScenicArea candidate = null;
        if (startArea != null && safeInt(startArea.getIsAreaType()) == 0) {
            ids.add(startArea.getId());
            candidate = startArea;
        }
        if (endArea != null && safeInt(endArea.getIsAreaType()) == 0) {
            ids.add(endArea.getId());
            candidate = endArea;
        }
        for (LargeScenicArea area : mentionedAreas) {
            if (area != null && safeInt(area.getIsAreaType()) == 0) {
                ids.add(area.getId());
                if (candidate == null) {
                    candidate = area;
                }
            }
        }
        return ids.size() == 1 ? candidate : null;
    }

    private boolean isSingleAreaTourIntent(String query,
                                           LargeScenicArea startArea,
                                           LargeScenicArea endArea,
                                           List<LargeScenicArea> mentionedAreas,
                                           LargeScenicArea singleArea) {
        if (singleArea == null) {
            return false;
        }
        if (startArea != null && endArea != null && !Objects.equals(startArea.getId(), endArea.getId())) {
            return false;
        }

        String text = normalize(query);
        boolean insideKeywords = containsAny(text,
                "园内", "景区内", "入园", "进入", "进园", "游玩", "游览", "逛", "怎么玩", "怎么游",
                "游玩路线", "游览路线", "游玩顺序", "游览顺序", "北门", "南门", "东门", "西门", "入口", "大门");
        boolean trafficOnly = containsAny(text, "怎么去", "到那里", "去那里", "去景区", "到景区")
                && !containsAny(text, "游玩", "游览", "园内", "景区内", "怎么玩", "游玩路线", "游玩顺序");
        return insideKeywords && !trafficOnly;
    }

    private String buildSingleAreaTourAnswer(LargeScenicArea area,
                                             String query,
                                             Map<String, Double> preferenceWeights) {
        List<SmallScenicSpot> allSpots = smallScenicSpotRepository.findAll();
        List<SmallScenicSpot> areaSpots = new ArrayList<>();
        for (SmallScenicSpot spot : allSpots) {
            if (spot != null && Objects.equals(spot.getLargeAreaId(), area.getId())) {
                areaSpots.add(spot);
            }
        }

        if (areaSpots.isEmpty()) {
            StringBuilder answer = new StringBuilder();
            answer.append("景区：").append(area.getName()).append("\n");
            answer.append("建议游玩时长：约").append(safeInt(area.getRecommendedVisitDuration())).append("分钟\n");
            String priceText = buildAreaPriceText(area);
            if (priceText != null && !priceText.isBlank()) {
                answer.append(priceText).append("\n");
            }
            answer.append("说明：目前还没有查到该景区更详细的园内点位介绍，所以这次先只给出该景区本身的游玩建议。\n");
            return answer.toString();
        }

        SmallScenicSpot entrySpot = selectEntrySpot(query, areaSpots);
        List<SmallScenicSpot> orderedSpots = selectOrderedInsideSpots(area.getId(), areaSpots, entrySpot, preferenceWeights);
        Map<String, Integer> entryEdgeInfo = entrySpot == null ? Collections.emptyMap() : loadEntryEdgeInfo(area.getId(), entrySpot.getId(), orderedSpots);
        Integer entryDistance = entryEdgeInfo.get("distance");
        Integer entryTime = entryEdgeInfo.get("time");

        int totalSpotDuration = 0;
        for (SmallScenicSpot spot : orderedSpots) {
            if (safeInt(spot.getIsSpotType()) == 0) {
                totalSpotDuration += safeInt(spot.getVisitingDuration());
            }
        }
        int suggestedDuration = Math.max(safeInt(area.getRecommendedVisitDuration()), totalSpotDuration);

        StringBuilder answer = new StringBuilder();
        answer.append("景区：").append(area.getName()).append("\n");
        if (entrySpot != null) {
            answer.append("入园起点：").append(entrySpot.getName());
            if (isSpecificEntryRequested(query) && !isEntryAlignedWithQuery(entrySpot, query)) {
                answer.append("（未完全匹配到你指定的门口，已按景区内最接近的真实入口处理）");
            }
            answer.append("\n");
            if (orderedSpots.size() > 1 && entryDistance != null && entryDistance > 0) {
                answer.append("入园后建议先前往：")
                        .append(orderedSpots.get(1).getName())
                        .append("，步行约")
                        .append(entryDistance)
                        .append("米");
                if (entryTime != null && entryTime > 0) {
                    answer.append("，约").append(entryTime).append("分钟");
                }
                answer.append("\n");
            }
        }
        answer.append("推荐园内顺序：\n");
        for (int i = 0; i < orderedSpots.size(); i++) {
            SmallScenicSpot spot = orderedSpots.get(i);
            answer.append(i + 1)
                    .append(". ")
                    .append(spot.getName());
            if (safeInt(spot.getIsSpotType()) == 1) {
                answer.append("（入园节点）");
            } else {
                answer.append("（约")
                        .append(safeInt(spot.getVisitingDuration()))
                        .append("分钟）");
            }
            String desc = defaultText(spot.getDescription());
            if (!"暂无信息".equals(desc)) {
                answer.append("：").append(desc);
            }
            answer.append("\n");
        }

        if (!preferenceWeights.isEmpty()) {
            String preferenceSummary = buildPreferenceSummary(preferenceWeights);
            if (preferenceSummary != null && !preferenceSummary.trim().isEmpty()
                    && !"未检测到明确偏好，默认按较高效率规划".equals(preferenceSummary)) {
                answer.append("\n识别到的偏好：").append(preferenceSummary).append("\n");
            }
        }

        answer.append("\n建议游玩时长：约")
                .append(suggestedDuration)
                .append("分钟\n");
        String priceText = buildAreaPriceText(area);
        if (priceText != null && !priceText.isBlank()) {
            answer.append(priceText).append("\n");
        }
        return answer.toString();
    }

    private SmallScenicSpot selectEntrySpot(String query, List<SmallScenicSpot> areaSpots) {
        String text = normalize(query);
        List<SmallScenicSpot> facilitySpots = new ArrayList<>();
        SmallScenicSpot firstSpot = areaSpots.isEmpty() ? null : areaSpots.get(0);
        for (SmallScenicSpot spot : areaSpots) {
            if (safeInt(spot.getIsSpotType()) == 1) {
                facilitySpots.add(spot);
            }
        }
        facilitySpots.sort((a, b) -> Integer.compare(normalize(b.getName()).length(), normalize(a.getName()).length()));

        for (SmallScenicSpot spot : facilitySpots) {
            String name = normalize(spot.getName());
            if (!name.isEmpty() && text.contains(name)) {
                return spot;
            }
        }

        for (SmallScenicSpot spot : facilitySpots) {
            if (isEntryAlignedWithQuery(spot, query)) {
                return spot;
            }
        }

        for (SmallScenicSpot spot : facilitySpots) {
            String name = normalize(spot.getName());
            if ((containsAny(text, "入口", "入园", "进园", "进入", "大门", "门口") && containsAny(name, "门", "入口", "游客中心"))
                    || name.contains("入口") || name.contains("游客中心") || name.contains("迎宾门")) {
                return spot;
            }
        }

        return !facilitySpots.isEmpty() ? facilitySpots.get(0) : firstSpot;
    }

    private List<SmallScenicSpot> selectOrderedInsideSpots(Long areaId,
                                                           List<SmallScenicSpot> areaSpots,
                                                           SmallScenicSpot entrySpot,
                                                           Map<String, Double> preferenceWeights) {
        List<SmallScenicSpot> scenicSpots = new ArrayList<>();
        for (SmallScenicSpot spot : areaSpots) {
            if (entrySpot != null && Objects.equals(entrySpot.getId(), spot.getId())) {
                continue;
            }
            if (safeInt(spot.getIsSpotType()) == 0) {
                scenicSpots.add(spot);
            }
        }

        SmallScenicSpot linkedFirstSpot = findFirstConnectedScenicSpot(areaId, entrySpot, scenicSpots);
        if (linkedFirstSpot != null) {
            scenicSpots.removeIf(spot -> Objects.equals(spot.getId(), linkedFirstSpot.getId()));
        }

        boolean hasExplicitPreference = hasInsidePreference(preferenceWeights);
        scenicSpots.sort((a, b) -> {
            if (hasExplicitPreference) {
                int scoreCompare = Double.compare(scoreSpot(b, preferenceWeights), scoreSpot(a, preferenceWeights));
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
            }
            return Long.compare(a.getId(), b.getId());
        });

        List<SmallScenicSpot> ordered = new ArrayList<>();
        if (entrySpot != null) {
            ordered.add(entrySpot);
        }
        if (linkedFirstSpot != null) {
            ordered.add(linkedFirstSpot);
        }
        ordered.addAll(scenicSpots);
        if (ordered.isEmpty()) {
            ordered.addAll(areaSpots);
        }
        return ordered;
    }

    private SmallScenicSpot findFirstConnectedScenicSpot(Long areaId,
                                                         SmallScenicSpot entrySpot,
                                                         List<SmallScenicSpot> scenicSpots) {
        if (entrySpot == null || scenicSpots == null || scenicSpots.isEmpty() || jdbcTemplate == null) {
            return null;
        }
        Map<Long, SmallScenicSpot> spotMap = new HashMap<>();
        for (SmallScenicSpot spot : scenicSpots) {
            if (spot != null && spot.getId() != null) {
                spotMap.put(spot.getId(), spot);
            }
        }
        if (spotMap.isEmpty()) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT to_spot_id AS targetId, distance, time_cost FROM scenic_edge WHERE large_area_id = ? AND from_spot_id = ? " +
                            "UNION ALL " +
                            "SELECT from_spot_id AS targetId, distance, time_cost FROM scenic_edge WHERE large_area_id = ? AND to_spot_id = ?",
                    areaId, entrySpot.getId(), areaId, entrySpot.getId());
            rows.sort(Comparator
                    .comparingDouble((Map<String, Object> row) -> getNumber(row.get("distance")))
                    .thenComparingDouble(row -> getNumber(row.get("time_cost"))));
            for (Map<String, Object> row : rows) {
                Long targetId = toLong(row.get("targetId"));
                SmallScenicSpot target = targetId == null ? null : spotMap.get(targetId);
                if (target != null) {
                    return target;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private Map<String, Integer> loadEntryEdgeInfo(Long areaId,
                                                   Long entrySpotId,
                                                   List<SmallScenicSpot> orderedSpots) {
        Map<String, Integer> result = new HashMap<>();
        if (jdbcTemplate == null || areaId == null || entrySpotId == null || orderedSpots == null || orderedSpots.size() < 2) {
            return result;
        }
        Long nextSpotId = orderedSpots.get(1).getId();
        if (nextSpotId == null) {
            return result;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT distance, time_cost FROM scenic_edge WHERE large_area_id = ? AND " +
                            "((from_spot_id = ? AND to_spot_id = ?) OR (from_spot_id = ? AND to_spot_id = ?)) " +
                            "ORDER BY distance ASC, time_cost ASC LIMIT 1",
                    areaId, entrySpotId, nextSpotId, nextSpotId, entrySpotId);
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                result.put("distance", (int) Math.round(getNumber(row.get("distance"))));
                result.put("time", (int) Math.round(getNumber(row.get("time_cost"))));
            }
        } catch (Exception ignored) {
            return result;
        }
        return result;
    }

    private boolean isSpecificEntryRequested(String query) {
        String text = normalize(query);
        return containsAny(text,
                "北门", "南门", "东门", "西门", "北大门", "南大门", "东大门", "西大门",
                "午门", "山门", "府门", "中门", "端门", "金水门", "丹凤门", "通津门", "东便门", "迎宾门");
    }

    private boolean isEntryAlignedWithQuery(SmallScenicSpot spot, String query) {
        if (spot == null) {
            return false;
        }
        String text = normalize(query);
        String name = normalize(spot.getName());
        if (name.isEmpty()) {
            return false;
        }
        if (text.contains(name)) {
            return true;
        }
        if (name.contains("北") && name.contains("门") && text.contains("北门")) {
            return true;
        }
        if (name.contains("南") && name.contains("门") && text.contains("南门")) {
            return true;
        }
        if (name.contains("东") && name.contains("门") && text.contains("东门")) {
            return true;
        }
        if (name.contains("西") && name.contains("门") && text.contains("西门")) {
            return true;
        }
        return false;
    }

    private boolean hasInsidePreference(Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return false;
        }
        return weights.getOrDefault("nature", 0.0) > 0.0
                || weights.getOrDefault("culture", 0.0) > 0.0
                || weights.getOrDefault("photography", 0.0) > 0.0
                || weights.getOrDefault("familyFriendly", 0.0) > 0.0
                || weights.getOrDefault("elderlyFriendly", 0.0) > 0.0
                || weights.getOrDefault("leisure", 0.0) > 0.0
                || weights.getOrDefault("foodConvenience", 0.0) > 0.0
                || weights.getOrDefault("restroomConvenience", 0.0) > 0.0
                || weights.getOrDefault("popularity", 0.0) > 0.0
                || weights.getOrDefault("intensity", 0.0) > 0.0
                || weights.getOrDefault("crowd", 0.0) > 0.0;
    }

    private double scoreSpot(SmallScenicSpot spot, Map<String, Double> weights) {
        double score = 0.0;
        score += safeDecimal(spot.getNatureScore()) * weights.getOrDefault("nature", 0.0);
        score += safeDecimal(spot.getCultureScore()) * weights.getOrDefault("culture", 0.0);
        score += safeDecimal(spot.getPhotographyScore()) * weights.getOrDefault("photography", 0.0);
        score += safeDecimal(spot.getFamilyFriendlyScore()) * weights.getOrDefault("familyFriendly", 0.0);
        score += safeDecimal(spot.getElderlyFriendlyScore()) * weights.getOrDefault("elderlyFriendly", 0.0);
        score += safeDecimal(spot.getRestConvenienceScore()) * (weights.getOrDefault("leisure", 0.0)
                + weights.getOrDefault("restroomConvenience", 0.0) * 0.8
                + weights.getOrDefault("comfort", 0.0) * 0.5);
        score += (5 - safeInt(spot.getIntensityLevel())) * weights.getOrDefault("intensity", 0.0);
        score += (5 - safeInt(spot.getQueueLevel())) * weights.getOrDefault("crowd", 0.0);
        score += safeInt(spot.getVisitingDuration()) > 0
                ? (120.0 / Math.max(20.0, safeInt(spot.getVisitingDuration()))) * weights.getOrDefault(MODE_DURATION, 0.0)
                : 0.0;
        return score;
    }

    private Map<String, Object> selectRouteResult(Long startAreaId,
                                                  Long endAreaId,
                                                  String routeMode,
                                                  Map<String, Double> preferenceWeights) {
        if (MODE_DISTANCE.equals(routeMode)) {
            return pathService.calculateShortestPath(startAreaId, endAreaId, MODE_DISTANCE);
        }
        if (MODE_DURATION.equals(routeMode)) {
            return pathService.calculateShortestPath(startAreaId, endAreaId, MODE_DURATION);
        }
        return pathService.calculatePersonalizedPath(startAreaId, endAreaId, preferenceWeights);
    }

    private Map<String, String> extractLocations(String query, List<LargeScenicArea> areas) {
        Map<String, String> result = new HashMap<>();
        List<Pattern> patterns = Arrays.asList(
                Pattern.compile("从(.+?)(?:开始|出发|起点|起始)[，,]?(?:.*?)(?:到|至|在)?(.+?)(?:结束|为终点|作为终点|终点|收尾|结束点)(?:[，。？！?]|$)"),
                Pattern.compile("从(.+?)(?:到|去)(.+?)(?:怎么走|怎么去|如何走|路线|路径|规划|安排|推荐|[，。？！?]|$)"),
                Pattern.compile("(.+?)到(.+?)(?:怎么走|怎么去|如何走|路线|路径|规划|安排|推荐|[，。？！?]|$)")
        );

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(query);
            if (matcher.find()) {
                LargeScenicArea startArea = findAreaByName(matcher.group(1).trim(), areas);
                LargeScenicArea endArea = findAreaByName(matcher.group(2).trim(), areas);
                if (startArea != null && endArea != null) {
                    result.put("start", startArea.getName());
                    result.put("end", endArea.getName());
                    return result;
                }
            }
        }

        List<LargeScenicArea> hits = extractMentionedAreas(query, areas);
        if (hits.size() >= 2) {
            result.put("start", hits.get(0).getName());
            result.put("end", hits.get(1).getName());
        }
        return result;
    }

    private List<LargeScenicArea> extractMentionedAreas(String query, List<LargeScenicArea> areas) {
        List<NameHit> hits = new ArrayList<>();
        String normalizedQuery = normalize(query);
        for (LargeScenicArea area : areas) {
            String normalizedName = normalize(area.getName());
            int index = normalizedQuery.indexOf(normalizedName);
            if (index >= 0) {
                hits.add(new NameHit(area.getName(), index, normalizedName.length()));
                continue;
            }
            for (String alias : buildAreaAliasCandidates(normalizedName)) {
                if (alias.equals(normalizedName) || isGenericAreaAlias(alias)) {
                    continue;
                }
                index = normalizedQuery.indexOf(alias);
                if (index >= 0) {
                    hits.add(new NameHit(area.getName(), index, alias.length()));
                    break;
                }
            }
        }
        hits.sort(Comparator.comparingInt(NameHit::getIndex)
                .thenComparing((a, b) -> Integer.compare(b.getLength(), a.getLength())));

        List<LargeScenicArea> result = new ArrayList<>();
        Set<Long> used = new LinkedHashSet<>();
        for (NameHit hit : hits) {
            LargeScenicArea area = findAreaByName(hit.getName(), areas);
            if (area != null && !used.contains(area.getId())) {
                result.add(area);
                used.add(area.getId());
            }
        }
        return result;
    }

    private LargeScenicArea findAreaByName(String name, List<LargeScenicArea> areas) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String normalizedInput = normalize(name);
        String normalizedInputCore = normalizeAreaQueryName(normalizedInput);
        LargeScenicArea bestMatch = null;
        int bestScore = -1;

        for (LargeScenicArea area : areas) {
            String normalizedAreaName = normalize(area.getName());
            int score = -1;
            if (normalizedAreaName.equals(normalizedInput)) {
                score = 1000;
            } else if (isAreaAliasMatch(normalizedInput, normalizedAreaName)) {
                score = 900;
            } else if (!isGenericAreaAlias(normalizedInputCore) && isAreaAliasMatch(normalizedInputCore, normalizedAreaName)) {
                score = 850;
            } else if (normalizedAreaName.contains(normalizedInput)) {
                score = normalizedInput.length() + 100;
            } else if (normalizedInput.contains(normalizedAreaName)) {
                score = normalizedAreaName.length() + 50;
            } else if (!isGenericAreaAlias(normalizedInputCore) && normalizedAreaName.contains(normalizedInputCore)) {
                score = normalizedInputCore.length() + 80;
            }
            if (score > bestScore) {
                bestScore = score;
                bestMatch = area;
            }
        }
        return bestMatch;
    }

    private boolean isAreaAliasMatch(String normalizedInput, String normalizedAreaName) {
        for (String inputAlias : buildAreaAliasCandidates(normalizedInput)) {
            if (isGenericAreaAlias(inputAlias)) {
                continue;
            }
            for (String areaAlias : buildAreaAliasCandidates(normalizedAreaName)) {
                if (!isGenericAreaAlias(areaAlias) && inputAlias.equals(areaAlias)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> buildAreaAliasCandidates(String value) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        String normalized = normalize(value);
        addCandidateVariant(aliases, normalized);

        String queryName = normalizeAreaQueryName(normalized);
        addCandidateVariant(aliases, queryName);

        if ("火车站".equals(normalized)
                || "开封火车站".equals(normalized)
                || "开封市火车站".equals(normalized)
                || "开封站".equals(normalized)) {
            addCandidateVariant(aliases, "开封站");
            return aliases;
        }
        String cityless = queryName;
        if (cityless.startsWith("开封市") && cityless.length() > 3) {
            cityless = cityless.substring(3);
        } else if (cityless.startsWith("开封") && cityless.length() > 2) {
            cityless = cityless.substring(2);
        }
        addCandidateVariant(aliases, cityless);

        String[] suffixes = {"景区", "公园"};
        for (String suffix : suffixes) {
            if (cityless.endsWith(suffix) && cityless.length() > suffix.length() + 1) {
                addCandidateVariant(aliases, cityless.substring(0, cityless.length() - suffix.length()));
            }
        }
        return aliases;
    }

    private String normalizeAreaQueryName(String value) {
        String cleaned = stripScenicCandidatePrefix(value);
        String[] suffixes = {
                "如何游玩", "怎么游玩", "怎么玩", "怎么游", "怎么逛", "如何玩", "如何逛",
                "游玩路线", "游览路线", "游玩建议", "游览建议", "游玩攻略", "游览攻略",
                "游玩方案", "游览方案", "旅游方案", "路线", "方案", "攻略", "介绍",
                "游玩", "游览", "旅游", "玩", "逛"
        };
        boolean changed;
        do {
            changed = false;
            for (String suffix : suffixes) {
                if (cleaned.endsWith(suffix) && cleaned.length() > suffix.length()) {
                    cleaned = cleaned.substring(0, cleaned.length() - suffix.length());
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return cleaned;
    }

    private boolean isGenericAreaAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return true;
        }
        return "开封".equals(alias)
                || "开封市".equals(alias)
                || "景区".equals(alias)
                || "景点".equals(alias)
                || "公园".equals(alias)
                || "地方".equals(alias)
                || alias.length() <= 1;
    }

    private SmallScenicSpot findSpotByName(String name, List<SmallScenicSpot> spots) {
        if (name == null || name.trim().isEmpty() || spots == null || spots.isEmpty()) {
            return null;
        }
        String normalizedInput = normalize(name);
        SmallScenicSpot bestMatch = null;
        int bestScore = -1;

        for (SmallScenicSpot spot : spots) {
            if (spot == null || spot.getName() == null || spot.getName().isBlank()) {
                continue;
            }
            String normalizedSpotName = normalize(spot.getName());
            int score = -1;
            if (normalizedSpotName.equals(normalizedInput)) {
                score = 1000;
            } else if (normalizedSpotName.contains(normalizedInput)) {
                score = normalizedInput.length() + 100;
            } else if (normalizedInput.contains(normalizedSpotName)) {
                score = normalizedSpotName.length() + 50;
            }
            if (score > bestScore) {
                bestScore = score;
                bestMatch = spot;
            }
        }
        return bestMatch;
    }

    private boolean isLikelySpecificScenicQuery(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return false;
        }
        if ((normalized.contains("景点") || normalized.contains("景区") || normalized.contains("地方"))
                && containsAny(normalized, "有哪些", "有什么", "什么", "哪些", "推荐", "知名", "著名", "热门", "好玩", "路线", "规划")) {
            return false;
        }
        if (normalized.contains("这个景点") || normalized.contains("这个地方") || normalized.contains("这个点位")) {
            return true;
        }

        Matcher scenicMatcher = SCENIC_NAME_PATTERN.matcher(query);
        while (scenicMatcher.find()) {
            String candidate = normalize(scenicMatcher.group());
            if (!isIgnorableScenicCandidate(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnrecordedSpecificScenicQuery(String query, Set<String> allowedNames) {
        List<String> candidates = extractSpecificScenicCandidates(query);
        if (candidates.isEmpty()) {
            return false;
        }
        for (String candidate : candidates) {
            if (!isKnownScenicCandidate(candidate, allowedNames)) {
                return true;
            }
        }
        return false;
    }

    private List<String> extractSpecificScenicCandidates(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        List<String> candidates = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();
        Matcher scenicMatcher = SCENIC_NAME_PATTERN.matcher(query);
        while (scenicMatcher.find()) {
            String candidate = normalize(scenicMatcher.group());
            if (candidate.isBlank() || isIgnorableScenicCandidate(candidate)) {
                continue;
            }
            if (used.add(candidate)) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private boolean isKnownScenicCandidate(String candidate, Set<String> allowedNames) {
        for (String variant : buildScenicCandidateVariants(candidate)) {
            if (isWhitelistedScenicName(variant, allowedNames)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInsufficientOrUnsupportedTourismQuery(String query, Set<String> allowedNames) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return false;
        }
        if (containsAllowedScenicName(normalized, allowedNames)) {
            return false;
        }
        if (!extractSpecificScenicCandidates(query).isEmpty()) {
            return false;
        }
        if (isLowInformationTourismQuery(normalized)) {
            return true;
        }
        if (isMetaOrTestOnlyQuery(normalized)) {
            return true;
        }
        return !hasTourismIntent(normalized);
    }

    private boolean containsAllowedScenicName(String normalizedQuery, Set<String> allowedNames) {
        if (normalizedQuery == null || normalizedQuery.isBlank() || allowedNames == null || allowedNames.isEmpty()) {
            return false;
        }
        for (String name : allowedNames) {
            String normalizedName = normalize(name);
            if (!normalizedName.isBlank() && normalizedQuery.contains(normalizedName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLowInformationTourismQuery(String normalized) {
        String cleaned = stripNoiseForIntent(normalized);
        return cleaned.isBlank()
                || "游".equals(cleaned)
                || "玩".equals(cleaned)
                || "旅游".equals(cleaned)
                || "游玩".equals(cleaned)
                || "景点".equals(cleaned)
                || "景区".equals(cleaned)
                || "路线".equals(cleaned);
    }

    private boolean isMetaOrTestOnlyQuery(String normalized) {
        if (!containsAny(normalized,
                "白名单", "校验", "测试", "用例", "留言问题", "游客问题解决完成", "问题解决完成",
                "系统", "数据库", "字段", "接口", "代码", "提示词", "回答不好", "白名单校验")) {
            return false;
        }
        String cleaned = stripNoiseForIntent(normalized)
                .replace("白名单", "")
                .replace("校验", "")
                .replace("测试", "")
                .replace("用例", "")
                .replace("留言问题", "")
                .replace("游客问题解决完成", "")
                .replace("问题解决完成", "")
                .replace("回答不好", "")
                .replace("系统", "")
                .replace("数据库", "")
                .replace("字段", "")
                .replace("接口", "")
                .replace("代码", "")
                .replace("提示词", "");
        return isLowInformationTourismQuery(cleaned);
    }

    private String stripNoiseForIntent(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return "";
        }
        return normalized
                .replaceAll("[\\p{Punct}，。！？；：、“”‘’（）【】《》]", "")
                .replaceAll("[0-9一二三四五六七八九十]+", "")
                .trim();
    }

    private boolean hasTourismIntent(String normalized) {
        return containsAny(normalized,
                "开封", "景点", "景区", "公园", "寺", "桥", "楼", "府", "祠", "台", "馆", "园",
                "博物馆", "展区", "夜市", "美食", "餐厅", "餐馆", "饭店", "饭馆", "小吃", "好吃", "吃饭", "用餐", "餐饮", "吃什么",
                "灌汤包", "锅贴", "桶子鸡", "游玩", "旅游", "游览", "方案", "好玩", "地方", "套餐", "优惠", "套票",
                "怎么玩", "如何游玩", "怎么去", "怎么走", "路线", "路径", "行程", "一日游", "二日游",
                "门票", "票价", "开放", "开放时间", "推荐", "值得", "打卡", "拍照", "亲子", "老人",
                "历史", "文化", "交通", "入口", "大门", "起点", "终点");
    }

    private Set<String> buildScenicCandidateVariants(String candidate) {
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        String cleaned = normalize(candidate);
        addCandidateVariant(variants, cleaned);

        String withoutPrefix = stripScenicCandidatePrefix(cleaned);
        addCandidateVariant(variants, withoutPrefix);

        if (withoutPrefix.startsWith("河南开封") && withoutPrefix.length() > 6) {
            addCandidateVariant(variants, withoutPrefix.substring(4));
        }
        if (withoutPrefix.startsWith("开封市") && withoutPrefix.length() > 5) {
            addCandidateVariant(variants, withoutPrefix.substring(3));
        }
        if (withoutPrefix.startsWith("开封") && withoutPrefix.length() > 4) {
            addCandidateVariant(variants, withoutPrefix.substring(2));
        }
        return variants;
    }

    private void addCandidateVariant(Set<String> variants, String candidate) {
        if (candidate != null && !candidate.isBlank()) {
            variants.add(candidate);
        }
    }

    private String stripScenicCandidatePrefix(String candidate) {
        String cleaned = normalize(candidate);
        boolean changed;
        do {
            changed = false;
            String[] prefixes = {
                    "河南省开封市的", "河南开封市的", "河南开封的", "开封市的", "开封的",
                    "请问", "麻烦问下", "帮我看看", "帮我查下", "请介绍一下", "介绍一下",
                    "请给出", "给出", "请给我", "给我", "再给我", "再给", "再推荐", "换一组", "换几个",
                    "请提供一下", "提供一下", "请提供", "提供", "再提供",
                    "推荐一下", "推荐", "查一下", "查查", "看看", "了解一下", "了解",
                    "我想去", "想去", "我要去", "去"
            };
            for (String prefix : prefixes) {
                if (cleaned.startsWith(prefix) && cleaned.length() > prefix.length()) {
                    cleaned = cleaned.substring(prefix.length());
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return cleaned;
    }

    private boolean isIgnorableScenicCandidate(String candidate) {
        String cleaned = stripScenicCandidatePrefix(candidate);
        if (cleaned.isBlank() || isPublicRouteNodeCandidate(cleaned) || isGenericScenicMentionPhrase(cleaned)) {
            return true;
        }
        String cityless = cleaned;
        if (cityless.startsWith("开封市") && cityless.length() > 3) {
            cityless = cityless.substring(3);
        } else if (cityless.startsWith("开封") && cityless.length() > 2) {
            cityless = cityless.substring(2);
        }
        if (isGenericScenicMentionPhrase(cityless)) {
            return true;
        }
        String[] inquiryPrefixes = {"有哪些", "有什么", "有啥", "什么", "哪些", "推荐", "知名", "著名", "热门", "好玩", "值得", "多少", "几个"};
        for (String prefix : inquiryPrefixes) {
            if (cityless.startsWith(prefix) && cityless.length() > prefix.length()) {
                String tail = cityless.substring(prefix.length()).replaceFirst("^的", "");
                return isGenericScenicMentionPhrase(tail)
                        || (containsAny(tail, "好玩", "值得", "知名", "著名", "热门") && endsWithGenericScenicCategory(tail));
            }
        }
        return false;
    }

    private boolean isPublicRouteNodeCandidate(String candidate) {
        String normalizedCandidate = normalize(candidate);
        if (normalizedCandidate.isBlank()) {
            return false;
        }
        if (normalizedCandidate.startsWith("开封市") && normalizedCandidate.length() > 3) {
            normalizedCandidate = normalizedCandidate.substring(3);
        } else if (normalizedCandidate.startsWith("开封") && normalizedCandidate.length() > 2 && !"开封站".equals(normalizedCandidate)) {
            normalizedCandidate = normalizedCandidate.substring(2);
        }
        if ("开封站".equals(normalizedCandidate)
                || "火车站".equals(normalizedCandidate)
                || "高铁站".equals(normalizedCandidate)
                || "汽车站".equals(normalizedCandidate)
                || "客运站".equals(normalizedCandidate)
                || "公交站".equals(normalizedCandidate)
                || "车站".equals(normalizedCandidate)
                || "公交枢纽".equals(normalizedCandidate)) {
            return true;
        }
        return normalizedCandidate.endsWith("火车站")
                || normalizedCandidate.endsWith("高铁站")
                || normalizedCandidate.endsWith("汽车站")
                || normalizedCandidate.endsWith("客运站")
                || normalizedCandidate.endsWith("公交站")
                || normalizedCandidate.endsWith("公交枢纽");
    }

    private boolean isGenericScenicMentionPhrase(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return true;
        }
        String normalizedCandidate = normalize(candidate);
        if (containsAny(normalizedCandidate,
                "没有这个景点", "没有这个景区", "未录入这个景点", "未录入这个景区",
                "没有录入这个景点", "没有录入这个景区", "没有查到这个景点", "没有查到这个景区")) {
            return true;
        }
        if (endsWithGenericScenicCategory(normalizedCandidate)
                && containsAny(normalizedCandidate,
                "适合", "亲子", "老人", "老年人", "老人家", "孩子", "小孩", "儿童", "家庭",
                "游玩", "好玩", "值得", "推荐", "哪些", "什么", "去的")) {
            return true;
        }
        return normalizedCandidate.length() <= 1
                || "景区".equals(normalizedCandidate)
                || "景点".equals(normalizedCandidate)
                || "更多景区".equals(normalizedCandidate)
                || "更多景点".equals(normalizedCandidate)
                || "更多地点".equals(normalizedCandidate)
                || "更多地方".equals(normalizedCandidate)
                || "其他景区".equals(normalizedCandidate)
                || "其他景点".equals(normalizedCandidate)
                || "其它景区".equals(normalizedCandidate)
                || "其它景点".equals(normalizedCandidate)
                || "别的景区".equals(normalizedCandidate)
                || "别的景点".equals(normalizedCandidate)
                || "新的景区".equals(normalizedCandidate)
                || "新的景点".equals(normalizedCandidate)
                || "公园".equals(normalizedCandidate)
                || "寺".equals(normalizedCandidate)
                || "桥".equals(normalizedCandidate)
                || "码头".equals(normalizedCandidate)
                || "门".equals(normalizedCandidate)
                || "楼".equals(normalizedCandidate)
                || "府".equals(normalizedCandidate)
                || "祠".equals(normalizedCandidate)
                || "台".equals(normalizedCandidate)
                || "城".equals(normalizedCandidate)
                || "站".equals(normalizedCandidate)
                || "中心".equals(normalizedCandidate)
                || "夜市".equals(normalizedCandidate)
                || "街".equals(normalizedCandidate)
                || "馆".equals(normalizedCandidate)
                || "园".equals(normalizedCandidate)
                || "林".equals(normalizedCandidate)
                || "树林".equals(normalizedCandidate)
                || "这个景点".equals(normalizedCandidate)
                || "这个景区".equals(normalizedCandidate)
                || "这个点位".equals(normalizedCandidate)
                || "这个地方".equals(normalizedCandidate)
                || "该景点".equals(normalizedCandidate)
                || "该景区".equals(normalizedCandidate)
                || "该点位".equals(normalizedCandidate)
                || "该地方".equals(normalizedCandidate);
    }

    private boolean endsWithGenericScenicCategory(String candidate) {
        return candidate != null && (candidate.endsWith("景点")
                || candidate.endsWith("景区")
                || candidate.endsWith("公园")
                || candidate.endsWith("楼")
                || candidate.endsWith("馆")
                || candidate.endsWith("园")
                || candidate.endsWith("街")
                || candidate.endsWith("夜市")
                || candidate.endsWith("地方"));
    }

    private String extractRouteMode(String query, Map<String, Double> weights) {
        String text = normalize(query);
        if (containsAny(text, "最短", "最近", "路程短", "少绕路")) {
            return MODE_DISTANCE;
        }
        if (containsAny(text, "最快", "赶时间", "尽快", "节省时间", "快一点", "时间短")) {
            return MODE_DURATION;
        }
        if (containsAny(text, "个性化", "按喜好", "根据喜好", "根据我的偏好", "根据我的喜好", "智能推荐")) {
            return MODE_PERSONALIZED;
        }
        boolean hasPreference = weights.values().stream().anyMatch(v -> v > 0.0);
        return hasPreference ? MODE_PERSONALIZED : MODE_DURATION;
    }

    private int extractMaxStops(String query) {
        String text = normalize(query);
        if (containsAny(text, "所有景点", "所有景区", "全部景点", "全部景区", "全部地点", "所有地点", "全景点", "全景区", "都逛", "全都逛", "全部都玩", "全玩一遍")) {
            return ALL_SCENIC_STOPS;
        }
        if (text.contains("半日游")) {
            return 2;
        }
        if (text.contains("一日游")) {
            return 3;
        }
        if (text.contains("二日游") || text.contains("两日游")) {
            return 5;
        }
        if (text.contains("三日游")) {
            return 6;
        }
        if (containsAny(text, "更多景区", "更多景点", "更多地点", "更多地方")) {
            return 5;
        }
        Matcher matcher = Pattern.compile("(\\d+)个(?:景区|景点|地点)").matcher(text);
        if (matcher.find()) {
            return safeParseInt(matcher.group(1), 3);
        }
        if (text.contains("两个景区") || text.contains("两个景点")) {
            return 2;
        }
        if (text.contains("三个景区") || text.contains("三个景点")) {
            return 3;
        }
        if (text.contains("四个景区") || text.contains("四个景点")) {
            return 4;
        }
        return 3;
    }

    private int resolveMaxStops(String query, TourismIntentClassification modelIntent) {
        int deterministicStops = extractMaxStops(query);
        if (deterministicStops != 3) {
            return deterministicStops;
        }
        if (modelIntent != null && modelIntent.maxStops > 0) {
            return Math.max(1, Math.min(modelIntent.maxStops, 8));
        }
        return deterministicStops;
    }

    private List<SmallScenicSpot> loadSmallSpotsSafely() {
        try {
            return smallScenicSpotRepository == null ? Collections.emptyList() : smallScenicSpotRepository.findAll();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private Map<String, Double> extractPreferenceWeights(String query) {
        String text = normalize(query);
        Map<String, Double> weights = initWeightMap();

        addWeightIfContains(text, weights, 1.8, MODE_DURATION, "最快", "赶时间", "尽快", "节省时间", "快一点", "时间短");
        addWeightIfContains(text, weights, 1.5, MODE_DISTANCE, "最近", "最短", "路程短", "少绕路");
        addWeightIfContains(text, weights, 1.6, "cost", "省钱", "便宜", "预算低", "花费少", "少花钱", "经济");

        addWeightIfContains(text, weights, 1.7, "intensity", "少走路", "轻松", "不想太累", "别太累", "少爬", "不想爬山", "体力消耗低");
        addWeightIfContains(text, weights, 1.3, "comfort", "舒服", "舒适", "不要折腾", "平稳");
        addWeightIfContains(text, weights, 1.5, "crowd", "人少", "避开人群", "不要太挤", "别太拥挤", "清净");

        addWeightIfContains(text, weights, 1.7, "elderlyFriendly", "老人", "长辈", "爸妈", "老年人", "适合老人");
        if (weights.get("elderlyFriendly") > 0) {
            addWeight(weights, "intensity", 0.8);
            addWeight(weights, "comfort", 0.8);
            addWeight(weights, "restroomConvenience", 0.7);
        }

        addWeightIfContains(text, weights, 1.6, "familyFriendly", "亲子", "孩子", "小孩", "儿童", "带娃");
        if (weights.get("familyFriendly") > 0) {
            addWeight(weights, "restroomConvenience", 0.5);
            addWeight(weights, "leisure", 0.4);
        }

        addWeightIfContains(text, weights, 1.6, "nature", "自然", "风景", "山水", "湖", "瀑布", "森林", "海子", "景色");
        addWeightIfContains(text, weights, 1.6, "culture", "文化", "历史", "人文", "书院", "古迹", "讲学");
        addWeightIfContains(text, weights, 1.5, "photography", "拍照", "摄影", "出片", "打卡", "观景");
        addWeightIfContains(text, weights, 1.4, "leisure", "休闲", "悠闲", "慢慢逛", "放松", "休息");
        addWeightIfContains(text, weights, 1.3, "foodConvenience", "吃饭", "餐饮", "美食", "用餐");
        addWeightIfContains(text, weights, 1.3, "restroomConvenience", "卫生间", "厕所", "洗手间");
        addWeightIfContains(text, weights, 1.1, "popularity", "热门", "经典", "必去", "网红");

        boolean hasPreference = weights.values().stream().anyMatch(v -> v > 0.0);
        if (!hasPreference) {
            weights.put(MODE_DURATION, 0.8);
            weights.put(MODE_DISTANCE, 0.6);
        }
        return weights;
    }

    private Map<String, Double> initWeightMap() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put(MODE_DISTANCE, 0.0);
        weights.put(MODE_DURATION, 0.0);
        weights.put("cost", 0.0);
        weights.put("intensity", 0.0);
        weights.put("crowd", 0.0);
        weights.put("nature", 0.0);
        weights.put("culture", 0.0);
        weights.put("photography", 0.0);
        weights.put("elderlyFriendly", 0.0);
        weights.put("familyFriendly", 0.0);
        weights.put("leisure", 0.0);
        weights.put("comfort", 0.0);
        weights.put("foodConvenience", 0.0);
        weights.put("restroomConvenience", 0.0);
        weights.put("popularity", 0.0);
        return weights;
    }

    private void addWeightIfContains(String text, Map<String, Double> weights, double value, String key, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(normalize(keyword))) {
                addWeight(weights, key, value);
                break;
            }
        }
    }

    private void addWeight(Map<String, Double> weights, String key, double value) {
        weights.put(key, weights.getOrDefault(key, 0.0) + value);
    }

    private String buildSingleRouteAnswer(LargeScenicArea startArea,
                                          LargeScenicArea endArea,
                                          Map<String, Double> preferenceWeights,
                                          String routeMode,
                                          Map<String, Object> selectedPath,
                                          Map<String, Object> distancePath,
                                          Map<String, Object> timePath) {
        StringBuilder answer = new StringBuilder();
        answer.append("已为你规划好路线。\n\n");
        answer.append("起点：").append(startArea.getName()).append("\n");
        answer.append("终点：").append(endArea.getName()).append("\n\n");
        answer.append("推荐路线：\n");
        appendPathNodes(answer, selectedPath);
        appendSegmentDetails(answer, selectedPath);
        appendVisitDetails(answer, selectedPath);
        appendRouteSummary(answer, selectedPath);
        return answer.toString();
    }

    private String buildCityRouteAnswer(LargeScenicArea startArea,
                                        LargeScenicArea endArea,
                                        Map<String, Double> preferenceWeights,
                                        String routeMode,
                                        Map<String, Object> cityRoute,
                                        int maxStops) {
        StringBuilder answer = new StringBuilder();
        answer.append("已为你整理出一条游玩路线。\n\n");
        int arrangedCount = countRecommendedScenic(getRecommendedPathDetails(cityRoute));
        if (arrangedCount > 0) {
            answer.append("本次共安排").append(arrangedCount).append("个景区。\n");
        }
        if (startArea != null) {
            answer.append("起点偏好：").append(startArea.getName()).append("\n");
        }
        if (endArea != null) {
            answer.append("终点偏好：").append(endArea.getName()).append("\n");
        }
        answer.append("\n推荐游览顺序：\n");
        appendPathNodes(answer, cityRoute);
        appendSegmentDetails(answer, cityRoute);
        appendVisitDetails(answer, cityRoute);
        appendRouteSummary(answer, cityRoute);
        return answer.toString();
    }


    private void appendRouteSummary(StringBuilder answer, Map<String, Object> pathResult) {
        int visitDuration = (int) getDouble(pathResult, "totalVisitDuration");
        int insideTransitDuration = (int) getDouble(pathResult, "totalInsideTransitDuration");
        int routeDuration = (int) getDouble(pathResult, "totalDuration");
        int overallDuration = (int) getDouble(pathResult, "overallDuration");

        if (visitDuration > 0) {
            answer.append("景区内建议游玩时间：")
                    .append(visitDuration)
                    .append("分钟\n");
        }
        if (insideTransitDuration > 0) {
            answer.append("景区内步行时间：")
                    .append(insideTransitDuration)
                    .append("分钟\n");
        }
        answer.append("景区间交通时间：")
                .append(routeDuration)
                .append("分钟\n");
        answer.append("总时间：")
                .append(overallDuration)
                .append("分钟\n");

        double ticketCost = getDouble(pathResult, "totalTicketCost");
        double transportCost = getDouble(pathResult, "totalTransportCost");
        double overallCost = getDouble(pathResult, "overallCost");
        if (ticketCost > 0) {
            answer.append("景区门票价格：")
                    .append(String.format(Locale.ROOT, "%.2f", ticketCost))
                    .append("元\n");
        }
        if (transportCost > 0) {
            answer.append("交通花费：")
                    .append(String.format(Locale.ROOT, "%.2f", transportCost))
                    .append("元\n");
        }
        answer.append("总花费：")
                .append(String.format(Locale.ROOT, "%.2f", overallCost > 0 ? overallCost : getDouble(pathResult, "totalCost")))
                .append("元\n");

        double insideDistance = getDouble(pathResult, "totalInsideDistance");
        if (insideDistance > 0) {
            answer.append("景区内步行路程：")
                    .append(String.format(Locale.ROOT, "%.1f", insideDistance))
                    .append("米\n");
        }
        answer.append("总路程：")
                .append(String.format(Locale.ROOT, "%.1f", Math.max(getDouble(pathResult, "overallDistance"), getDouble(pathResult, "totalDistance"))))
                .append("米\n");
    }

    private void appendSegmentDetails(StringBuilder answer, Map<String, Object> pathResult) {
        List<Map<String, Object>> segmentDetails = getSegmentDetails(pathResult);
        if (segmentDetails.isEmpty()) {
            return;
        }
        answer.append("分段说明：\n");
        for (int i = 0; i < segmentDetails.size(); i++) {
            Map<String, Object> segment = segmentDetails.get(i);
            answer.append(i + 1)
                    .append(". ")
                    .append(segment.get("fromName"))
                    .append(" → ")
                    .append(segment.get("toName"))
                    .append("，方式：")
                    .append(transportModeLabel(String.valueOf(segment.get("transportMode"))))
                    .append("，距离：")
                    .append(String.format(Locale.ROOT, "%.2f", getNumber(segment.get("distance"))))
                    .append("米，时间：")
                    .append((int) getNumber(segment.get("duration")))
                    .append("分钟");
            Object costAmount = segment.get("costAmount");
            if (costAmount != null && getNumber(costAmount) > 0) {
                answer.append("，花费：")
                        .append(String.format(Locale.ROOT, "%.2f", getNumber(costAmount)))
                        .append("元");
            }
            if (segment.get("description") != null && !String.valueOf(segment.get("description")).trim().isEmpty()) {
                answer.append("（").append(segment.get("description")).append("）");
            }
            answer.append("\n");
        }
        answer.append("\n");
    }


    private void appendVisitDetails(StringBuilder answer, Map<String, Object> pathResult) {
        List<Map<String, Object>> visitDetails = getVisitDetails(pathResult);
        if (visitDetails.isEmpty()) {
            return;
        }
        answer.append("各景区建议游玩时间：\n");
        for (int i = 0; i < visitDetails.size(); i++) {
            Map<String, Object> detail = visitDetails.get(i);
            answer.append(i + 1)
                    .append(". ")
                    .append(detail.get("areaName"))
                    .append("：约")
                    .append((int) getNumber(detail.get("suggestedVisitDuration")))
                    .append("分钟");

            List<Map<String, Object>> spots = getRecommendedSpots(detail);
            if (!spots.isEmpty()) {
                answer.append("；园内可安排：");
                for (int j = 0; j < spots.size(); j++) {
                    Map<String, Object> spot = spots.get(j);
                    if (j > 0) {
                        answer.append("、");
                    }
                    answer.append(spot.get("name"))
                            .append("（")
                            .append((int) getNumber(spot.get("visitingDuration")))
                            .append("分钟）");
                }
            }
            double insideDistance = getNumber(detail.get("insideDistance"));
            int insideTransitDuration = (int) getNumber(detail.get("insideTransitDuration"));
            if (insideDistance > 0) {
                answer.append("；园内步行约")
                        .append(String.format(Locale.ROOT, "%.1f", insideDistance))
                        .append("米");
                if (insideTransitDuration > 0) {
                    answer.append("，约").append(insideTransitDuration).append("分钟");
                }
            }
            answer.append("\n");
        }
        answer.append("\n");
    }


    private void appendPathNodes(StringBuilder answer, Map<String, Object> pathResult) {
        List<Map<String, Object>> orderedNodes = getRecommendedPathDetails(pathResult);
        for (int i = 0; i < orderedNodes.size(); i++) {
            Map<String, Object> area = orderedNodes.get(i);
            answer.append(i + 1).append(". ").append(area.get("name"));
            Object isAreaType = area.get("isAreaType");
            if (isAreaType != null && "1".equals(String.valueOf(isAreaType))) {
                answer.append("（公共节点）");
            }
            answer.append("\n");
        }
        answer.append("\n");
    }

    private String buildPreferenceSummary(Map<String, Double> weights) {
        List<String> labels = new ArrayList<>();
        addLabelIfHigh(weights, labels, MODE_DISTANCE, "尽量走更短路线");
        addLabelIfHigh(weights, labels, MODE_DURATION, "更看重节省时间");
        addLabelIfHigh(weights, labels, "cost", "更看重节省花费");
        addLabelIfHigh(weights, labels, "intensity", "想少走路/少消耗体力");
        addLabelIfHigh(weights, labels, "crowd", "希望人少一些");
        addLabelIfHigh(weights, labels, "nature", "偏好自然风景");
        addLabelIfHigh(weights, labels, "culture", "偏好人文历史");
        addLabelIfHigh(weights, labels, "photography", "偏好拍照观景");
        addLabelIfHigh(weights, labels, "elderlyFriendly", "希望更适合老人");
        addLabelIfHigh(weights, labels, "familyFriendly", "希望更适合亲子");
        addLabelIfHigh(weights, labels, "leisure", "更偏好轻松休闲");
        addLabelIfHigh(weights, labels, "foodConvenience", "希望餐饮更方便");
        addLabelIfHigh(weights, labels, "restroomConvenience", "希望卫生间更方便");
        addLabelIfHigh(weights, labels, "comfort", "更看重舒适度");
        addLabelIfHigh(weights, labels, "popularity", "偏好热门经典点位");
        if (labels.isEmpty()) {
            return "未检测到明确偏好，默认按较高效率规划";
        }
        return String.join("、", labels);
    }

    private void addLabelIfHigh(Map<String, Double> weights, List<String> labels, String key, String label) {
        if (weights.getOrDefault(key, 0.0) >= 1.0) {
            labels.add(label);
        }
    }

    private String routeModeLabel(String routeMode) {
        if (MODE_DISTANCE.equals(routeMode)) {
            return "路程更短";
        }
        if (MODE_DURATION.equals(routeMode)) {
            return "节省时间";
        }
        return "结合你的需求";
    }

    private String transportModeLabel(String transportMode) {
        String mode = normalize(transportMode).toUpperCase(Locale.ROOT);
        if ("WALK".equals(mode)) {
            return "步行";
        }
        if ("SHUTTLE".equals(mode)) {
            return "接驳车";
        }
        if ("CABLEWAY".equals(mode)) {
            return "索道";
        }
        if ("DRIVE".equals(mode)) {
            return "驾车";
        }
        if ("ROAD".equals(mode)) {
            return "道路通行";
        }
        return defaultText(transportMode);
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isBlank() || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String buildAreaPriceText(LargeScenicArea area) {
        BigDecimal price = area == null ? BigDecimal.ZERO : area.getPrice();
        BigDecimal safePrice = price == null ? BigDecimal.ZERO : price;

        if (isFoodPlace(area)) {
            return "人均消费参考：" + safeDecimal(safePrice) + "元";
        }

        if (isOpenConsumptionNode(area)) {
            if (safePrice.compareTo(BigDecimal.ZERO) > 0) {
                return "免费开放，可按需消费";
            }
            return "免费开放";
        }

        if (safeInt(area == null ? null : area.getIsAreaType()) == 1) {
            if (safePrice.compareTo(BigDecimal.ZERO) > 0) {
                return "费用参考：" + safeDecimal(safePrice) + "元";
            }
            return "免费开放";
        }

        return "门票参考：" + safeDecimal(safePrice) + "元";
    }

    private boolean isFoodPlace(LargeScenicArea area) {
        String combined = buildAreaKeywordText(area);
        return containsAny(combined,
                "饭店", "饭馆", "餐厅", "餐馆", "酒楼", "老店", "小吃", "美食", "豫菜", "灌汤包", "锅贴", "桶子鸡", "熟食", "正餐", "轻餐");
    }

    private boolean isOpenConsumptionNode(LargeScenicArea area) {
        String combined = buildAreaKeywordText(area);
        return containsAny(combined,
                "夜市", "美食街", "步行街", "广场", "商圈", "街区", "游客中心", "大门", "车站", "火车站", "高铁站", "公交枢纽");
    }

    private String buildAreaKeywordText(LargeScenicArea area) {
        if (area == null) {
            return "";
        }
        return (defaultText(area.getName()) + " " + defaultText(area.getDescription()) + " " + defaultText(area.getTags()))
                .toLowerCase(Locale.ROOT);
    }
    private String sanitizeAiAnswer(String text) {
        if (text == null || text.isBlank()) {
            return "抱歉，暂时没有生成合适的回答，请稍后再试。";
        }

        String cleaned = text;
        cleaned = cleaned.replaceAll("(?m)^\\s*#{1,6}\\s*", "");
        cleaned = cleaned.replaceAll("(?m)^\\s*[-*+]\\s+", "");
        cleaned = cleaned.replaceAll("(?m)^\\s*>\\s*", "");
        cleaned = cleaned.replaceAll("(?m)^\\s*-{3,}\\s*$", "");
        cleaned = cleaned.replace("**", "");
        cleaned = cleaned.replace("*", "");
        cleaned = cleaned.replace("```", "");
        cleaned = cleaned.replace("`", "");

        cleaned = cleaned.replaceAll("（\\s*ID\\s*[:：]?\\s*\\d+\\s*）", "");
        cleaned = cleaned.replaceAll("\\(\\s*ID\\s*[:：]?\\s*\\d+\\s*\\)", "");
        cleaned = cleaned.replaceAll("\\bID\\s*[:：]?\\s*\\d+\\b", "");
        cleaned = cleaned.replaceAll("所属大景区ID\\s*[:：]?\\s*\\d+", "");
        cleaned = cleaned.replaceAll("(?i)(?<![a-z])meal\\s*price(?![a-z])", "人均消费参考");
        cleaned = cleaned.replaceAll("(?i)(?<![a-z])ticket\\s*price(?![a-z])", "门票参考");
        cleaned = cleaned.replaceAll("(?i)(?<![a-z])price(?![a-z])", "价格参考");
        cleaned = cleaned.replaceAll("(?<![a-zA-Z])WALK(?![a-zA-Z])", "步行");
        cleaned = cleaned.replaceAll("(?<![a-zA-Z])ROAD(?![a-zA-Z])", "道路通行");
        cleaned = cleaned.replaceAll("(?<![a-zA-Z])DRIVE(?![a-zA-Z])", "驾车");
        cleaned = cleaned.replaceAll("(?<![a-zA-Z])SHUTTLE(?![a-zA-Z])", "接驳车");
        cleaned = cleaned.replaceAll("(?<![a-zA-Z])CABLEWAY(?![a-zA-Z])", "索道");
        cleaned = cleaned.replaceAll("\\bpathDetails\\b", "");
        cleaned = cleaned.replaceAll("\\brecommendedAreaIds\\b", "");
        cleaned = cleaned.replaceAll("\\bsegmentDetails\\b", "");
        cleaned = cleaned.replaceAll("\\bvisitDetails\\b", "");
        cleaned = cleaned.replaceAll("\\bisAreaType\\b", "");
        cleaned = cleaned.replaceAll("\\btransportMode\\b", "");

        cleaned = cleaned.replaceAll("[ \\t]{2,}", " ");
        cleaned = cleaned.replaceAll("，{2,}", "，");
        cleaned = cleaned.replaceAll("：{2,}", "：");
        cleaned = cleaned.replaceAll("\\n[ \\t]*\\n[ \\t]*\\n+", "\\n\\n");
        cleaned = cleaned.replaceAll("(?m)^\\s*[（(][^\\n]*[）)]\\s*$", "");
        cleaned = cleaned.replaceAll("\\n{3,}", "\\n\\n");
        cleaned = normalizeAwkwardGeneratedText(cleaned);
        return cleaned.trim();
    }

    private String normalizeAwkwardGeneratedText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = text;

        cleaned = cleaned.replaceAll("虽然([^。；！？]{2,80}?)，(?:相关景点|园内景观|景区内景观|公共区域|码头区域|桥梁景观|入口)。", "这里$1。");
        cleaned = cleaned.replaceAll("如果需要进一步的([^，。；！？]{1,20})，(?:相关景点|园内景观|景区内景观|公共区域|码头区域|桥梁景观|入口)。", "如果你想继续了解$1，我也可以继续为你介绍。");
        cleaned = cleaned.replaceAll("如果需要进一步的([^。；！？]{1,20})。", "如果你想继续了解$1，我也可以继续为你介绍。");

        cleaned = cleaned.replaceAll("([，；。！？])\\s*(?:相关景点|园内景观|景区内景观|公共区域|码头区域|桥梁景观)\\s*([，；。！？])", "$1");
        cleaned = cleaned.replaceAll("，\\s*(?:相关景点|园内景观|景区内景观|公共区域|码头区域|桥梁景观|入口)。(?=$|\\n)", "。");
        cleaned = cleaned.replaceAll("^\\s*(?:相关景点|园内景观|景区内景观|公共区域|码头区域|桥梁景观|入口)[，。；！？]?", "");

        cleaned = cleaned.replaceAll("，{2,}", "，");
        cleaned = cleaned.replaceAll("。{2,}", "。");
        cleaned = cleaned.replaceAll("，。", "。");
        cleaned = cleaned.replaceAll("。；", "。");
        cleaned = cleaned.replaceAll("[ \\t]{2,}", " ");
        cleaned = cleaned.replaceAll("\\n[ \\t]*\\n[ \\t]*\\n+", "\\n\\n");
        return cleaned.trim();
    }



    private String sanitizeAndValidateGeneratedAnswer(String text,
                                                      String query,
                                                      List<LargeScenicArea> largeAreas,
                                                      List<SmallScenicSpot> smallSpots,
                                                      String context,
                                                      Set<String> allowedNames) {
        String cleaned = sanitizeAiAnswer(text);
        cleaned = enforcePureChineseAndWhitelist(cleaned, allowedNames);

        ValidationSummary validation = validateGeneratedAnswer(cleaned, allowedNames);
        if (validation.hasIssues()) {
            String rewritten = rewriteAnswerWithWhitelist(query, context, cleaned, allowedNames, validation);
            cleaned = sanitizeAiAnswer(rewritten);
            cleaned = enforcePureChineseAndWhitelist(cleaned, allowedNames);
            validation = validateGeneratedAnswer(cleaned, allowedNames);
        }

        if (validation.hasIssues()) {
            cleaned = stripUnknownScenicCandidates(cleaned, allowedNames);
            cleaned = enforcePureChineseAndWhitelist(cleaned, allowedNames);
            validation = validateGeneratedAnswer(cleaned, allowedNames);
        }

        if (cleaned.isBlank() || validation.hasIssues()) {
            return buildKnowledgeOnlyFallbackAnswer(query, largeAreas, smallSpots);
        }
        return cleaned;
    }

    private String polishFactAnswer(String query, String factAnswer, Set<String> allowedNames) {
        String fallback = sanitizeAiAnswer(factAnswer);
        if (chatModel == null || fallback.isBlank()) {
            return fallback;
        }

        try {
            String polished = chatModel.generate(buildFactPolishPrompt(query, fallback, allowedNames));
            String cleaned = sanitizeAiAnswer(polished);
            ValidationSummary validation = validateGeneratedAnswer(cleaned, allowedNames);
            if (cleaned.isBlank() || validation.hasIssues()) {
                return fallback;
            }

            cleaned = enforcePureChineseAndWhitelist(cleaned, allowedNames);
            validation = validateGeneratedAnswer(cleaned, allowedNames);
            if (cleaned.isBlank() || validation.hasIssues()) {
                return fallback;
            }
            if (!preservesFactAnswer(fallback, cleaned, allowedNames)) {
                return fallback;
            }
            return cleaned;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String buildFactPolishPrompt(String query, String factAnswer, Set<String> allowedNames) {
        return "你是一名中文导游。下面的事实答案已经由系统根据数据库生成，你只能做表达润色。\n\n"
                + "硬性要求：\n"
                + "1. 必须保留事实答案里的全部景点名、点位名、价格、开放时间、游览时长和先后顺序。\n"
                + "2. 禁止新增任何景点、建筑、路线、套餐、价格、开放时间或历史传说。\n"
                + "3. 如果事实答案里说没有查到或没有录入，必须保留这个意思。\n"
                + "4. 只能输出简体中文自然段，不要使用 Markdown，不要提到数据库、接口、字段或内部编号。\n"
                + "5. 如果事实答案是偏好推荐，必须保留“以下是一些推荐”这类开头，并保留每个景区“作为以……为特色”的推荐理由。\n"
                + "6. 允许出现的真实名称只有：" + buildAllowedNameSummary(allowedNames) + "\n\n"
                + "游客问题：\n" + defaultText(query) + "\n\n"
                + "事实答案：\n" + factAnswer;
    }

    private boolean preservesFactAnswer(String factAnswer, String polishedAnswer, Set<String> allowedNames) {
        if (factAnswer == null || factAnswer.isBlank() || polishedAnswer == null || polishedAnswer.isBlank()) {
            return false;
        }
        if (!preservesAllowedNamesInOrder(factAnswer, polishedAnswer, allowedNames)) {
            return false;
        }
        if (!extractNumberUnitFacts(polishedAnswer).containsAll(extractNumberUnitFacts(factAnswer))) {
            return false;
        }
        for (String phrase : List.of("开封市并没有这个景点", "没有录入", "暂无信息", "暂时没有查到", "免费开放", "可按需消费")) {
            if (factAnswer.contains(phrase) && !polishedAnswer.contains(phrase)) {
                return false;
            }
        }
        for (String phrase : List.of("以下是一些推荐", "作为以")) {
            if (factAnswer.contains(phrase) && !polishedAnswer.contains(phrase)) {
                return false;
            }
        }
        return polishedAnswer.length() <= factAnswer.length() * 2 + 80;
    }

    private boolean preservesAllowedNamesInOrder(String factAnswer, String polishedAnswer, Set<String> allowedNames) {
        if (allowedNames == null || allowedNames.isEmpty()) {
            return true;
        }
        List<NameHit> orderedNames = new ArrayList<>();
        String normalizedFactAnswer = normalize(factAnswer);
        Set<String> seenNormalizedNames = new LinkedHashSet<>();
        for (String name : allowedNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String normalizedName = normalize(name);
            int index = normalizedFactAnswer.indexOf(normalizedName);
            if (index >= 0 && seenNormalizedNames.add(normalizedName)) {
                orderedNames.add(new NameHit(name, index, normalizedName.length()));
            }
        }
        orderedNames.sort(Comparator.comparingInt(NameHit::getIndex)
                .thenComparing((a, b) -> Integer.compare(b.getLength(), a.getLength())));

        int lastIndex = -1;
        for (NameHit hit : orderedNames) {
            String name = hit.getName();
            int currentIndex = polishedAnswer.indexOf(name);
            if (currentIndex < 0 || currentIndex < lastIndex) {
                return false;
            }
            lastIndex = currentIndex;
        }
        return true;
    }

    private Set<String> extractNumberUnitFacts(String text) {
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return facts;
        }
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(元|分钟|米|公里|小时)").matcher(text);
        while (matcher.find()) {
            facts.add(canonicalNumber(matcher.group(1)) + matcher.group(2));
        }
        return facts;
    }

    private String canonicalNumber(String value) {
        try {
            return new BigDecimal(value).stripTrailingZeros().toPlainString();
        } catch (Exception ignored) {
            return value;
        }
    }

    private LinkedHashSet<String> buildAllowedScenicNameWhitelist(List<LargeScenicArea> largeAreas,
                                                                  List<SmallScenicSpot> smallSpots) {
        LinkedHashSet<String> allowedNames = new LinkedHashSet<>();
        if (largeAreas != null) {
            for (LargeScenicArea area : largeAreas) {
                if (area != null && area.getName() != null && !area.getName().isBlank()) {
                    allowedNames.add(area.getName().trim());
                }
            }
        }
        if (smallSpots != null) {
            for (SmallScenicSpot spot : smallSpots) {
                if (spot != null && spot.getName() != null && !spot.getName().isBlank()) {
                    allowedNames.add(spot.getName().trim());
                }
            }
        }
        return allowedNames;
    }

    private String buildAllowedNameSummary(Set<String> allowedNames) {
        if (allowedNames == null || allowedNames.isEmpty()) {
            return "当前没有可用景点名称";
        }
        StringJoiner joiner = new StringJoiner("、");
        for (String name : allowedNames) {
            if (name != null && !name.isBlank()) {
                joiner.add(name.trim());
            }
        }
        return joiner.toString();
    }

    private String enforcePureChineseAndWhitelist(String text, Set<String> allowedNames) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = text;
        cleaned = removeIllegalEnglishTokens(cleaned, allowedNames);
        cleaned = stripUnknownScenicCandidates(cleaned, allowedNames);
        cleaned = cleaned.replaceAll("[ \t]{2,}", " ");
        cleaned = cleaned.replaceAll("（\\s*）", "");
        cleaned = cleaned.replaceAll("\\(\\s*\\)", "");
        cleaned = cleaned.replaceAll("，\\s*，", "，");
        cleaned = cleaned.replaceAll("。\\s*。", "。");
        cleaned = cleaned.replaceAll("\n[ \t]*\n[ \t]*\n+", "\n\n");
        cleaned = normalizeAwkwardGeneratedText(cleaned);
        return cleaned.trim();
    }

    private String removeIllegalEnglishTokens(String text, Set<String> allowedNames) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Matcher matcher = Pattern.compile("[A-Za-z]{2,}").matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group();
            if (isAllowedEnglishToken(token, allowedNames)) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(token));
            } else {
                matcher.appendReplacement(buffer, "");
            }
        }
        matcher.appendTail(buffer);
        String cleaned = buffer.toString();
        cleaned = cleaned.replaceAll("(?<=[\\u4e00-\\u9fa5])\\s+(?=[\\u4e00-\\u9fa5])", "");
        cleaned = cleaned.replaceAll("(?<=[\\u4e00-\\u9fa5])\\s+(?=[，。；：！？])", "");
        cleaned = cleaned.replaceAll("(?<=[（(])\\s+", "");
        cleaned = cleaned.replaceAll("\\s+(?=[）)])", "");
        return cleaned;
    }

    private boolean isAllowedEnglishToken(String token, Set<String> allowedNames) {
        if (token == null || token.isBlank() || allowedNames == null || allowedNames.isEmpty()) {
            return false;
        }
        for (String name : allowedNames) {
            if (name != null && name.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private ValidationSummary validateGeneratedAnswer(String text, Set<String> allowedNames) {
        ValidationSummary summary = new ValidationSummary();
        if (text == null || text.isBlank()) {
            summary.unknownScenicCandidates.add("回答为空");
            return summary;
        }

        Matcher englishMatcher = Pattern.compile("[A-Za-z]{2,}").matcher(text);
        while (englishMatcher.find()) {
            String token = englishMatcher.group();
            if (!isAllowedEnglishToken(token, allowedNames)) {
                summary.illegalEnglishTokens.add(token);
            }
        }

        Matcher scenicMatcher = SCENIC_NAME_PATTERN.matcher(text);
        while (scenicMatcher.find()) {
            String candidate = scenicMatcher.group();
            if (!isGenericScenicPhrase(candidate) && !isWhitelistedScenicName(candidate, allowedNames)) {
                summary.unknownScenicCandidates.add(candidate);
            }
        }
        return summary;
    }

    private boolean isWhitelistedScenicName(String candidate, Set<String> allowedNames) {
        if (candidate == null || candidate.isBlank() || allowedNames == null || allowedNames.isEmpty()) {
            return false;
        }
        String normalizedCandidate = normalize(candidate);
        for (String name : allowedNames) {
            String normalizedName = normalize(name);
            if (normalizedCandidate.equals(normalizedName)
                    || normalizedCandidate.contains(normalizedName)
                    || normalizedName.contains(normalizedCandidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGenericScenicPhrase(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return true;
        }
        String normalizedCandidate = normalize(candidate);
        if (containsAny(normalizedCandidate,
                "没有这个景点", "没有这个景区", "未录入这个景点", "未录入这个景区",
                "没有录入这个景点", "没有录入这个景区", "没有查到这个景点", "没有查到这个景区")) {
            return true;
        }
        return candidate.length() <= 2
                || "景区".equals(candidate)
                || "景点".equals(candidate)
                || "公园".equals(candidate)
                || "这个景点".equals(candidate)
                || "这个景区".equals(candidate)
                || "这个点位".equals(candidate)
                || "这个地方".equals(candidate)
                || "该景点".equals(candidate)
                || "该景区".equals(candidate)
                || "该点位".equals(candidate)
                || "该地方".equals(candidate)
                || "大门".equals(candidate)
                || "入口".equals(candidate)
                || "门口".equals(candidate)
                || "游客中心".equals(candidate)
                || "夜市".equals(candidate)
                || "车站".equals(candidate)
                || "园内景观".equals(candidate)
                || "景区内景观".equals(candidate)
                || "相关景点".equals(candidate)
                || "码头区域".equals(candidate)
                || "桥梁景观".equals(candidate)
                || "公共区域".equals(candidate);
    }

    private String stripUnknownScenicCandidates(String text, Set<String> allowedNames) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Matcher matcher = SCENIC_NAME_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String candidate = matcher.group();
            if (isGenericScenicPhrase(candidate) || isWhitelistedScenicName(candidate, allowedNames)) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(candidate));
            } else {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(buildSafeReplacementForUnknownCandidate(candidate)));
            }
        }
        matcher.appendTail(buffer);
        String cleaned = buffer.toString();
        cleaned = cleaned.replaceAll("以园内景观闻名", "景色较有特色");
        cleaned = cleaned.replaceAll("以景区内景观闻名", "景色较有特色");
        cleaned = cleaned.replaceAll("以相关景点闻名", "有一定游览价值");
        cleaned = cleaned.replaceAll("以桥梁景观闻名", "桥梁景观较有特色");
        return cleaned;
    }

    private String buildSafeReplacementForUnknownCandidate(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return "景区内景观";
        }
        if (candidate.endsWith("门") || candidate.contains("游客中心") || candidate.endsWith("中心")) {
            return "入口";
        }
        if (candidate.endsWith("桥")) {
            return "桥梁景观";
        }
        if (candidate.endsWith("码头")) {
            return "码头区域";
        }
        if (candidate.endsWith("夜市") || candidate.endsWith("街") || candidate.endsWith("站")) {
            return "公共区域";
        }
        if (candidate.endsWith("林") || candidate.endsWith("树林") || candidate.endsWith("园")) {
            return "园内景观";
        }
        return "景区内景观";
    }

    private String rewriteAnswerWithWhitelist(String query,
                                              String context,
                                              String previousAnswer,
                                              Set<String> allowedNames,
                                              ValidationSummary validation) {
        if (chatModel == null) {
            return previousAnswer;
        }
        String rewritePrompt = context +
                "允许出现的真实景点名称：" + buildAllowedNameSummary(allowedNames) +
                "\n\n游客问题：" + query +
                "\n\n上一版回答：" + previousAnswer +
                "\n\n发现的问题：" + buildValidationSummaryText(validation) +
                "\n\n请重新生成答案，并严格遵守以下要求：\n" +
                "1. 只能使用知识库中已经出现过的真实景点名称，不得新增任何名称\n" +
                "2. 不得出现英文、拼音、外文别名或中英混写\n" +
                "3. 若用户提到的景点不在已知景点范围内，就明确回答“开封市并没有这个景点”；若只是缺少相关细节，再说明“暂时没有查到这方面的介绍”\n" +
                "4. 不得输出任何内部字段、内部编号或程序术语\n" +
                "5. 只输出自然中文段落，不要使用 Markdown 或项目符号";
        return chatModel.generate(rewritePrompt);
    }

    private String buildValidationSummaryText(ValidationSummary validation) {
        if (validation == null || !validation.hasIssues()) {
            return "未发现问题";
        }
        StringBuilder message = new StringBuilder();
        if (!validation.illegalEnglishTokens.isEmpty()) {
            message.append("存在英文或拼音片段：").append(String.join("、", validation.illegalEnglishTokens)).append("。 ");
        }
        if (!validation.unknownScenicCandidates.isEmpty()) {
            message.append("存在不在白名单中的景点或设施名称：").append(String.join("、", validation.unknownScenicCandidates)).append("。 ");
        }
        return message.toString().trim();
    }

    private String buildKnowledgeOnlyFallbackAnswer(String query,
                                                    List<LargeScenicArea> largeAreas,
                                                    List<SmallScenicSpot> smallSpots) {
        if ((largeAreas == null || largeAreas.isEmpty()) && (smallSpots == null || smallSpots.isEmpty())) {
            return "目前还没有查到可用于介绍的景点信息。";
        }

        List<LargeScenicArea> safeLargeAreas = largeAreas == null ? Collections.emptyList() : largeAreas;
        List<SmallScenicSpot> safeSmallSpots = smallSpots == null ? Collections.emptyList() : smallSpots;
        LargeScenicArea targetArea = findAreaByName(query, safeLargeAreas);
        SmallScenicSpot targetSpot = findSpotByName(query, safeSmallSpots);
        StringBuilder answer = new StringBuilder();

        if (targetArea != null) {
            answer.append(targetArea.getName()).append("是开封的真实景点。");
            String description = defaultText(targetArea.getDescription());
            if (!"暂无信息".equals(description)) {
                answer.append(description).append("。");
            }
            if (targetArea.getOpeningHours() != null && !targetArea.getOpeningHours().isBlank()) {
                answer.append("开放时间可参考：").append(targetArea.getOpeningHours()).append("。");
            }
            answer.append(buildAreaPriceText(targetArea)).append("。");

            List<SmallScenicSpot> areaSpots = new ArrayList<>();
            for (SmallScenicSpot spot : safeSmallSpots) {
                if (spot != null && Objects.equals(targetArea.getId(), spot.getLargeAreaId()) && safeInt(spot.getIsSpotType()) == 0) {
                    areaSpots.add(spot);
                }
            }
            areaSpots.sort(Comparator.comparingInt((SmallScenicSpot spot) -> safeInt(spot.getVisitingDuration())).reversed());
            if (!areaSpots.isEmpty()) {
                answer.append("园内可重点关注：");
                for (int i = 0; i < Math.min(3, areaSpots.size()); i++) {
                    if (i > 0) {
                        answer.append("、");
                    }
                    answer.append(areaSpots.get(i).getName());
                }
                answer.append("。");
            }
            return sanitizeAiAnswer(answer.toString());
        }

        if (targetSpot != null) {
            answer.append(targetSpot.getName()).append("是景区内的真实点位。");
            String description = defaultText(targetSpot.getDescription());
            if (!"暂无信息".equals(description)) {
                answer.append(description).append("。");
            }
            LargeScenicArea parentArea = null;
            for (LargeScenicArea area : safeLargeAreas) {
                if (area != null && Objects.equals(area.getId(), targetSpot.getLargeAreaId())) {
                    parentArea = area;
                    break;
                }
            }
            if (parentArea != null) {
                answer.append("它属于").append(parentArea.getName()).append("。");
            }
            if (safeInt(targetSpot.getIsSpotType()) == 1) {
                answer.append("该点位更适合作为入园节点、集合点或路线起终点。");
            } else if (safeInt(targetSpot.getVisitingDuration()) > 0) {
                answer.append("建议停留约").append(safeInt(targetSpot.getVisitingDuration())).append("分钟。");
            }
            return sanitizeAiAnswer(answer.toString());
        }

        if (isLikelySpecificScenicQuery(query)) {
            return "开封市并没有这个景点。";
        }

        List<LargeScenicArea> sortedAreas = new ArrayList<>();
        for (LargeScenicArea area : safeLargeAreas) {
            if (area != null && safeInt(area.getIsAreaType()) == 0) {
                sortedAreas.add(area);
            }
        }
        if (sortedAreas.isEmpty()) {
            sortedAreas = new ArrayList<>(safeLargeAreas);
        }
        sortedAreas.sort(Comparator
                .comparingDouble((LargeScenicArea area) -> safeDecimal(area.getPopularityScore())).reversed()
                .thenComparingInt(area -> safeInt(area.getRecommendedVisitDuration())).reversed());

        for (int i = 0; i < Math.min(4, sortedAreas.size()); i++) {
            LargeScenicArea area = sortedAreas.get(i);
            if (i > 0) {
                answer.append("；");
            }
            answer.append(area.getName());
            String description = defaultText(area.getDescription());
            if (!"暂无信息".equals(description)) {
                answer.append("，").append(trimToSentence(description));
            }
        }
        answer.append("。");
        return sanitizeAiAnswer(answer.toString());
    }

    private String trimToSentence(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = text.replaceAll("[\r\n]+", " ").trim();
        int idx = cleaned.indexOf("。");
        if (idx > 0) {
            return cleaned.substring(0, idx);
        }
        return cleaned.length() > 40 ? cleaned.substring(0, 40) + "..." : cleaned;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getRecommendedPathDetails(Map<String, Object> result) {
        Object recommendedIdsObj = result.get("recommendedAreaIds");
        List<Map<String, Object>> pathDetails = getPathDetails(result);
        if (!(recommendedIdsObj instanceof List) || pathDetails.isEmpty()) {
            return deduplicatePathDetails(pathDetails);
        }

        Map<Long, Map<String, Object>> detailMap = new LinkedHashMap<>();
        for (Map<String, Object> detail : pathDetails) {
            Long id = toLong(detail.get("id"));
            if (id != null && !detailMap.containsKey(id)) {
                detailMap.put(id, detail);
            }
        }

        List<Map<String, Object>> ordered = new ArrayList<>();
        for (Object idObj : (List<?>) recommendedIdsObj) {
            Long id = toLong(idObj);
            Map<String, Object> detail = id == null ? null : detailMap.get(id);
            if (detail != null) {
                ordered.add(detail);
            }
        }
        return ordered.isEmpty() ? deduplicatePathDetails(pathDetails) : ordered;
    }

    private List<Map<String, Object>> deduplicatePathDetails(List<Map<String, Object>> pathDetails) {
        List<Map<String, Object>> ordered = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (Map<String, Object> detail : pathDetails) {
            Long id = toLong(detail.get("id"));
            if (id == null || seen.add(id)) {
                ordered.add(detail);
            }
        }
        return ordered;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getPathDetails(Map<String, Object> result) {
        Object pathDetails = result.get("pathDetails");
        return pathDetails instanceof List ? (List<Map<String, Object>>) pathDetails : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSegmentDetails(Map<String, Object> result) {
        Object segmentDetails = result.get("segmentDetails");
        return segmentDetails instanceof List ? (List<Map<String, Object>>) segmentDetails : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getVisitDetails(Map<String, Object> result) {
        Object visitDetails = result.get("visitDetails");
        return visitDetails instanceof List ? (List<Map<String, Object>>) visitDetails : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getRecommendedSpots(Map<String, Object> detail) {
        Object spots = detail.get("recommendedSpots");
        return spots instanceof List ? (List<Map<String, Object>>) spots : Collections.emptyList();
    }


    private int countRecommendedScenic(List<Map<String, Object>> pathDetails) {
        int count = 0;
        for (Map<String, Object> detail : pathDetails) {
            Object isAreaType = detail.get("isAreaType");
            if (isAreaType == null || !"1".equals(String.valueOf(isAreaType))) {
                count++;
            }
        }
        return count;
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

    private double getDouble(Map<String, Object> result, String key) {
        return getNumber(result.get(key));
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

    private String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "")
                .replace("“", "")
                .replace("”", "")
                .replace("\"", "");
    }

    private String defaultText(String value) {
        return value == null || value.trim().isEmpty() ? "暂无信息" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDecimal(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private enum TourismIntentType {
        ROUTE_PLAN,
        SCENIC_DETAIL,
        PACKAGE_RECOMMENDATION,
        FOOD_RECOMMENDATION,
        GENERAL_SCENIC_LIST,
        UNSUPPORTED,
        UNKNOWN
    }

    private static class TourismIntentClassification {
        private final TourismIntentType intentType;
        private final String scenicName;
        private final int maxStops;

        private TourismIntentClassification(TourismIntentType intentType, String scenicName, int maxStops) {
            this.intentType = intentType == null ? TourismIntentType.UNKNOWN : intentType;
            this.scenicName = scenicName == null ? "" : scenicName.trim();
            this.maxStops = maxStops;
        }

        private static TourismIntentClassification unknown() {
            return new TourismIntentClassification(TourismIntentType.UNKNOWN, "", 0);
        }

        private boolean is(TourismIntentType type) {
            return intentType == type;
        }
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

    private static class ValidationSummary {
        private final LinkedHashSet<String> illegalEnglishTokens = new LinkedHashSet<>();
        private final LinkedHashSet<String> unknownScenicCandidates = new LinkedHashSet<>();

        private boolean hasIssues() {
            return !illegalEnglishTokens.isEmpty() || !unknownScenicCandidates.isEmpty();
        }
    }

    private static class NameHit {
        private final String name;
        private final int index;
        private final int length;

        private NameHit(String name, int index, int length) {
            this.name = name;
            this.index = index;
            this.length = length;
        }

        public String getName() {
            return name;
        }

        public int getIndex() {
            return index;
        }

        public int getLength() {
            return length;
        }
    }
}
