package com.incident.commander.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    // Note: DefaultIncidentAgent builds its own ChatClient per request with selected tools.
    // This global ChatClient is kept for ClassifierAgent and AiService which need no tools.
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
