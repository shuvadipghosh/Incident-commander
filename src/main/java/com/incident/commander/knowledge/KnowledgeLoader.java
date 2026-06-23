package com.incident.commander.knowledge;

import com.incident.commander.domain.IncidentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class KnowledgeLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeLoader.class);

    private final VectorStore vectorStore;
    private final KnowledgeService knowledgeService;

    public KnowledgeLoader(VectorStore vectorStore, KnowledgeService knowledgeService) {
        this.vectorStore = vectorStore;
        this.knowledgeService = knowledgeService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadKnowledge() {
        log.info("Loading RSA knowledge base into Qdrant...");
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:knowledge/*.md");

            if (resources.length == 0) {
                log.warn("No knowledge files found in classpath:knowledge/");
                return;
            }

            List<Document> allDocs = new ArrayList<>();
            TokenTextSplitter splitter = new TokenTextSplitter(512, 50, 5, 10000, true);

            for (Resource resource : resources) {
                try {
                    String filename = resource.getFilename();
                    String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    String incidentTypeTag = resolveIncidentType(filename);

                    List<Document> chunks = splitter.split(
                            List.of(new Document(content, Map.of(
                                    "source", filename,
                                    "incidentType", incidentTypeTag,
                                    "type", "rule"
                            )))
                    );
                    allDocs.addAll(chunks);
                    log.info("Loaded knowledge file: {} ({} chunks, type={})", filename, chunks.size(), incidentTypeTag);
                } catch (IOException e) {
                    log.warn("Failed to load knowledge file: {}", resource.getFilename(), e);
                }
            }

            if (!allDocs.isEmpty()) {
                vectorStore.add(allDocs);
                knowledgeService.markAvailable();
                log.info("Successfully loaded {} document chunks into Qdrant", allDocs.size());
            }

        } catch (Exception e) {
            log.error("Failed to load knowledge base. Continuing without RAG. Error: {}", e.getMessage());
        }
    }

    private String resolveIncidentType(String filename) {
        if (filename == null) return "GLOBAL";
        String base = filename.replace(".md", "").toUpperCase().replace("-", "_");
        for (IncidentType type : IncidentType.values()) {
            if (base.contains(type.name())) return type.name();
        }
        return "GLOBAL";
    }
}
