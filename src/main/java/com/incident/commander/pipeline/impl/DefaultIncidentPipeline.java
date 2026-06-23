package com.incident.commander.pipeline.impl;

import com.incident.commander.domain.IncidentContext;
import com.incident.commander.domain.IncidentResult;
import com.incident.commander.domain.IncidentType;
import com.incident.commander.handler.IncidentHandler;
import com.incident.commander.pipeline.IncidentPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DefaultIncidentPipeline implements IncidentPipeline {

    private static final Logger log = LoggerFactory.getLogger(DefaultIncidentPipeline.class);

    private final Map<IncidentType, IncidentHandler> handlerRegistry;

    public DefaultIncidentPipeline(List<IncidentHandler> handlers) {
        this.handlerRegistry = handlers.stream()
                .collect(Collectors.toMap(IncidentHandler::supports, Function.identity()));
        log.info("Registered {} incident handlers: {}", handlers.size(),
                handlers.stream().map(h -> h.supports().name()).collect(Collectors.joining(", ")));
    }

    @Override
    public IncidentResult process(IncidentContext context) {
        IncidentType type = context.getIncidentType();
        IncidentHandler handler = handlerRegistry.get(type);

        if (handler == null) {
            log.warn("No handler found for incident type: {}. Falling back to UNKNOWN handler.", type);
            handler = handlerRegistry.getOrDefault(IncidentType.UNKNOWN, new FallbackHandler());
        }

        log.info("Routing incident type {} to handler: {}", type, handler.getClass().getSimpleName());
        return handler.handle(context);
    }

    /** Inline fallback when no handler matches */
    private static class FallbackHandler implements IncidentHandler {
        @Override
        public IncidentType supports() {
            return IncidentType.UNKNOWN;
        }

        @Override
        public IncidentResult handle(IncidentContext context) {
            return IncidentResult.builder()
                    .incidentType(IncidentType.UNKNOWN)
                    .summary("We are reviewing your request. A support agent will contact you shortly.")
                    .recommendations(List.of(
                            IncidentResult.RecommendedAction.builder()
                                    .rank(1).action("CONTACT_SUPPORT")
                                    .reason("Incident type could not be determined automatically.")
                                    .build()
                    ))
                    .sessionId(context.getSessionId())
                    .build();
        }
    }
}
