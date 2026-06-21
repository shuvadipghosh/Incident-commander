package com.incident.commander.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.commander.dto.FuelStationDTO;
import com.incident.commander.dto.IncidentRequest;
import com.incident.commander.dto.IncidentResponseDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class AiService {
    @Autowired
    ChatClient chatClient;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    OverpassService overpassService;


    public AiService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

//    public String ask(String prompt) {
//        return chatClient.prompt()
//                .user(prompt)
//                .call()
//                .content();
//    }

    public IncidentResponseDTO ask(IncidentRequest request) throws Exception {
        String systemPrompt = loadPrompt(
                        "incident-command.md");

        String response =
                chatClient.prompt()
                        .system(systemPrompt)
                        .user("""
                            Scenario: %s
                            Description: %s
                            Latitude: %s
                            Longitude: %s
                            """
                                .formatted(
                                        request.getScenario(),
                                        request.getDescription(),
                                        request.getLatitude(),
                                        request.getLongitude()))
                        .call()
                        .content();


        FuelStationDTO fuelStationDTO = overpassService.findNearestFuelStation(request.getLatitude() , request.getLongitude());


        IncidentResponseDTO incidentResponseDTO = objectMapper.readValue(
                response,
                IncidentResponseDTO.class);

        incidentResponseDTO.setNearestFuelPump(fuelStationDTO.getDistanceKm());

        return incidentResponseDTO;
    }


    public String loadPrompt(String fileName)
            throws IOException {

        ClassPathResource resource =
                new ClassPathResource(
                        "static/" + fileName);

        return new String(
                resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }
}
