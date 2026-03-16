package org.example.toutismplatform.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.entity.SmallScenicSpot;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.example.toutismplatform.repository.SmallScenicSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    @Autowired
    private OllamaChatModel ollamaChatModel;
    
    @Autowired
    private LargeScenicAreaRepository largeScenicAreaRepository;
    
    @Autowired
    private SmallScenicSpotRepository smallScenicSpotRepository;

    public String generateAnswer(String query) {
        // 查询相关景点信息
        List<LargeScenicArea> largeAreas = largeScenicAreaRepository.findAll();
        List<SmallScenicSpot> smallSpots = smallScenicSpotRepository.findAll();
        
        // 构建景点信息上下文
        StringBuilder context = new StringBuilder();
        context.append("你是一名深知景点内容的导游，负责回答游客关于景点的问题。\n\n");
        context.append("景点信息：\n");
        
        // 添加大景区信息
        for (LargeScenicArea area : largeAreas) {
            context.append("大景区：").append(area.getName()).append("\n");
            context.append("描述：").append(area.getDescription() != null ? area.getDescription() : "暂无描述").append("\n");
            context.append("位置：").append(area.getLocation() != null ? area.getLocation() : "暂无位置信息").append("\n");
            context.append("开放时间：").append(area.getOpeningHours() != null ? area.getOpeningHours() : "暂无开放时间").append("\n");
            context.append("价格：").append(area.getPrice()).append("元\n");
            context.append("标签：").append(area.getTags() != null ? area.getTags() : "暂无标签").append("\n\n");
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
        String enhancedQuery = context.toString() + "游客问题：" + query + "\n\n请以导游的身份回答，不要提及任何编程相关信息，只关注景点内容。";
        return ollamaChatModel.generate(enhancedQuery);
    }
}
