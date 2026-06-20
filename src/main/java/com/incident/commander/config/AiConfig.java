package com.incident.commander.config;

import com.incident.commander.MCP.MCPController.LocationController;
import com.incident.commander.MCP.MCPController.WeatherController;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Autowired
    LocationController locationController;
    @Autowired
    WeatherController weatherController;

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultTools(locationController,
                        weatherController)
                .build();
    }


}
