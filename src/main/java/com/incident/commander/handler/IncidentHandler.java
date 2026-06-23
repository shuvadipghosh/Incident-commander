package com.incident.commander.handler;

import com.incident.commander.domain.IncidentContext;
import com.incident.commander.domain.IncidentResult;
import com.incident.commander.domain.IncidentType;

public interface IncidentHandler {
    /**
     * Returns the incident type this handler is responsible for.
     */
    IncidentType supports();

    /**
     * Process the incident and return a result.
     */
    IncidentResult handle(IncidentContext context);
}
