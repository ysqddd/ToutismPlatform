package org.example.toutismplatform.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.entity.SmallScenicSpot;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.example.toutismplatform.repository.SmallScenicSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RagService {

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @Autowired
    private LargeScenicAreaRepository largeScenicAreaRepository;

    @Autowired
    private SmallScenicSpotRepository smallScenicSpotRepository;

    @Autowired
    private PathService pathService;

    public String generateAnswer(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "请先告诉我你的问题，我可以为你介绍景点或规划路线。";
        }

        if (isPathPlanningQuery(query)) {
            return handlePathPlanningQuery(query);
        }

        List<LargeScenicArea> largeAreas = largeScenicAreaRepository.findAll();
        List<SmallScenicSpot> smallSpots = smallScenicSpotRepository.findAll();

        StringBuilder context = new StringBuilder();
        context.append("你是一名深知景点内容的导游，负责回答游客关于景点的问题。\n\n");
        context.append("重要声明：以下所有景区都是虚构的，仅用于系统演示和测试，请勿与现实中的任何景区进行关联或对比。\n\n");
        context.append("景点知识库：\n");
        context.append("重要提示：所有收费景区都需要通过免费的公共大门或游客中心进入，这些公共节点不收门票。\n\n");

        for (LargeScenicArea area : largeAreas) {
            context.append("大景区/地点：").append(area.getName()).append("\n");
            context.append("描述：").append(defaultText(area.getDescription())).append("\n");
            context.append("位置：").append(defaultText(area.getLocation())).append("\n");
            context.append("开放时间：").append(defaultText(area.getOpeningHours())).append("\n");
            context.append("建议游览时长：").append(safeInt(area.getRecommendedVisitDuration())).append("分钟\n");
            context.append("价格：");
            if (safeInt(area.getIsAreaType()) == 1) {
                context.append("免费（非景区节点）\n");
            } else {
                context.append(safeDecimal(area.getPrice())).append("元\n");
            }
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
                "1. 只基于以上虚构景区知识库回答\n" +
                "2. 如涉及公共大门/游客中心，要说明它们免费且常作为进入收费景区的入口\n" +
                "3. 不要提及编程、数据库、接口等实现细节\n" +
                "4. 若用户问现实景区，请礼貌说明当前系统展示的是虚构演示景区\n" +
                "5. 优先结合景区标签、适合人群和游览体验给出建议";

        return ollamaChatModel.generate(enhancedQuery);
    }

    private boolean isPathPlanningQuery(String query) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return normalized.contains("路线")
                || normalized.contains("路径")
                || normalized.contains("怎么去")
                || normalized.contains("怎么走")
                || normalized.contains("从") && (normalized.contains("到") || normalized.contains("去"))
                || normalized.contains("规划")
                || normalized.contains("安排路线")
                || normalized.contains("推荐路线");
    }

    private String handlePathPlanningQuery(String query) {
        List<LargeScenicArea> allAreas = largeScenicAreaRepository.findAll();
        Map<String, String> locations = extractLocations(query, allAreas);
        String startName = locations.get("start");
        String endName = locations.get("end");

        if (startName == null || endName == null) {
            return "请明确告诉我起点和终点，例如：从东大门到书证沟景区，想少走路、适合老人。";
        }

        LargeScenicArea startArea = findAreaByName(startName, allAreas);
        LargeScenicArea endArea = findAreaByName(endName, allAreas);

        if (startArea == null) {
            return "抱歉，我找不到名为“" + startName + "”的地点，请检查名称是否正确。";
        }
        if (endArea == null) {
            return "抱歉，我找不到名为“" + endName + "”的地点，请检查名称是否正确。";
        }

        Map<String, Double> preferenceWeights = extractPreferenceWeights(query);
        Map<String, Object> personalizedPath = pathService.calculatePersonalizedPath(startArea.getId(), endArea.getId(), preferenceWeights);
        if (!Boolean.TRUE.equals(personalizedPath.get("success"))) {
            return String.valueOf(personalizedPath.getOrDefault("message", "暂时无法规划该路线。"));
        }

        Map<String, Object> distancePath = pathService.calculateShortestPath(startArea.getId(), endArea.getId(), "distance");
        Map<String, Object> timePath = pathService.calculateShortestPath(startArea.getId(), endArea.getId(), "duration");

        return buildPathAnswer(query, startArea, endArea, preferenceWeights, personalizedPath, distancePath, timePath);
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
                String startCandidate = matcher.group(1).trim();
                String endCandidate = matcher.group(2).trim();
                LargeScenicArea startArea = findAreaByName(startCandidate, areas);
                LargeScenicArea endArea = findAreaByName(endCandidate, areas);
                if (startArea != null && endArea != null) {
                    result.put("start", startArea.getName());
                    result.put("end", endArea.getName());
                    return result;
                }
            }
        }

        List<NameHit> hits = new ArrayList<>();
        String normalizedQuery = normalize(query);
        for (LargeScenicArea area : areas) {
            String normalizedName = normalize(area.getName());
            int index = normalizedQuery.indexOf(normalizedName);
            if (index >= 0) {
                hits.add(new NameHit(area.getName(), index, normalizedName.length()));
            }
        }

        hits.sort(Comparator.comparingInt(NameHit::getIndex).thenComparing((a, b) -> Integer.compare(b.getLength(), a.getLength())));
        List<String> orderedDistinctNames = new ArrayList<>();
        for (NameHit hit : hits) {
            if (!orderedDistinctNames.contains(hit.getName())) {
                orderedDistinctNames.add(hit.getName());
            }
        }

        if (orderedDistinctNames.size() >= 2) {
            result.put("start", orderedDistinctNames.get(0));
            result.put("end", orderedDistinctNames.get(1));
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

    private Map<String, Double> extractPreferenceWeights(String query) {
        String text = normalize(query);
        Map<String, Double> weights = initWeightMap();

        addWeightIfContains(text, weights, 1.6, "duration", "最快", "赶时间", "尽快", "节省时间", "快一点", "时间短");
        addWeightIfContains(text, weights, 1.3, "distance", "最近", "最短", "路程短", "少绕路");

        addWeightIfContains(text, weights, 1.6, "cost", "省钱", "便宜", "预算低", "花费少", "少花钱", "经济");

        addWeightIfContains(text, weights, 1.6, "intensity", "少走路", "轻松", "不想太累", "别太累", "少爬", "不想爬山", "体力消耗低");
        addWeightIfContains(text, weights, 1.2, "comfort", "舒服", "舒适", "不要折腾", "平稳");
        addWeightIfContains(text, weights, 1.5, "crowd", "人少", "避开人群", "不要太挤", "别太拥挤", "清净");

        addWeightIfContains(text, weights, 1.6, "elderlyFriendly", "老人", "长辈", "爸妈", "老年人", "适合老人");
        if (weights.get("elderlyFriendly") > 0) {
            addWeight(weights, "intensity", 0.8);
            addWeight(weights, "comfort", 0.8);
            addWeight(weights, "restroomConvenience", 0.6);
        }

        addWeightIfContains(text, weights, 1.5, "familyFriendly", "亲子", "孩子", "小孩", "儿童", "带娃");
        if (weights.get("familyFriendly") > 0) {
            addWeight(weights, "restroomConvenience", 0.5);
            addWeight(weights, "leisure", 0.4);
        }

        addWeightIfContains(text, weights, 1.5, "nature", "自然", "风景", "山水", "湖", "瀑布", "森林", "海子", "景色");
        addWeightIfContains(text, weights, 1.5, "culture", "文化", "历史", "人文", "书院", "古迹", "讲学");
        addWeightIfContains(text, weights, 1.5, "photography", "拍照", "摄影", "出片", "打卡", "观景");
        addWeightIfContains(text, weights, 1.3, "leisure", "休闲", "悠闲", "慢慢逛", "放松", "休息");
        addWeightIfContains(text, weights, 1.2, "foodConvenience", "吃饭", "餐饮", "美食", "用餐");
        addWeightIfContains(text, weights, 1.2, "restroomConvenience", "卫生间", "厕所", "洗手间");
        addWeightIfContains(text, weights, 1.0, "popularity", "热门", "经典", "必去", "网红");

        boolean hasPreference = weights.values().stream().anyMatch(value -> value > 0.0);
        if (!hasPreference) {
            weights.put("duration", 0.8);
            weights.put("distance", 0.6);
        }
        return weights;
    }

    private Map<String, Double> initWeightMap() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("distance", 0.0);
        weights.put("duration", 0.0);
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

    private String buildPathAnswer(String query,
                                   LargeScenicArea startArea,
                                   LargeScenicArea endArea,
                                   Map<String, Double> preferenceWeights,
                                   Map<String, Object> personalizedPath,
                                   Map<String, Object> distancePath,
                                   Map<String, Object> timePath) {
        StringBuilder answer = new StringBuilder();
        answer.append("我已经根据你的直接描述做了个性化路线规划。\n\n");
        answer.append("起点：").append(startArea.getName()).append("\n");
        answer.append("终点：").append(endArea.getName()).append("\n");
        answer.append("识别到的偏好：").append(buildPreferenceSummary(preferenceWeights)).append("\n\n");

        answer.append("推荐路线：\n");
        appendPathNodes(answer, personalizedPath);
        answer.append("总距离：")
                .append(String.format(Locale.ROOT, "%.1f", getDouble(personalizedPath, "totalDistance")))
                .append("米\n");
        answer.append("预计通行时间：")
                .append((int) getDouble(personalizedPath, "totalDuration"))
                .append("分钟\n");
        answer.append("路线额外花费：")
                .append(String.format(Locale.ROOT, "%.2f", getDouble(personalizedPath, "totalCost")))
                .append("元\n\n");

        List<Map<String, Object>> segmentDetails = getSegmentDetails(personalizedPath);
        if (!segmentDetails.isEmpty()) {
            answer.append("分段说明：\n");
            for (int i = 0; i < segmentDetails.size(); i++) {
                Map<String, Object> segment = segmentDetails.get(i);
                answer.append(i + 1)
                        .append(". ")
                        .append(segment.get("fromName"))
                        .append(" → ")
                        .append(segment.get("toName"))
                        .append("，方式：")
                        .append(defaultText((String) segment.get("transportMode")))
                        .append("，距离：")
                        .append(segment.get("distance"))
                        .append("米，时间：")
                        .append(segment.get("duration"))
                        .append("分钟");
                Object costAmount = segment.get("costAmount");
                if (costAmount != null && Double.parseDouble(String.valueOf(costAmount)) > 0) {
                    answer.append("，花费：").append(costAmount).append("元");
                }
                if (segment.get("description") != null) {
                    answer.append("（").append(segment.get("description")).append("）");
                }
                answer.append("\n");
            }
            answer.append("\n");
        }

        if (Boolean.TRUE.equals(distancePath.get("success"))) {
            answer.append("仅按最短距离计算时：")
                    .append(String.format(Locale.ROOT, "%.1f", getDouble(distancePath, "totalDistance")))
                    .append("米。\n");
        }
        if (Boolean.TRUE.equals(timePath.get("success"))) {
            answer.append("仅按最短时间计算时：")
                    .append((int) getDouble(timePath, "totalDuration"))
                    .append("分钟。\n");
        }

        answer.append("\n说明：本次推荐不是单纯按最短路，而是把你在问题里表达的偏好转成了权重，再对路径和地点一起打分后得到的结果。");
        return answer.toString();
    }

    private void appendPathNodes(StringBuilder answer, Map<String, Object> pathResult) {
        List<Map<String, Object>> pathDetails = getPathDetails(pathResult);
        for (int i = 0; i < pathDetails.size(); i++) {
            Map<String, Object> area = pathDetails.get(i);
            answer.append(i + 1).append(". ").append(area.get("name"));
            Object isAreaType = area.get("isAreaType");
            if (isAreaType != null && Integer.parseInt(String.valueOf(isAreaType)) == 1) {
                answer.append("（公共节点/非景区地点）");
            }
            answer.append("\n");
        }
        answer.append("\n");
    }

    private String buildPreferenceSummary(Map<String, Double> preferenceWeights) {
        List<String> labels = new ArrayList<>();
        addLabelIfHigh(preferenceWeights, labels, "distance", "尽量走更短路线");
        addLabelIfHigh(preferenceWeights, labels, "duration", "更看重节省时间");
        addLabelIfHigh(preferenceWeights, labels, "cost", "更看重节省花费");
        addLabelIfHigh(preferenceWeights, labels, "intensity", "想少走路/少消耗体力");
        addLabelIfHigh(preferenceWeights, labels, "crowd", "希望人少一些");
        addLabelIfHigh(preferenceWeights, labels, "nature", "偏好自然风景");
        addLabelIfHigh(preferenceWeights, labels, "culture", "偏好人文历史");
        addLabelIfHigh(preferenceWeights, labels, "photography", "偏好拍照观景");
        addLabelIfHigh(preferenceWeights, labels, "elderlyFriendly", "希望更适合老人");
        addLabelIfHigh(preferenceWeights, labels, "familyFriendly", "希望更适合亲子");
        addLabelIfHigh(preferenceWeights, labels, "leisure", "更偏好轻松休闲");
        addLabelIfHigh(preferenceWeights, labels, "foodConvenience", "希望餐饮更方便");
        addLabelIfHigh(preferenceWeights, labels, "restroomConvenience", "希望卫生间更方便");
        addLabelIfHigh(preferenceWeights, labels, "comfort", "更看重舒适度");
        addLabelIfHigh(preferenceWeights, labels, "popularity", "偏好热门经典点位");
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

    private double getDouble(Map<String, Object> result, String key) {
        Object value = result.get(key);
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

    private String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "").replace("“", "").replace("”", "").replace("\"", "");
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
