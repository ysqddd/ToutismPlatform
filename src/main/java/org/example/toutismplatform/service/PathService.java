package org.example.toutismplatform.service;

import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.entity.ScenicAreaEdge;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.example.toutismplatform.repository.ScenicAreaEdgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PathService {
    @Autowired
    private LargeScenicAreaRepository largeScenicAreaRepository;
    @Autowired
    private ScenicAreaEdgeRepository scenicAreaEdgeRepository;

    // 使用Dijkstra算法计算最短路径
    public Map<String, Object> calculateShortestPath(Long startAreaId, Long endAreaId, String weightType) {
        // 获取所有大景区
        List<LargeScenicArea> areas = largeScenicAreaRepository.findAll();
        // 获取所有边
        List<ScenicAreaEdge> edges = scenicAreaEdgeRepository.findAll();

        // 构建图
        Map<Long, List<Edge>> graph = new HashMap<>();
        for (LargeScenicArea area : areas) {
            graph.put(area.getId(), new ArrayList<>());
        }

        // 添加边
        for (ScenicAreaEdge edge : edges) {
            double weight = "distance".equals(weightType) ? edge.getDistance().doubleValue() : edge.getDuration();
            graph.get(edge.getFromAreaId()).add(new Edge(edge.getToAreaId(), weight));
            // 添加反向边（假设路径是双向的）
            graph.get(edge.getToAreaId()).add(new Edge(edge.getFromAreaId(), weight));
        }

        // 执行Dijkstra算法
        Map<Long, Double> distances = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(node -> node.distance));

        // 初始化距离
        for (LargeScenicArea area : areas) {
            distances.put(area.getId(), Double.MAX_VALUE);
            previous.put(area.getId(), null);
        }
        distances.put(startAreaId, 0.0);
        pq.offer(new Node(startAreaId, 0.0));

        // Dijkstra算法主循环
        while (!pq.isEmpty()) {
            Node current = pq.poll();
            Long currentId = current.id;
            double currentDistance = current.distance;

            if (currentDistance > distances.get(currentId)) {
                continue;
            }

            if (currentId.equals(endAreaId)) {
                break;
            }

            for (Edge edge : graph.get(currentId)) {
                double newDistance = currentDistance + edge.weight;
                if (newDistance < distances.get(edge.to)) {
                    distances.put(edge.to, newDistance);
                    previous.put(edge.to, currentId);
                    pq.offer(new Node(edge.to, newDistance));
                }
            }
        }

        // 构建路径
        List<Long> path = new ArrayList<>();
        Long current = endAreaId;
        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }
        Collections.reverse(path);

        // 构建响应
        Map<String, Object> result = new HashMap<>();
        result.put("path", path);
        result.put("totalWeight", distances.get(endAreaId));
        result.put("weightType", weightType);

        // 获取路径中的景区名称
        List<Map<String, Object>> pathDetails = new ArrayList<>();
        for (Long areaId : path) {
            LargeScenicArea area = largeScenicAreaRepository.findById(areaId).orElse(null);
            if (area != null) {
                Map<String, Object> areaInfo = new HashMap<>();
                areaInfo.put("id", area.getId());
                areaInfo.put("name", area.getName());
                pathDetails.add(areaInfo);
            }
        }
        result.put("pathDetails", pathDetails);

        return result;
    }

    // 内部类：边
    private static class Edge {
        Long to;
        double weight;

        Edge(Long to, double weight) {
            this.to = to;
            this.weight = weight;
        }
    }

    // 内部类：优先队列节点
    private static class Node {
        Long id;
        double distance;

        Node(Long id, double distance) {
            this.id = id;
            this.distance = distance;
        }
    }
}
