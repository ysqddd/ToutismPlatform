package org.example.toutismplatform.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${deepseek.api-key:${DEEPSEEK_API_KEY:}}") String apiKey,
            @Value("${deepseek.base-url:https://api.deepseek.com/v1}") String baseUrl,
            @Value("${deepseek.model:deepseek-chat}") String modelName,
            @Value("${deepseek.temperature:0.5}") double temperature,
            @Value("${deepseek.timeout:60s}") Duration timeout) {
        return new DeepSeekChatLanguageModel(apiKey, baseUrl, modelName, temperature, timeout, null, null);
    }
}
