package com.incident.commander.pipeline;

import com.incident.commander.domain.IncidentContext;
import com.incident.commander.domain.IncidentResult;

public interface IncidentPipeline {
    IncidentResult process(IncidentContext context);
}
