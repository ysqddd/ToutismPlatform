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

@Service
public class RagService {

    private static final String MODE_DISTANCE = "distance";
    private static final String MODE_DURATION = "duration";
    private static final String MODE_PERSONALIZED = "personalized";
    private static final int ALL_SCENIC_STOPS = Integer.MAX_VALUE;

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

    public String generateAnswer(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "请先告诉我你的问题，我可以为你介绍景点或规划路线。";
        }

        if (isPathPlanningQuery(query)) {
            return sanitizeAiAnswer(handlePathPlanningQuery(query));
        }

        List<LargeScenicArea> largeAreas = largeScenicAreaRepository.findAll();
        List<SmallScenicSpot> smallSpots = smallScenicSpotRepository.findAll();

        StringBuilder context = new StringBuilder();
        context.append("你是一名深知景点内容的导游，负责回答游客关于景点的问题。\n\n");
        context.append("说明：以下内容优先基于系统当前数据库中的景区与点位信息进行推荐；若用户偏好明确，应优先结合偏好、路线与游玩节奏回答。\n\n");
        context.append("景点知识库：\n");
        context.append("重要提示：所有收费景区都需要通过免费的公共大门或游客中心进入，这些公共节点不收门票。\n\n");

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
            context.append("所属大景区ID：").append(spot.getLargeAreaId()).append("\n");
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
                "游客问题：" + query +
                "\n\n请以导游身份回答，要求：\n" +
                "1. 优先基于以上景区知识库回答\n" +
                "2. 如涉及公共大门、车站、夜市等节点，要说明它们可作为路线起终点或补给点\n" +
                "3. 不要提及编程、数据库、接口等实现细节\n" +
                "4. 若用户给出偏好，如少走路、亲子、拍照、人文、赶时间，要显式体现在建议中\n" +
                "5. 优先结合景区标签、适合人群、游览体验和路线顺序给出建议\n" +
                "6. 景区的价格一般表示门票或票价参考；饭店、餐馆、小吃店等餐饮地点的价格表示平均人均消费，不要说成门票\n" +
                "7. 夜市、车站、大门、游客中心等开放型公共节点，如果价格为 0，表示无需门票或无固定消费\n" +
                "8. 只用纯文本回答，不要使用 Markdown 格式\n" +
                "9. 不要出现 #、*、-、>、`、--- 等符号\n" +
                "10. 不要写 Markdown 标题，不要写项目符号列表\n" +
                "11. 直接用自然段输出，分段时不要加任何特殊符号";

        String rawAnswer = ollamaChatModel.generate(enhancedQuery);
        return sanitizeAiAnswer(rawAnswer);
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
            answer.append("说明：当前数据库里还没有该景区的园内点位数据，所以本次不再扩展到其他景区，只返回该景区本身的游玩建议。\n");
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
            return "参考人均消费：" + safeDecimal(safePrice) + "元";
        }

        if (isOpenConsumptionNode(area)) {
            if (safePrice.compareTo(BigDecimal.ZERO) > 0) {
                return "门票：免费；消费参考：" + safeDecimal(safePrice) + "元";
            }
            return "门票：免费（开放型公共节点）";
        }

        if (safeInt(area == null ? null : area.getIsAreaType()) == 1) {
            if (safePrice.compareTo(BigDecimal.ZERO) > 0) {
                return "费用参考：" + safeDecimal(safePrice) + "元";
            }
            return "免费（非景区节点）";
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
        cleaned = cleaned.replaceAll("(?m)^\s*#{1,6}\s*", "");
        cleaned = cleaned.replaceAll("(?m)^\s*[-*+]\s+", "");
        cleaned = cleaned.replaceAll("(?m)^\s*>\s*", "");
        cleaned = cleaned.replaceAll("(?m)^\s*-{3,}\s*$", "");
        cleaned = cleaned.replace("**", "");
        cleaned = cleaned.replace("*", "");
        cleaned = cleaned.replace("```", "");
        cleaned = cleaned.replace("`", "");
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        return cleaned.trim();
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
