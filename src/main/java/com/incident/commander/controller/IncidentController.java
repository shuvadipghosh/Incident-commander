
package com.incident.commander.controller;

import com.incident.commander.Service.AiService;
import com.incident.commander.dto.IncidentRequest;
import com.incident.commander.dto.IncidentResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/incident")
public class IncidentController {

    @Autowired
    AiService aiService;

    @PostMapping("/analyze")
    public ResponseEntity<Object> analyze(@RequestBody IncidentRequest request) {

        IncidentResponseDTO aiResponse = null;
        try {
            aiResponse = aiService.ask(request);
        } catch (IOException e) {
           return ResponseEntity.status(500).body(Map.of("error", "Failed to process the request: " + e.getMessage()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(aiResponse);
    }
}
