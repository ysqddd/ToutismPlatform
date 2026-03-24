package org.example.toutismplatform.service;

import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.entity.ScenicAreaEdge;
import org.example.toutismplatform.entity.SmallScenicSpot;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.example.toutismplatform.repository.ScenicAreaEdgeRepository;
import org.example.toutismplatform.repository.SmallScenicSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class PathService {

    private static final String MODE_DISTANCE = "distance";
    private static final String MODE_DURATION = "duration";
    private static final String MODE_PERSONALIZED = "personalized";

    @Autowired
    private LargeScenicAreaRepository largeScenicAreaRepository;

    @Autowired
    private ScenicAreaEdgeRepository scenicAreaEdgeRepository;

    @Autowired
    private SmallScenicSpotRepository smallScenicSpotRepository;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> calculateShortestPath(Long startAreaId, Long endAreaId, String weightType) {
        String mode = normalizeWeightType(weightType);
        return calculateClassicPath(startAreaId, endAreaId, mode);
    }

    public Map<String, Object> calculatePersonalizedPath(Long startAreaId,
                                                         Long endAreaId,
                                                         Map<String, Double> preferenceWeights) {
        List<LargeScenicArea> areas = largeScenicAreaRepository.findAll();
        List<ScenicAreaEdge> edges = scenicAreaEdgeRepository.findAll();
        Map<Long, LargeScenicArea> areaMap = buildAreaMap(areas);

        if (!areaMap.containsKey(startAreaId) || !areaMap.containsKey(endAreaId)) {
            return buildFailureResult("起点或终点不存在，无法规划路线。", MODE_PERSONALIZED);
        }

        Map<Long, List<Edge>> graph = buildPersonalizedGraph(areas, edges, preferenceWeights);
        return runDijkstra(startAreaId, endAreaId, graph, areaMap, MODE_PERSONALIZED, preferenceWeights);
    }

    public Map<String, Object> recommendCityRoute(Long preferredStartAreaId,
                                                  Long preferredEndAreaId,
                                                  Map<String, Double> preferenceWeights,
                                                  String routeMode,
                                                  int maxStops) {
        List<LargeScenicArea> areas = largeScenicAreaRepository.findAll();
        List<ScenicAreaEdge> edges = scenicAreaEdgeRepository.findAll();
        Map<Long, LargeScenicArea> areaMap = buildAreaMap(areas);
        String mode = normalizeWeightType(routeMode);

        if (areas.isEmpty()) {
            return buildFailureResult("当前没有可用于规划的景区数据。", mode);
        }

        List<LargeScenicArea> scenicAreas = new ArrayList<>();
        for (LargeScenicArea area : areas) {
            if (safeInt(area.getIsAreaType()) == 0) {
                scenicAreas.add(area);
            }
        }
        if (scenicAreas.isEmpty()) {
            scenicAreas.addAll(areas);
        }

        scenicAreas.sort((a, b) -> Double.compare(scoreArea(b, preferenceWeights), scoreArea(a, preferenceWeights)));

        Map<Long, List<Edge>> graph = MODE_PERSONALIZED.equals(mode)
                ? buildPersonalizedGraph(areas, edges, preferenceWeights)
                : buildClassicGraph(areas, edges, mode);

        List<Long> stops = new ArrayList<>();
        Set<Long> used = new LinkedHashSet<>();

        Long currentId = preferredStartAreaId != null && areaMap.containsKey(preferredStartAreaId)
                ? preferredStartAreaId
                : scenicAreas.get(0).getId();
        stops.add(currentId);
        used.add(currentId);

        int scenicCount = scenicAreas.size();
        int target = maxStops == Integer.MAX_VALUE ? scenicCount : Math.max(1, Math.min(maxStops, scenicCount));
        while (countScenicStops(stops, areaMap) < target) {
            Long nextId = selectNextArea(currentId, scenicAreas, used, graph, areaMap, mode, preferenceWeights, preferredEndAreaId);
            if (nextId == null) {
                break;
            }
            stops.add(nextId);
            used.add(nextId);
            currentId = nextId;
        }

        if (preferredEndAreaId != null && areaMap.containsKey(preferredEndAreaId) && !Objects.equals(preferredEndAreaId, currentId)) {
            stops.add(preferredEndAreaId);
        }

        if (stops.size() == 1) {
            return buildSingleStopResult(areaMap.get(stops.get(0)), mode, preferenceWeights);
        }
        return mergeRoute(stops, graph, areaMap, mode, preferenceWeights);
    }

    private Map<String, Object> calculateClassicPath(Long startAreaId, Long endAreaId, String weightType) {
        List<LargeScenicArea> areas = largeScenicAreaRepository.findAll();
        List<ScenicAreaEdge> edges = scenicAreaEdgeRepository.findAll();
        Map<Long, LargeScenicArea> areaMap = buildAreaMap(areas);

        if (!areaMap.containsKey(startAreaId) || !areaMap.containsKey(endAreaId)) {
            return buildFailureResult("起点或终点不存在，无法规划路线。", weightType);
        }

        Map<Long, List<Edge>> graph = buildClassicGraph(areas, edges, weightType);
        return runDijkstra(startAreaId, endAreaId, graph, areaMap, weightType, null);
    }

    private Map<Long, LargeScenicArea> buildAreaMap(List<LargeScenicArea> areas) {
        Map<Long, LargeScenicArea> map = new HashMap<>();
        for (LargeScenicArea area : areas) {
            map.put(area.getId(), area);
        }
        return map;
    }

    private Map<Long, List<Edge>> buildClassicGraph(List<LargeScenicArea> areas, List<ScenicAreaEdge> edges, String mode) {
        Map<Long, List<Edge>> graph = new HashMap<>();
        for (LargeScenicArea area : areas) {
            graph.put(area.getId(), new ArrayList<>());
        }
        for (ScenicAreaEdge edge : edges) {
            graph.computeIfAbsent(edge.getFromAreaId(), k -> new ArrayList<>());
            graph.computeIfAbsent(edge.getToAreaId(), k -> new ArrayList<>());
            double weight = MODE_DISTANCE.equals(mode) ? safeDecimal(edge.getDistance()) : safeInt(edge.getDuration());
            graph.get(edge.getFromAreaId()).add(new Edge(edge.getFromAreaId(), edge.getToAreaId(), weight, edge));
            graph.get(edge.getToAreaId()).add(new Edge(edge.getToAreaId(), edge.getFromAreaId(), weight, edge));
        }
        return graph;
    }

    private Map<Long, List<Edge>> buildPersonalizedGraph(List<LargeScenicArea> areas,
                                                         List<ScenicAreaEdge> edges,
                                                         Map<String, Double> preferenceWeights) {
        Map<Long, LargeScenicArea> areaMap = buildAreaMap(areas);
        Map<Long, List<Edge>> graph = new HashMap<>();
        for (LargeScenicArea area : areas) {
            graph.put(area.getId(), new ArrayList<>());
        }
        for (ScenicAreaEdge edge : edges) {
            LargeScenicArea forward = areaMap.get(edge.getToAreaId());
            LargeScenicArea backward = areaMap.get(edge.getFromAreaId());
            double w1 = personalizedWeight(edge, forward, preferenceWeights);
            double w2 = personalizedWeight(edge, backward, preferenceWeights);
            graph.get(edge.getFromAreaId()).add(new Edge(edge.getFromAreaId(), edge.getToAreaId(), w1, edge));
            graph.get(edge.getToAreaId()).add(new Edge(edge.getToAreaId(), edge.getFromAreaId(), w2, edge));
        }
        return graph;
    }

    private double personalizedWeight(ScenicAreaEdge edge, LargeScenicArea target, Map<String, Double> weights) {
        double result = safeDecimal(edge.getDistance()) / 1000.0 * (1.0 + getWeight(weights, MODE_DISTANCE) * 2.0)
                + safeInt(edge.getDuration()) / 10.0 * (1.0 + getWeight(weights, MODE_DURATION) * 2.2)
                + safeDecimal(edge.getCostAmount()) / 10.0 * (0.8 + getWeight(weights, "cost") * 1.8);
        if (target != null) {
            result += safeInt(target.getIntensityLevel()) * (0.3 + getWeight(weights, "intensity") * 0.6);
            result += safeInt(target.getCrowdLevel()) * (0.2 + getWeight(weights, "crowd") * 0.5);
            result -= safeDecimal(target.getNatureScore()) * getWeight(weights, "nature");
            result -= safeDecimal(target.getCultureScore()) * getWeight(weights, "culture");
            result -= safeDecimal(target.getPhotographyScore()) * getWeight(weights, "photography");
            result -= safeDecimal(target.getFamilyFriendlyScore()) * getWeight(weights, "familyFriendly");
            result -= safeDecimal(target.getElderlyFriendlyScore()) * getWeight(weights, "elderlyFriendly");
            result -= safeDecimal(target.getLeisureScore()) * getWeight(weights, "leisure");
            result -= safeDecimal(target.getFoodConvenienceScore()) * getWeight(weights, "foodConvenience");
            result -= safeDecimal(target.getRestroomConvenienceScore()) * getWeight(weights, "restroomConvenience");
            result -= safeDecimal(target.getPopularityScore()) * getWeight(weights, "popularity") * 0.6;
        }
        return Math.max(0.3, result);
    }

    private Map<String, Object> runDijkstra(Long startAreaId,
                                            Long endAreaId,
                                            Map<Long, List<Edge>> graph,
                                            Map<Long, LargeScenicArea> areaMap,
                                            String weightType,
                                            Map<String, Double> preferenceWeights) {
        if (!graph.containsKey(startAreaId) || !graph.containsKey(endAreaId)) {
            return buildFailureResult("起点或终点不存在于当前图中。", weightType);
        }

        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        Map<Long, Edge> prevEdge = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance));

        for (Long id : graph.keySet()) {
            dist.put(id, Double.MAX_VALUE);
        }
        dist.put(startAreaId, 0.0);
        pq.offer(new Node(startAreaId, 0.0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (current.distance > dist.getOrDefault(current.id, Double.MAX_VALUE)) {
                continue;
            }
            if (Objects.equals(current.id, endAreaId)) {
                break;
            }
            for (Edge edge : graph.getOrDefault(current.id, Collections.emptyList())) {
                double next = current.distance + edge.weight;
                if (next < dist.getOrDefault(edge.to, Double.MAX_VALUE)) {
                    dist.put(edge.to, next);
                    prev.put(edge.to, current.id);
                    prevEdge.put(edge.to, edge);
                    pq.offer(new Node(edge.to, next));
                }
            }
        }

        if (dist.getOrDefault(endAreaId, Double.MAX_VALUE) == Double.MAX_VALUE) {
            return buildFailureResult("起点和终点之间暂无可达路径。", weightType);
        }

        List<Long> path = new ArrayList<>();
        Long current = endAreaId;
        while (current != null) {
            path.add(current);
            current = prev.get(current);
        }
        Collections.reverse(path);
        return buildPathResult(path, prevEdge, areaMap, weightType, preferenceWeights, dist.get(endAreaId));
    }

    private Map<String, Object> mergeRoute(List<Long> stops,
                                           Map<Long, List<Edge>> graph,
                                           Map<Long, LargeScenicArea> areaMap,
                                           String mode,
                                           Map<String, Double> preferenceWeights) {
        List<Long> mergedPath = new ArrayList<>();
        List<Map<String, Object>> mergedSegments = new ArrayList<>();
        double totalWeight = 0.0;
        double totalDistance = 0.0;
        int totalDuration = 0;
        double totalCost = 0.0;

        for (int i = 1; i < stops.size(); i++) {
            Map<String, Object> step = runDijkstra(stops.get(i - 1), stops.get(i), graph, areaMap, mode, preferenceWeights);
            if (!Boolean.TRUE.equals(step.get("success"))) {
                return buildFailureResult("推荐的景区之间缺少连通路径，无法生成完整城市路线。", mode);
            }
            List<Long> stepPath = extractPathIds(step);
            if (mergedPath.isEmpty()) {
                mergedPath.addAll(stepPath);
            } else if (stepPath.size() > 1) {
                mergedPath.addAll(stepPath.subList(1, stepPath.size()));
            }
            mergedSegments.addAll(getSegmentDetails(step));
            totalWeight += getDouble(step, "totalWeight");
            totalDistance += getDouble(step, "totalDistance");
            totalDuration += (int) getDouble(step, "totalDuration");
            totalCost += getDouble(step, "totalCost");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("path", mergedPath);
        result.put("weightType", mode);
        result.put("totalWeight", totalWeight);
        result.put("pathDetails", buildPathDetails(mergedPath, areaMap));
        result.put("segmentDetails", mergedSegments);
        result.put("totalDistance", totalDistance);
        result.put("totalDuration", totalDuration);
        result.put("totalCost", totalCost);
        result.put("recommendedAreaIds", new ArrayList<>(new LinkedHashSet<>(stops)));
        if (preferenceWeights != null) {
            result.put("preferences", new LinkedHashMap<>(preferenceWeights));
        }
        enrichVisitSummary(result, mergedPath, areaMap, preferenceWeights);
        return result;
    }

    private Map<String, Object> buildPathResult(List<Long> path,
                                                Map<Long, Edge> previousEdges,
                                                Map<Long, LargeScenicArea> areaMap,
                                                String weightType,
                                                Map<String, Double> preferenceWeights,
                                                double totalWeight) {
        List<Map<String, Object>> segmentDetails = new ArrayList<>();
        double totalDistance = 0.0;
        int totalDuration = 0;
        double totalCost = 0.0;

        for (int i = 1; i < path.size(); i++) {
            Edge edge = previousEdges.get(path.get(i));
            if (edge == null || edge.rawEdge == null) {
                continue;
            }
            ScenicAreaEdge raw = edge.rawEdge;
            totalDistance += safeDecimal(raw.getDistance());
            totalDuration += safeInt(raw.getDuration());
            totalCost += safeDecimal(raw.getCostAmount());

            Map<String, Object> segment = new LinkedHashMap<>();
            segment.put("fromAreaId", edge.from);
            segment.put("toAreaId", edge.to);
            segment.put("fromName", areaMap.containsKey(edge.from) ? areaMap.get(edge.from).getName() : "未知地点");
            segment.put("toName", areaMap.containsKey(edge.to) ? areaMap.get(edge.to).getName() : "未知地点");
            segment.put("distance", raw.getDistance());
            segment.put("duration", raw.getDuration());
            segment.put("costAmount", raw.getCostAmount());
            segment.put("description", raw.getDescription());
            segment.put("transportMode", inferTransportMode(raw.getDescription()));
            segment.put("calculatedWeight", edge.weight);
            segmentDetails.add(segment);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("path", path);
        result.put("weightType", weightType);
        result.put("totalWeight", totalWeight);
        result.put("pathDetails", buildPathDetails(path, areaMap));
        result.put("segmentDetails", segmentDetails);
        result.put("totalDistance", totalDistance);
        result.put("totalDuration", totalDuration);
        result.put("totalCost", totalCost);
        if (preferenceWeights != null) {
            result.put("preferences", new LinkedHashMap<>(preferenceWeights));
        }
        enrichVisitSummary(result, path, areaMap, preferenceWeights);
        return result;
    }

    private List<Map<String, Object>> buildPathDetails(List<Long> path, Map<Long, LargeScenicArea> areaMap) {
        List<Map<String, Object>> details = new ArrayList<>();
        for (Long id : path) {
            LargeScenicArea area = areaMap.get(id);
            if (area == null) {
                continue;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("id", area.getId());
            info.put("name", area.getName());
            info.put("isAreaType", area.getIsAreaType());
            info.put("tags", area.getTags());
            info.put("location", area.getLocation());
            info.put("recommendedVisitDuration", area.getRecommendedVisitDuration());
            details.add(info);
        }
        return details;
    }

    private Map<String, Object> buildSingleStopResult(LargeScenicArea area,
                                                      String mode,
                                                      Map<String, Double> preferenceWeights) {
        Map<Long, LargeScenicArea> map = new HashMap<>();
        map.put(area.getId(), area);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("path", Collections.singletonList(area.getId()));
        result.put("weightType", mode);
        result.put("totalWeight", 0.0);
        result.put("pathDetails", buildPathDetails(Collections.singletonList(area.getId()), map));
        result.put("segmentDetails", Collections.emptyList());
        result.put("totalDistance", 0.0);
        result.put("totalDuration", 0);
        result.put("totalCost", 0.0);
        result.put("recommendedAreaIds", Collections.singletonList(area.getId()));
        if (preferenceWeights != null) {
            result.put("preferences", new LinkedHashMap<>(preferenceWeights));
        }
        enrichVisitSummary(result, Collections.singletonList(area.getId()), map, preferenceWeights);
        return result;
    }

    private Map<String, Object> buildFailureResult(String message, String mode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        result.put("weightType", mode);
        result.put("path", Collections.emptyList());
        result.put("pathDetails", Collections.emptyList());
        result.put("segmentDetails", Collections.emptyList());
        result.put("totalWeight", Double.MAX_VALUE);
        result.put("totalDistance", 0.0);
        result.put("totalDuration", 0);
        result.put("totalCost", 0.0);
        result.put("recommendedAreaIds", Collections.emptyList());
        return result;
    }

    private Long selectNextArea(Long currentId,
                                List<LargeScenicArea> scenicAreas,
                                Set<Long> used,
                                Map<Long, List<Edge>> graph,
                                Map<Long, LargeScenicArea> areaMap,
                                String mode,
                                Map<String, Double> preferenceWeights,
                                Long preferredEndAreaId) {
        Long bestId = null;
        double bestScore = -Double.MAX_VALUE;
        for (LargeScenicArea area : scenicAreas) {
            if (used.contains(area.getId())) {
                continue;
            }
            if (preferredEndAreaId != null && Objects.equals(preferredEndAreaId, area.getId())) {
                continue;
            }
            Map<String, Object> step = runDijkstra(currentId, area.getId(), graph, areaMap, mode, preferenceWeights);
            if (!Boolean.TRUE.equals(step.get("success"))) {
                continue;
            }
            double score = scoreArea(area, preferenceWeights) * 4.0 - transitionPenalty(step, mode);
            if (score > bestScore) {
                bestScore = score;
                bestId = area.getId();
            }
        }
        return bestId;
    }

    private double scoreArea(LargeScenicArea area, Map<String, Double> weights) {
        double score = 0.0;
        score += safeDecimal(area.getNatureScore()) * getWeight(weights, "nature");
        score += safeDecimal(area.getCultureScore()) * getWeight(weights, "culture");
        score += safeDecimal(area.getPhotographyScore()) * getWeight(weights, "photography");
        score += safeDecimal(area.getFamilyFriendlyScore()) * getWeight(weights, "familyFriendly");
        score += safeDecimal(area.getElderlyFriendlyScore()) * getWeight(weights, "elderlyFriendly");
        score += safeDecimal(area.getLeisureScore()) * getWeight(weights, "leisure");
        score += safeDecimal(area.getFoodConvenienceScore()) * getWeight(weights, "foodConvenience");
        score += safeDecimal(area.getRestroomConvenienceScore()) * getWeight(weights, "restroomConvenience");
        score += safeDecimal(area.getPopularityScore()) * getWeight(weights, "popularity");
        score -= safeInt(area.getIntensityLevel()) * getWeight(weights, "intensity") * 0.8;
        score -= safeInt(area.getCrowdLevel()) * getWeight(weights, "crowd") * 0.6;
        if (score <= 0.0) {
            return safeDecimal(area.getPopularityScore()) + safeDecimal(area.getNatureScore())
                    + safeDecimal(area.getCultureScore()) + safeDecimal(area.getPhotographyScore());
        }
        return score;
    }

    private int countScenicStops(List<Long> stops, Map<Long, LargeScenicArea> areaMap) {
        int count = 0;
        for (Long id : stops) {
            LargeScenicArea area = areaMap.get(id);
            if (area != null && safeInt(area.getIsAreaType()) == 0) {
                count++;
            }
        }
        return count;
    }


    private void enrichVisitSummary(Map<String, Object> result,
                                    List<Long> orderedAreaIds,
                                    Map<Long, LargeScenicArea> areaMap,
                                    Map<String, Double> preferenceWeights) {
        List<SmallScenicSpot> allSpots = smallScenicSpotRepository == null
                ? Collections.emptyList()
                : smallScenicSpotRepository.findAll();
        Map<Long, List<SmallScenicSpot>> spotMap = new HashMap<>();
        for (SmallScenicSpot spot : allSpots) {
            if (spot == null || spot.getLargeAreaId() == null) {
                continue;
            }
            spotMap.computeIfAbsent(spot.getLargeAreaId(), key -> new ArrayList<>()).add(spot);
        }

        Map<Long, InsideRouteSummary> insideRouteSummaryMap = loadInsideRouteSummaries();

        List<Map<String, Object>> visitDetails = new ArrayList<>();
        Set<Long> visited = new LinkedHashSet<>();
        int totalVisitDuration = 0;
        int totalInsideSpotDuration = 0;
        int totalInsideTransitDuration = 0;
        double totalInsideDistance = 0.0;
        double totalTicketCost = 0.0;

        for (Long areaId : orderedAreaIds) {
            if (areaId == null || !visited.add(areaId)) {
                continue;
            }
            LargeScenicArea area = areaMap.get(areaId);
            if (area == null || safeInt(area.getIsAreaType()) != 0) {
                continue;
            }

            List<SmallScenicSpot> scenicSpots = new ArrayList<>();
            for (SmallScenicSpot spot : spotMap.getOrDefault(areaId, Collections.emptyList())) {
                if (safeInt(spot.getIsSpotType()) == 0) {
                    scenicSpots.add(spot);
                }
            }
            scenicSpots.sort((a, b) -> {
                int compare = Double.compare(scoreSpot(b, preferenceWeights), scoreSpot(a, preferenceWeights));
                if (compare != 0) {
                    return compare;
                }
                return Integer.compare(safeInt(b.getVisitingDuration()), safeInt(a.getVisitingDuration()));
            });

            int insideSpotDuration = 0;
            List<Map<String, Object>> recommendedSpots = new ArrayList<>();
            for (SmallScenicSpot spot : scenicSpots) {
                insideSpotDuration += safeInt(spot.getVisitingDuration());
                Map<String, Object> spotInfo = new LinkedHashMap<>();
                spotInfo.put("id", spot.getId());
                spotInfo.put("name", spot.getName());
                spotInfo.put("visitingDuration", safeInt(spot.getVisitingDuration()));
                recommendedSpots.add(spotInfo);
            }

            InsideRouteSummary insideRoute = insideRouteSummaryMap.getOrDefault(areaId, InsideRouteSummary.empty());
            int baseDuration = safeInt(area.getRecommendedVisitDuration());
            int suggestedDuration = Math.max(baseDuration, insideSpotDuration);

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("areaId", area.getId());
            detail.put("areaName", area.getName());
            detail.put("ticketPrice", safeDecimal(area.getPrice()));
            detail.put("baseVisitDuration", baseDuration);
            detail.put("insideSpotDuration", insideSpotDuration);
            detail.put("insideTransitDuration", insideRoute.totalTime);
            detail.put("insideDistance", insideRoute.totalDistance);
            detail.put("suggestedVisitDuration", suggestedDuration);
            detail.put("spotCount", recommendedSpots.size());
            detail.put("recommendedSpots", recommendedSpots);
            visitDetails.add(detail);

            totalVisitDuration += suggestedDuration;
            totalInsideSpotDuration += insideSpotDuration;
            totalInsideTransitDuration += insideRoute.totalTime;
            totalInsideDistance += insideRoute.totalDistance;
            totalTicketCost += safeDecimal(area.getPrice());
        }

        int routeDuration = (int) Math.round(getDouble(result, "totalDuration"));
        double routeDistance = getDouble(result, "totalDistance");
        double transportCost = getDouble(result, "totalCost");

        result.put("visitDetails", visitDetails);
        result.put("totalVisitDuration", totalVisitDuration);
        result.put("totalInsideSpotDuration", totalInsideSpotDuration);
        result.put("totalInsideTransitDuration", totalInsideTransitDuration);
        result.put("totalInsideDistance", totalInsideDistance);
        result.put("totalTicketCost", totalTicketCost);
        result.put("totalTransportCost", transportCost);
        result.put("overallCost", transportCost + totalTicketCost);
        result.put("overallDistance", routeDistance + totalInsideDistance);
        result.put("overallDuration", totalVisitDuration + totalInsideTransitDuration + routeDuration);
    }

    private Map<Long, InsideRouteSummary> loadInsideRouteSummaries() {
        Map<Long, InsideRouteSummary> summaryMap = new HashMap<>();
        if (jdbcTemplate == null) {
            return summaryMap;
        }
        try {
            List<Map<String, Object>> rows;
            try {
                rows = jdbcTemplate.queryForList(
                        "SELECT se.large_area_id AS largeAreaId, " +
                                "COALESCE(SUM(CASE WHEN COALESCE(fs.is_spot_type, 0) = 0 AND COALESCE(ts.is_spot_type, 0) = 0 THEN se.distance ELSE 0 END), 0) AS totalDistance, " +
                                "COALESCE(SUM(CASE WHEN COALESCE(fs.is_spot_type, 0) = 0 AND COALESCE(ts.is_spot_type, 0) = 0 THEN se.time_cost ELSE 0 END), 0) AS totalTime " +
                                "FROM scenic_edge se " +
                                "LEFT JOIN small_scenic_spot fs ON se.from_spot_id = fs.id " +
                                "LEFT JOIN small_scenic_spot ts ON se.to_spot_id = ts.id " +
                                "GROUP BY se.large_area_id"
                );
            } catch (Exception ignored) {
                rows = jdbcTemplate.queryForList(
                        "SELECT large_area_id AS largeAreaId, " +
                                "COALESCE(SUM(distance), 0) AS totalDistance, " +
                                "COALESCE(SUM(time_cost), 0) AS totalTime " +
                                "FROM scenic_edge GROUP BY large_area_id"
                );
            }
            for (Map<String, Object> row : rows) {
                Long areaId = toLong(row.get("largeAreaId"));
                if (areaId == null) {
                    continue;
                }
                double distance = getNumber(row.get("totalDistance"));
                int time = (int) Math.round(getNumber(row.get("totalTime")));
                summaryMap.put(areaId, new InsideRouteSummary(distance, time));
            }
        } catch (Exception ignored) {
            return summaryMap;
        }
        return summaryMap;
    }

    private double scoreSpot(SmallScenicSpot spot, Map<String, Double> weights) {
        double score = 0.0;
        score += safeDecimal(spot.getNatureScore()) * getWeight(weights, "nature");
        score += safeDecimal(spot.getCultureScore()) * getWeight(weights, "culture");
        score += safeDecimal(spot.getPhotographyScore()) * getWeight(weights, "photography");
        score += safeDecimal(spot.getFamilyFriendlyScore()) * getWeight(weights, "familyFriendly");
        score += safeDecimal(spot.getElderlyFriendlyScore()) * getWeight(weights, "elderlyFriendly");
        score += safeDecimal(spot.getRestConvenienceScore()) * getWeight(weights, "restroomConvenience");
        score -= safeInt(spot.getIntensityLevel()) * getWeight(weights, "intensity") * 0.5;
        score -= safeInt(spot.getQueueLevel()) * getWeight(weights, "crowd") * 0.4;
        if (score <= 0.0) {
            return safeDecimal(spot.getCultureScore())
                    + safeDecimal(spot.getPhotographyScore())
                    + safeDecimal(spot.getNatureScore());
        }
        return score;
    }

    private double transitionPenalty(Map<String, Object> step, String mode) {
        double distance = getDouble(step, "totalDistance");
        double duration = getDouble(step, "totalDuration");
        double cost = getDouble(step, "totalCost");
        if (MODE_DISTANCE.equals(mode)) {
            return distance / 1000.0 + duration / 30.0 + cost / 20.0;
        }
        if (MODE_DURATION.equals(mode)) {
            return duration / 10.0 + distance / 3000.0 + cost / 20.0;
        }
        return duration / 12.0 + distance / 2500.0 + cost / 12.0;
    }

    private String normalizeWeightType(String mode) {
        if (mode == null) {
            return MODE_DURATION;
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if (MODE_DISTANCE.equals(normalized)) {
            return MODE_DISTANCE;
        }
        if (MODE_PERSONALIZED.equals(normalized)) {
            return MODE_PERSONALIZED;
        }
        return MODE_DURATION;
    }

    private String inferTransportMode(String description) {
        String text = defaultText(description);
        if (text.contains("步行")) {
            return "WALK";
        }
        if (text.contains("摆渡") || text.contains("接驳") || text.contains("班车")) {
            return "SHUTTLE";
        }
        if (text.contains("索道") || text.contains("缆车")) {
            return "CABLEWAY";
        }
        if (text.contains("公路") || text.contains("驾车") || text.contains("车行")) {
            return "DRIVE";
        }
        return "ROAD";
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractPathIds(Map<String, Object> result) {
        Object path = result.get("path");
        return path instanceof List ? (List<Long>) path : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSegmentDetails(Map<String, Object> result) {
        Object segmentDetails = result.get("segmentDetails");
        return segmentDetails instanceof List ? (List<Map<String, Object>>) segmentDetails : Collections.emptyList();
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

    private double getWeight(Map<String, Double> weights, String key) {
        if (weights == null) {
            return 0.0;
        }
        return weights.getOrDefault(key, 0.0);
    }

    private String defaultText(String value) {
        return value == null || value.trim().isEmpty() ? "暂无说明" : value;
    }

    private double safeDecimal(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static class Edge {
        Long from;
        Long to;
        double weight;
        ScenicAreaEdge rawEdge;

        Edge(Long from, Long to, double weight, ScenicAreaEdge rawEdge) {
            this.from = from;
            this.to = to;
            this.weight = weight;
            this.rawEdge = rawEdge;
        }
    }

    private static class InsideRouteSummary {
        private final double totalDistance;
        private final int totalTime;

        private InsideRouteSummary(double totalDistance, int totalTime) {
            this.totalDistance = totalDistance;
            this.totalTime = totalTime;
        }

        private static InsideRouteSummary empty() {
            return new InsideRouteSummary(0.0, 0);
        }
    }

    private static class Node {
        Long id;
        double distance;

        Node(Long id, double distance) {
            this.id = id;
            this.distance = distance;
        }
    }
}
