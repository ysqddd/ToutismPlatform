package org.example.toutismplatform.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.entity.SmallScenicSpot;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
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
    private static final Map<String, Map<String, Object>> PENDING_CART_CONTEXT_BY_USER = new ConcurrentHashMap<>();
    private static final ThreadLocal<Long> EXPLICIT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> EXPLICIT_USERNAME = new ThreadLocal<>();

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @Autowired
    private LargeScenicAreaRepository largeScenicAreaRepository;

    @Autowired
    private SmallScenicSpotRepository smallScenicSpotRepository;

    @Autowired
    private PathService pathService;

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

        if (isPathPlanningQuery(query)) {
            Map<String, Object> routeCartContext = buildRouteCartContext(query);
            rememberPendingCartContext(currentUserKey, routeCartContext);
            return sanitizeAiAnswer(String.valueOf(routeCartContext.getOrDefault("answer", "暂时无法生成路线。")));
        }

        List<LargeScenicArea> largeAreas = largeScenicAreaRepository.findAll();
        List<SmallScenicSpot> smallSpots = smallScenicSpotRepository.findAll();

        if (isGeneralScenicListQuery(query)) {
            Map<String, Object> recommendationContext = buildGeneralRecommendationCartContext(query, largeAreas);
            rememberPendingCartContext(currentUserKey, recommendationContext);
            return sanitizeAiAnswer(String.valueOf(recommendationContext.getOrDefault("answer", buildGeneralScenicListAnswer(query, largeAreas))));
        }
        Map<Long, String> areaNameMap = new HashMap<>();
        for (LargeScenicArea area : largeAreas) {
            if (area != null && area.getId() != null) {
                areaNameMap.put(area.getId(), area.getName());
            }
        }
        LinkedHashSet<String> allowedNames = buildAllowedScenicNameWhitelist(largeAreas, smallSpots);

        StringBuilder context = new StringBuilder();
        context.append("你是一名中文导游，负责回答游客关于景点的问题。\n\n");
        context.append("说明：以下内容必须严格依据现有景点资料回答；如果用户提到的景点并不存在，必须明确回答“开封市并没有这个景点”；如果只是缺少某个细节，就说明“暂时没有查到这方面的介绍”，不得猜测。\n\n");
        context.append("景点知识库：\n");
        context.append("重要提示：所有收费景区都需要通过免费的公共大门或游客中心进入，这些公共节点不收门票。\n");
        context.append("重要提示：以下信息里如果存在系统内部字段，只能用于理解，不允许在回答中直接输出。\n");
        context.append("重要提示：只允许使用知识库中真实存在的大景区名称和小景点名称，禁止编造别名、英文名、传说名称或不存在的景点。\n\n");

        for (LargeScenicArea area : largeAreas) {
            context.append("大景区/地点：").append(area.getName()).append("\n");
            context.append("描述：").append(defaultText(area.getDescription())).append("\n");
            context.append("位置：").append(defaultText(area.getLocation())).append("\n");
            context.append("开放时间：").append(defaultText(area.getOpeningHours())).append("\n");
            context.append("建议游览时长：").append(safeInt(area.getRecommendedVisitDuration())).append("分钟\n");
            context.append(buildAreaPriceText(area)).append("\n");
            context.append("标签：").append(defaultText(area.getTags())).append("\n");
            context.append("偏好特征：自然=").append(safeDecimal(area.getNatureScore()))
                    .append("，人文=").append(safeDecimal(area.getCultureScore()))
                    .append("，拍照=").append(safeDecimal(area.getPhotographyScore()))
                    .append("，老人友好=").append(safeDecimal(area.getElderlyFriendlyScore()))
                    .append("，亲子=").append(safeDecimal(area.getFamilyFriendlyScore()))
                    .append("，休闲=").append(safeDecimal(area.getLeisureScore()))
                    .append("，餐饮便利=").append(safeDecimal(area.getFoodConvenienceScore()))
                    .append("，卫生间便利=").append(safeDecimal(area.getRestroomConvenienceScore()))
                    .append("，热门=").append(safeDecimal(area.getPopularityScore()))
                    .append("，体力消耗等级=").append(safeInt(area.getIntensityLevel()))
                    .append("，拥挤等级=").append(safeInt(area.getCrowdLevel()))
                    .append("\n\n");
        }

        for (SmallScenicSpot spot : smallSpots) {
            context.append("小景点/设施：").append(spot.getName()).append("\n");
            String parentAreaName = areaNameMap.get(spot.getLargeAreaId());
            if (parentAreaName != null && !parentAreaName.isBlank()) {
                context.append("所属大景区：").append(parentAreaName).append("\n");
            }
            context.append("描述：").append(defaultText(spot.getDescription())).append("\n");
            context.append("建议游览时长：").append(safeInt(spot.getVisitingDuration())).append("分钟\n");
            context.append("标签：").append(defaultText(spot.getTags())).append("\n");
            context.append("偏好特征：自然=").append(safeDecimal(spot.getNatureScore()))
                    .append("，人文=").append(safeDecimal(spot.getCultureScore()))
                    .append("，拍照=").append(safeDecimal(spot.getPhotographyScore()))
                    .append("，老人友好=").append(safeDecimal(spot.getElderlyFriendlyScore()))
                    .append("，亲子=").append(safeDecimal(spot.getFamilyFriendlyScore()))
                    .append("，休息便利=").append(safeDecimal(spot.getRestConvenienceScore()))
                    .append("，体力消耗等级=").append(safeInt(spot.getIntensityLevel()))
                    .append("，排队等级=").append(safeInt(spot.getQueueLevel()))
                    .append("\n\n");
        }

        if (largeAreas.isEmpty() && smallSpots.isEmpty()) {
            context.append("当前知识库中暂无景点信息。\n\n");
        }

        String enhancedQuery = context +
                "允许出现的真实景点名称：" + buildAllowedNameSummary(allowedNames) +
                "\n\n游客问题：" + query +
                "\n\n请以导游身份回答，要求：\n" +
                "1. 只能基于以上景区资料和真实景点名称回答，不得补充现有资料里没有的景点、树林、建筑、别名或传说内容\n" +
                "2. 如果用户提到的景点不在已知景点范围内，就直接回答“开封市并没有这个景点”；如果只是缺少某个细节，再说明“暂时没有查到这方面的介绍”，不要猜测，不要编造\n" +
                "3. 如涉及公共大门、车站、夜市等节点，要说明它们可作为路线起终点或补给点\n" +
                "4. 不要提及编程、数据库、接口、字段名、内部编号、内部ID等实现细节\n" +
                "5. 不要输出任何类似 ID 3、景区ID、所属大景区ID、areaId、pathDetails、recommendedAreaIds、isAreaType、transportMode 之类的内部信息\n" +
                "6. 若用户给出偏好，如少走路、亲子、拍照、人文、赶时间，要显式体现在建议中\n" +
                "7. 优先结合景区标签、适合人群、游览体验和路线顺序给出建议\n" +
                "8. 必须全部使用简体中文，不得出现英文单词、英文缩写、拼音、外文别名或中英混写表达\n" +
                "9. 景区的价格一般表示门票或票价参考；饭店、餐馆、小吃店等餐饮地点的价格表示人均消费参考，不要说成门票\n" +
                "10. 夜市、车站、大门、游客中心等开放型公共节点，如果价格为 0，表示免费开放或无固定消费\n" +
                "11. 只用纯文本回答，不要使用 Markdown 格式\n" +
                "12. 不要出现 #、*、-、>、`、--- 等符号\n" +
                "13. 不要写 Markdown 标题，不要写项目符号列表\n" +
                "14. 直接用自然段输出，分段时不要加任何特殊符号";

        String rawAnswer = ollamaChatModel.generate(enhancedQuery);
        return sanitizeAndValidateGeneratedAnswer(rawAnswer, query, largeAreas, smallSpots, context.toString(), allowedNames);
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
        Map<String, Object> pendingContext = PENDING_CART_CONTEXT_BY_USER.get(currentUserKey);
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
        String answer = buildGeneralScenicListAnswer(query, largeAreas);
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
        scenicAreas.sort(Comparator
                .comparingDouble((LargeScenicArea area) -> safeDecimal(area.getPopularityScore())).reversed()
                .thenComparingInt(area -> safeInt(area.getRecommendedVisitDuration())).reversed());

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

        if (startArea == null && !mentionedAreas.isEmpty() && normalize(query).contains("从")) {
            startArea = mentionedAreas.get(0);
        }
        if (endArea == null && mentionedAreas.size() >= 2) {
            endArea = mentionedAreas.get(mentionedAreas.size() - 1);
        }

        Map<String, Double> preferenceWeights = extractPreferenceWeights(query);
        String routeMode = extractRouteMode(query, preferenceWeights);
        int maxStops = extractMaxStops(query);

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
        } else if (startArea != null && endArea != null && !Objects.equals(startArea.getId(), endArea.getId())) {
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
                "有哪些好玩的地方", "好玩的地方", "值得去的地方", "推荐景点", "必去景点", "开封去哪玩");
    }

    private String buildGeneralScenicListAnswer(String query, List<LargeScenicArea> largeAreas) {
        List<LargeScenicArea> scenicAreas = new ArrayList<>();
        if (largeAreas != null) {
            for (LargeScenicArea area : largeAreas) {
                if (area != null && safeInt(area.getIsAreaType()) == 0) {
                    scenicAreas.add(area);
                }
            }
        }
        if (scenicAreas.isEmpty()) {
            scenicAreas = largeAreas == null ? new ArrayList<>() : new ArrayList<>(largeAreas);
        }
        if (scenicAreas.isEmpty()) {
            return "目前还没有查到可用于介绍的景点信息。";
        }

        scenicAreas.sort(Comparator
                .comparingDouble((LargeScenicArea area) -> safeDecimal(area.getPopularityScore())).reversed()
                .thenComparingInt(area -> safeInt(area.getRecommendedVisitDuration())).reversed());

        String opening = containsAny(normalize(query), "好玩的地方", "值得去的地方")
                ? "开封比较值得一去的地方有："
                : "开封比较知名的景点有：";

        StringBuilder answer = new StringBuilder();
        answer.append(opening);
        int limit = Math.min(6, scenicAreas.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                answer.append("、");
            }
            answer.append(scenicAreas.get(i).getName());
        }
        answer.append("。");

        for (int i = 0; i < Math.min(4, scenicAreas.size()); i++) {
            LargeScenicArea area = scenicAreas.get(i);
            String description = trimToSentence(defaultText(area.getDescription()));
            if (!description.isBlank() && !"暂无信息".equals(description)) {
                answer.append(area.getName()).append("：").append(description).append("。");
            }
        }

        answer.append("如果你想按历史文化、拍照打卡、亲子休闲或少走路来选，我可以继续按你的偏好推荐。");
        return answer.toString();
    }

    private boolean isPathPlanningQuery(String query) {
        String normalized = normalize(query).toLowerCase(Locale.ROOT);
        return normalized.contains("路线")
                || normalized.contains("路径")
                || normalized.contains("怎么去")
                || normalized.contains("怎么走")
                || (normalized.contains("从") && (normalized.contains("到") || normalized.contains("去")))
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

        LargeScenicArea singleArea = resolveSingleAreaRouteTarget(startArea, endArea, mentionedAreas);
        if (singleArea != null && isSingleAreaTourIntent(query, startArea, endArea, mentionedAreas, singleArea)) {
            return buildSingleAreaTourAnswer(singleArea, query, preferenceWeights);
        }

        if (startArea != null && endArea != null && !Objects.equals(startArea.getId(), endArea.getId())) {
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
        answer.append("说明：本次仅规划")
                .append(area.getName())
                .append("园内路线，不再自动扩展到其他景区。\n");
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
        LargeScenicArea bestMatch = null;
        int bestScore = -1;

        for (LargeScenicArea area : areas) {
            String normalizedAreaName = normalize(area.getName());
            int score = -1;
            if (normalizedAreaName.equals(normalizedInput)) {
                score = 1000;
            } else if (normalizedAreaName.contains(normalizedInput)) {
                score = normalizedInput.length() + 100;
            } else if (normalizedInput.contains(normalizedAreaName)) {
                score = normalizedAreaName.length() + 50;
            }
            if (score > bestScore) {
                bestScore = score;
                bestMatch = area;
            }
        }
        return bestMatch;
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

        Pattern scenicPattern = Pattern.compile("[\u4e00-\u9fa5A-Za-z]{2,20}(?:景区|景点|公园|寺|桥|码头|门|楼|府|祠|台|城|站|中心|夜市|街|馆|园|林|树林)");
        Matcher scenicMatcher = scenicPattern.matcher(query);
        while (scenicMatcher.find()) {
            String candidate = scenicMatcher.group();
            if (!isGenericScenicPhrase(candidate)) {
                return true;
            }
        }
        return false;
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
            if (isAreaType != null && Integer.parseInt(String.valueOf(isAreaType)) == 1) {
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
        cleaned = cleaned.replaceAll("(?i)\\bmeal\\s*price\\b", "人均消费参考");
        cleaned = cleaned.replaceAll("(?i)\\bticket\\s*price\\b", "门票参考");
        cleaned = cleaned.replaceAll("(?i)\\bprice\\b", "价格参考");
        cleaned = cleaned.replaceAll("\\bWALK\\b", "步行");
        cleaned = cleaned.replaceAll("\\bROAD\\b", "道路通行");
        cleaned = cleaned.replaceAll("\\bDRIVE\\b", "驾车");
        cleaned = cleaned.replaceAll("\\bSHUTTLE\\b", "接驳车");
        cleaned = cleaned.replaceAll("\\bCABLEWAY\\b", "索道");
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

        Pattern scenicPattern = Pattern.compile("[\u4e00-\u9fa5A-Za-z]{2,20}(?:景区|景点|公园|寺|桥|码头|门|楼|府|祠|台|城|站|中心|夜市|街|馆|园|林|树林)");
        Matcher scenicMatcher = scenicPattern.matcher(text);
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
        return candidate.length() <= 2
                || "景区".equals(candidate)
                || "景点".equals(candidate)
                || "公园".equals(candidate)
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
        Pattern scenicPattern = Pattern.compile("[\u4e00-\u9fa5A-Za-z]{2,20}(?:景区|景点|公园|寺|桥|码头|门|楼|府|祠|台|城|站|中心|夜市|街|馆|园|林|树林)");
        Matcher matcher = scenicPattern.matcher(text);
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
        return ollamaChatModel.generate(rewritePrompt);
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

        answer.append("目前较受关注的景点有：");
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
        answer.append("。以上内容仅依据现有景点资料整理，不包含未收录的名称或别名。");
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
