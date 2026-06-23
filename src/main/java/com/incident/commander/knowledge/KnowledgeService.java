package com.incident.commander.knowledge;

import com.incident.commander.domain.IncidentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final VectorStore vectorStore;
    private boolean available = true;

    public KnowledgeService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<String> retrieveRelevantRules(IncidentType type, String description) {
        if (!available) return List.of();

        try {
            String query = type.name() + " " + description;

            // Try type-specific search first
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            List<Document> docs;
            try {
                docs = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(query)
                                .topK(4)
                                .filterExpression(b.in("incidentType", type.name(), "GLOBAL").build())
                                .build()
                );
            } catch (Exception filterEx) {
                // Fallback: no filter
                log.debug("Filtered search failed, falling back to unfiltered: {}", filterEx.getMessage());
                docs = vectorStore.similaritySearch(
                        SearchRequest.builder().query(query).topK(4).build()
                );
            }

            List<String> rules = docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.toList());

            log.debug("Retrieved {} relevant rules for type {} from Qdrant", rules.size(), type);
            return rules;

        } catch (Exception e) {
            log.warn("Qdrant search failed, proceeding without RAG context: {}", e.getMessage());
            available = false;
            return List.of();
        }
    }

    public void markAvailable() {
        this.available = true;
    }
}
