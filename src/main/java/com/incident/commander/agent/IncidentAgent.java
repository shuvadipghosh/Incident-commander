package com.incident.commander.agent;

import com.incident.commander.domain.IncidentContext;
import com.incident.commander.domain.IncidentResult;

/**
 * AI agent that analyzes an incident context and returns structured recommendations.
 * Implemented by DefaultIncidentAgent which uses Spring AI ChatClient with tool calling.
 */
public interface IncidentAgent {
    IncidentResult analyze(IncidentContext context);
}
