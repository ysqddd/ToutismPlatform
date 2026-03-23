package org.example.toutismplatform.service;

import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.entity.ScenicAreaEdge;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.example.toutismplatform.repository.ScenicAreaEdgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class PathService {

    @Autowired
    private LargeScenicAreaRepository largeScenicAreaRepository;

    @Autowired
    private ScenicAreaEdgeRepository scenicAreaEdgeRepository;

    /**
     按距离或时间计算最短路径
     */
    public Map<String, Object> calculateShortestPath(Long startAreaId, Long endAreaId, String weightType) {
        return calculateClassicPath(startAreaId, endAreaId, weightType);
    }

    /**
     根据用户在自然语言里表达的偏好进行综合加权路径规划
     */
    public Map<String, Object> calculatePersonalizedPath(Long startAreaId,
                                                         Long endAreaId,
                                                         Map<String, Double> preferenceWeights) {
        List<LargeScenicArea> areas = largeScenicAreaRepository.findAll();
        List<ScenicAreaEdge> edges = scenicAreaEdgeRepository.findAll();

        Map<Long, LargeScenicArea> areaMap = buildAreaMap(areas);
        if (!areaMap.containsKey(startAreaId) || !areaMap.containsKey(endAreaId)) {
            return buildFailureResult("起点或终点不存在，无法规划路线。", "personalized");
        }

        Map<Long, List<Edge>> graph = buildPersonalizedGraph(edges, areaMap, preferenceWeights);
        if (!graph.containsKey(startAreaId) || !graph.containsKey(endAreaId)) {
            return buildFailureResult("图数据不完整，无法规划路线。", "personalized");
        }

        return runDijkstra(startAreaId, endAreaId, graph, areaMap, "personalized", preferenceWeights);
    }

    private Map<String, Object> calculateClassicPath(Long startAreaId, Long endAreaId, String weightType) {
        List<LargeScenicArea> areas = largeScenicAreaRepository.findAll();
        List<ScenicAreaEdge> edges = scenicAreaEdgeRepository.findAll();

        Map<Long, LargeScenicArea> areaMap = buildAreaMap(areas);
        if (!areaMap.containsKey(startAreaId) || !areaMap.containsKey(endAreaId)) {
            return buildFailureResult("起点或终点不存在，无法规划路线。", weightType);
        }

        Map<Long, List<Edge>> graph = new HashMap<>();
        for (LargeScenicArea area : areas) {
            graph.put(area.getId(), new ArrayList<>());
        }

        for (ScenicAreaEdge edge : edges) {
            graph.computeIfAbsent(edge.getFromAreaId(), key -> new ArrayList<>());
            graph.computeIfAbsent(edge.getToAreaId(), key -> new ArrayList<>());

            double weight = "distance".equalsIgnoreCase(weightType)
                    ? safeDecimal(edge.getDistance())
                    : safeInt(edge.getDuration());

            graph.get(edge.getFromAreaId()).add(new Edge(edge.getFromAreaId(), edge.getToAreaId(), weight, edge));
            graph.get(edge.getToAreaId()).add(new Edge(edge.getToAreaId(), edge.getFromAreaId(), weight, edge));
        }

        return runDijkstra(startAreaId, endAreaId, graph, areaMap, weightType, null);
    }

    private Map<Long, LargeScenicArea> buildAreaMap(List<LargeScenicArea> areas) {
        Map<Long, LargeScenicArea> areaMap = new HashMap<>();
        for (LargeScenicArea area : areas) {
            areaMap.put(area.getId(), area);
        }
        return areaMap;
    }

    private Map<Long, List<Edge>> buildPersonalizedGraph(List<ScenicAreaEdge> edges,
                                                         Map<Long, LargeScenicArea> areaMap,
                                                         Map<String, Double> preferenceWeights) {
        Map<Long, List<Edge>> graph = new HashMap<>();
        for (Long areaId : areaMap.keySet()) {
            graph.put(areaId, new ArrayList<>());
        }

        for (ScenicAreaEdge edge : edges) {
            graph.computeIfAbsent(edge.getFromAreaId(), key -> new ArrayList<>());
            graph.computeIfAbsent(edge.getToAreaId(), key -> new ArrayList<>());

            LargeScenicArea forwardTarget = areaMap.get(edge.getToAreaId());
            LargeScenicArea reverseTarget = areaMap.get(edge.getFromAreaId());

            double forwardWeight = calculatePersonalizedWeight(edge, forwardTarget, preferenceWeights);
            double reverseWeight = calculatePersonalizedWeight(edge, reverseTarget, preferenceWeights);

            graph.get(edge.getFromAreaId()).add(new Edge(edge.getFromAreaId(), edge.getToAreaId(), forwardWeight, edge));
            graph.get(edge.getToAreaId()).add(new Edge(edge.getToAreaId(), edge.getFromAreaId(), reverseWeight, edge));
        }
        return graph;
    }

    private double calculatePersonalizedWeight(ScenicAreaEdge edge,
                                               LargeScenicArea targetArea,
                                               Map<String, Double> preferenceWeights) {
        double distanceKm = safeDecimal(edge.getDistance()) / 1000.0;
        double durationUnit = safeInt(edge.getDuration()) / 10.0;
        double costUnit = safeDecimal(edge.getCostAmount()) / 10.0;
        double edgeIntensity = safeInt(edge.getIntensityLevel());

        double distanceWeight = getWeight(preferenceWeights, "distance");
        double durationWeight = getWeight(preferenceWeights, "duration");
        double costWeight = getWeight(preferenceWeights, "cost");
        double intensityWeight = getWeight(preferenceWeights, "intensity");
        double crowdWeight = getWeight(preferenceWeights, "crowd");
        double natureWeight = getWeight(preferenceWeights, "nature");
        double cultureWeight = getWeight(preferenceWeights, "culture");
        double photographyWeight = getWeight(preferenceWeights, "photography");
        double elderlyWeight = getWeight(preferenceWeights, "elderlyFriendly");
        double familyWeight = getWeight(preferenceWeights, "familyFriendly");
        double leisureWeight = getWeight(preferenceWeights, "leisure");
        double comfortWeight = getWeight(preferenceWeights, "comfort");
        double foodWeight = getWeight(preferenceWeights, "foodConvenience");
        double restroomWeight = getWeight(preferenceWeights, "restroomConvenience");
        double popularityWeight = getWeight(preferenceWeights, "popularity");

        double basePenalty = 0.0;
        basePenalty += distanceKm * (1.2 + distanceWeight * 2.0);
        basePenalty += durationUnit * (1.0 + durationWeight * 2.0);
        basePenalty += costUnit * (0.8 + costWeight * 2.2);
        basePenalty += edgeIntensity * (0.6 + intensityWeight * 1.8);

        if (targetArea != null) {
            basePenalty += safeInt(targetArea.getCrowdLevel()) * 0.35 * (1.0 + crowdWeight);
            basePenalty += (safeInt(targetArea.getRecommendedVisitDuration()) / 60.0) * 0.15 * (1.0 + durationWeight);
            basePenalty += safeInt(targetArea.getIntensityLevel()) * 0.25 * intensityWeight;
        }

        double reward = 0.0;
        if (targetArea != null) {
            reward += average(edge.getScenicScore(), targetArea.getNatureScore()) * natureWeight;
            reward += safeDecimal(targetArea.getCultureScore()) * cultureWeight;
            reward += safeDecimal(targetArea.getPhotographyScore()) * photographyWeight;
            reward += average(edge.getElderlyFriendlyScore(), targetArea.getElderlyFriendlyScore()) * elderlyWeight;
            reward += safeDecimal(targetArea.getFamilyFriendlyScore()) * familyWeight;
            reward += safeDecimal(targetArea.getLeisureScore()) * leisureWeight;
            reward += safeDecimal(targetArea.getFoodConvenienceScore()) * foodWeight;
            reward += safeDecimal(targetArea.getRestroomConvenienceScore()) * restroomWeight;
            reward += safeDecimal(targetArea.getPopularityScore()) * popularityWeight;
        }
        reward += average(edge.getComfortScore(), targetArea == null ? BigDecimal.ZERO : targetArea.getLeisureScore()) * comfortWeight;

        String transportMode = edge.getTransportMode() == null ? "" : edge.getTransportMode().trim().toUpperCase(Locale.ROOT);
        if ("CABLEWAY".equals(transportMode)) {
            reward += intensityWeight * 0.8;
            reward += elderlyWeight * 0.6;
            basePenalty += costWeight * 0.5;
        } else if ("ROAD".equals(transportMode) || "SHUTTLE".equals(transportMode)) {
            reward += comfortWeight * 0.4;
        } else if ("WALK".equals(transportMode)) {
            basePenalty += intensityWeight * 0.5;
        }

        return Math.max(0.3, basePenalty - reward * 0.9);
    }

    private Map<String, Object> runDijkstra(Long startAreaId,
                                            Long endAreaId,
                                            Map<Long, List<Edge>> graph,
                                            Map<Long, LargeScenicArea> areaMap,
                                            String weightType,
                                            Map<String, Double> preferenceWeights) {
        Map<Long, Double> distances = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        Map<Long, Edge> previousEdges = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(node -> node.distance));

        for (Long areaId : graph.keySet()) {
            distances.put(areaId, Double.MAX_VALUE);
            previous.put(areaId, null);
        }

        distances.put(startAreaId, 0.0);
        pq.offer(new Node(startAreaId, 0.0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (current.distance > distances.getOrDefault(current.id, Double.MAX_VALUE)) {
                continue;
            }
            if (current.id.equals(endAreaId)) {
                break;
            }

            List<Edge> neighbors = graph.getOrDefault(current.id, Collections.emptyList());
            for (Edge edge : neighbors) {
                double newDistance = current.distance + edge.weight;
                if (newDistance < distances.getOrDefault(edge.to, Double.MAX_VALUE)) {
                    distances.put(edge.to, newDistance);
                    previous.put(edge.to, current.id);
                    previousEdges.put(edge.to, edge);
                    pq.offer(new Node(edge.to, newDistance));
                }
            }
        }

        if (distances.getOrDefault(endAreaId, Double.MAX_VALUE) == Double.MAX_VALUE) {
            return buildFailureResult("起点和终点之间暂无可达路径。", weightType);
        }

        List<Long> path = new ArrayList<>();
        Long current = endAreaId;
        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }
        Collections.reverse(path);

        List<Map<String, Object>> pathDetails = new ArrayList<>();
        List<Map<String, Object>> segmentDetails = new ArrayList<>();
        double totalDistance = 0.0;
        int totalDuration = 0;
        double totalCost = 0.0;

        for (Long areaId : path) {
            LargeScenicArea area = areaMap.get(areaId);
            if (area != null) {
                Map<String, Object> areaInfo = new LinkedHashMap<>();
                areaInfo.put("id", area.getId());
                areaInfo.put("name", area.getName());
                areaInfo.put("isAreaType", area.getIsAreaType());
                areaInfo.put("tags", area.getTags());
                pathDetails.add(areaInfo);
            }
        }

        for (int i = 1; i < path.size(); i++) {
            Long toAreaId = path.get(i);
            Edge selectedEdge = previousEdges.get(toAreaId);
            if (selectedEdge == null || selectedEdge.rawEdge == null) {
                continue;
            }
            ScenicAreaEdge rawEdge = selectedEdge.rawEdge;
            LargeScenicArea fromArea = areaMap.get(selectedEdge.from);
            LargeScenicArea toArea = areaMap.get(selectedEdge.to);

            totalDistance += safeDecimal(rawEdge.getDistance());
            totalDuration += safeInt(rawEdge.getDuration());
            totalCost += safeDecimal(rawEdge.getCostAmount());

            Map<String, Object> segmentInfo = new LinkedHashMap<>();
            segmentInfo.put("fromAreaId", selectedEdge.from);
            segmentInfo.put("toAreaId", selectedEdge.to);
            segmentInfo.put("fromName", fromArea == null ? "未知地点" : fromArea.getName());
            segmentInfo.put("toName", toArea == null ? "未知地点" : toArea.getName());
            segmentInfo.put("distance", rawEdge.getDistance());
            segmentInfo.put("duration", rawEdge.getDuration());
            segmentInfo.put("costAmount", rawEdge.getCostAmount());
            segmentInfo.put("transportMode", rawEdge.getTransportMode());
            segmentInfo.put("description", rawEdge.getDescription());
            segmentInfo.put("calculatedWeight", selectedEdge.weight);
            segmentDetails.add(segmentInfo);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("path", path);
        result.put("totalWeight", distances.get(endAreaId));
        result.put("weightType", weightType);
        result.put("pathDetails", pathDetails);
        result.put("segmentDetails", segmentDetails);
        result.put("totalDistance", totalDistance);
        result.put("totalDuration", totalDuration);
        result.put("totalCost", totalCost);
        if (preferenceWeights != null) {
            result.put("preferences", new LinkedHashMap<>(preferenceWeights));
        }
        return result;
    }

    private Map<String, Object> buildFailureResult(String message, String weightType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        result.put("weightType", weightType);
        result.put("path", Collections.emptyList());
        result.put("pathDetails", Collections.emptyList());
        result.put("segmentDetails", Collections.emptyList());
        result.put("totalWeight", Double.MAX_VALUE);
        result.put("totalDistance", 0.0);
        result.put("totalDuration", 0);
        result.put("totalCost", 0.0);
        return result;
    }

    private double getWeight(Map<String, Double> preferenceWeights, String key) {
        if (preferenceWeights == null) {
            return 0.0;
        }
        return preferenceWeights.getOrDefault(key, 0.0);
    }

    private double average(BigDecimal first, BigDecimal second) {
        return (safeDecimal(first) + safeDecimal(second)) / 2.0;
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

    private static class Node {
        Long id;
        double distance;

        Node(Long id, double distance) {
            this.id = id;
            this.distance = distance;
        }
    }
}
