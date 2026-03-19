package org.example.toutismplatform.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.entity.SmallScenicSpot;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.example.toutismplatform.repository.SmallScenicSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
        // 检查是否是路径规划问题
        if (isPathPlanningQuery(query)) {
            return handlePathPlanningQuery(query);
        }
        
        // 查询相关景点信息
        List<LargeScenicArea> largeAreas = largeScenicAreaRepository.findAll();
        List<SmallScenicSpot> smallSpots = smallScenicSpotRepository.findAll();
        
        // 构建景点信息上下文
        StringBuilder context = new StringBuilder();
        context.append("你是一名深知景点内容的导游，负责回答游客关于景点的问题。\n\n");
        context.append("重要声明：以下所有景区都是虚构的，仅用于系统演示和测试，请勿与现实中的任何景区进行关联或对比。\n\n");
        context.append("景点信息：\n");
        context.append("重要提示：所有收费景区都需要通过免费的公共大门（如东大门、西大门、北大门等）进入。这些公共大门是游客进入景区的必经之路，不收取任何费用。\n\n");
        
        // 添加大景区信息
        for (LargeScenicArea area : largeAreas) {
            context.append("大景区：").append(area.getName()).append("\n");
            context.append("描述：").append(area.getDescription() != null ? area.getDescription() : "暂无描述").append("\n");
            context.append("位置：").append(area.getLocation() != null ? area.getLocation() : "暂无位置信息").append("\n");
            context.append("开放时间：").append(area.getOpeningHours() != null ? area.getOpeningHours() : "暂无开放时间").append("\n");
            // 如果是公共措施，显示为免费；否则显示实际价格
            if (area.isPublicFacility()) {
                context.append("价格：免费（公共设施，不收取门票费用）\n");
            } else {
                context.append("价格：").append(area.getPrice()).append("元\n");
            }
            context.append("标签：").append(area.getTags() != null ? area.getTags() : "暂无标签").append("\n");
            if (area.isPublicFacility()) {
                context.append("类型：公共入口设施（游客可通过此门进入收费景区）\n");
            }
            context.append("\n");
        }
        
        // 添加小景点信息
        for (SmallScenicSpot spot : smallSpots) {
            context.append("小景点：").append(spot.getName()).append("\n");
            context.append("描述：").append(spot.getDescription() != null ? spot.getDescription() : "暂无描述").append("\n");
            context.append("建议游览时长：").append(spot.getVisitingDuration()).append("分钟\n");
            context.append("标签：").append(spot.getTags() != null ? spot.getTags() : "暂无标签").append("\n\n");
        }
        
        // 如果没有景点信息，添加提示
        if (largeAreas.isEmpty() && smallSpots.isEmpty()) {
            context.append("目前暂无景点信息，请提供其他问题。\n\n");
        }
        
        // 构建查询
        String enhancedQuery = context.toString() + "游客问题：" + query + "\n\n请以导游的身份回答，注意：\n1. 提及公共大门时强调是免费进入的\n2. 说明游客需要通过公共大门进入收费景区\n3. 不要重复提及公共大门的价格（因为它们是免费的）\n4. 只关注景点内容，不要提及任何编程相关信息\n5. 明确这些景区都是虚构的，不要与现实中的景区（如九寨沟、黄龙等）进行关联\n6. 如果游客问及现实中的景区，礼貌地说明我们这里介绍的都是虚构的演示景区\n7. 专注于介绍本系统中的虚构景区特色和服务";
        return ollamaChatModel.generate(enhancedQuery);
    }
    
    // 判断是否是路径规划问题
    private boolean isPathPlanningQuery(String query) {
        query = query.toLowerCase();
        return query.contains("路线") || query.contains("路径") || query.contains("怎么去") || query.contains("从...到") || query.contains("从" ) && query.contains("到");
    }
    
    // 处理路径规划问题
    private String handlePathPlanningQuery(String query) {
        // 提取起点和终点景区名称
        Map<String, String> locations = extractLocations(query);
        String startName = locations.get("start");
        String endName = locations.get("end");
        
        if (startName == null || endName == null) {
            return "请明确告诉我起点和终点景区的名称，我可以为您规划最优路径。";
        }
        
        // 查找景区ID
        Long startId = findAreaIdByName(startName);
        Long endId = findAreaIdByName(endName);
        
        if (startId == null) {
            return "抱歉，我找不到名为\"" + startName + "\"的景区，请检查名称是否正确。";
        }
        
        if (endId == null) {
            return "抱歉，我找不到名为\"" + endName + "\"的景区，请检查名称是否正确。";
        }
        
        // 计算最短距离路径
        Map<String, Object> distancePath = pathService.calculateShortestPath(startId, endId, "distance");
        // 计算最短时间路径
        Map<String, Object> timePath = pathService.calculateShortestPath(startId, endId, "duration");
        
        // 构建路径规划回答
        return buildPathAnswer(startName, endName, distancePath, timePath);
    }
    
    // 提取起点和终点景区名称
    private Map<String, String> extractLocations(String query) {
        Map<String, String> locations = new HashMap<>();
        
        // 简单的正则表达式，匹配"从X到Y"的模式
        Pattern pattern = Pattern.compile("从([^到]+)到([^，。]+)");
        Matcher matcher = pattern.matcher(query);
        
        if (matcher.find()) {
            locations.put("start", matcher.group(1).trim());
            locations.put("end", matcher.group(2).trim());
        } else {
            // 尝试其他模式
            // 这里可以添加更多的模式匹配逻辑
        }
        
        return locations;
    }
    
    // 根据名称查找景区ID
    private Long findAreaIdByName(String name) {
        List<LargeScenicArea> areas = largeScenicAreaRepository.findAll();
        for (LargeScenicArea area : areas) {
            if (area.getName().contains(name) || name.contains(area.getName())) {
                return area.getId();
            }
        }
        return null;
    }
    
    // 构建路径规划回答
    private String buildPathAnswer(String startName, String endName, Map<String, Object> distancePath, Map<String, Object> timePath) {
        StringBuilder answer = new StringBuilder();
        answer.append("从\"").append(startName).append("\"到\"").append(endName).append("\"的最优路径规划：\n\n");
        
        // 最短距离路径
        List<Map<String, Object>> distancePathDetails = (List<Map<String, Object>>) distancePath.get("pathDetails");
        double totalDistance = (double) distancePath.get("totalWeight");
        
        answer.append("最短距离路线：\n");
        for (int i = 0; i < distancePathDetails.size(); i++) {
            Map<String, Object> area = distancePathDetails.get(i);
            answer.append(i + 1).append(". ").append(area.get("name")).append("\n");
        }
        answer.append("总距离：").append(String.format("%.1f", totalDistance)).append("米\n\n");
        
        // 最短时间路径
        List<Map<String, Object>> timePathDetails = (List<Map<String, Object>>) timePath.get("pathDetails");
        double totalTime = (double) timePath.get("totalWeight");
        
        answer.append("最短时间路线：\n");
        for (int i = 0; i < timePathDetails.size(); i++) {
            Map<String, Object> area = timePathDetails.get(i);
            answer.append(i + 1).append(". ").append(area.get("name")).append("\n");
        }
        answer.append("总时间：").append(String.format("%.0f", totalTime)).append("分钟\n");
        
        return answer.toString();
    }
}
