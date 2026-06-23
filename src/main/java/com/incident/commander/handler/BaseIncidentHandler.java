package com.incident.commander.handler;

import com.incident.commander.agent.IncidentAgent;
import com.incident.commander.domain.IncidentContext;
import com.incident.commander.domain.IncidentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseIncidentHandler implements IncidentHandler {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final IncidentAgent incidentAgent;

    protected BaseIncidentHandler(IncidentAgent incidentAgent) {
        this.incidentAgent = incidentAgent;
    }

    @Override
    public IncidentResult handle(IncidentContext context) {
        log.info("Handling {} incident for session {}", supports(), context.getSessionId());
        return incidentAgent.analyze(context);
    }
}
