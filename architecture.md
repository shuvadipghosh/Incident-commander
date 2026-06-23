# Roadside Assistance (RSA) System Architecture

This document presents the system architecture of the Incident Commander platform. It covers the high-level components, the detailed request-response pipeline, and the sequence of interactions that occur when a roadside emergency is reported.

---

## 1. High-Level Component Architecture

The diagram below maps the interaction between the Angular Single-Page Application (SPA), the Spring Boot backend, the Qdrant Vector database, the LLM service (Ollama), and external APIs (OpenStreetMap Overpass and Open-Meteo).

```mermaid
graph TD
    %% Styling
    classDef client fill:#1e293b,stroke:#3b82f6,stroke-width:2px,color:#fff;
    classDef server fill:#14532d,stroke:#22c55e,stroke-width:2px,color:#fff;
    classDef database fill:#1e1b4b,stroke:#6366f1,stroke-width:2px,color:#fff;
    classDef external fill:#7c2d12,stroke:#f97316,stroke-width:2px,color:#fff;

    subgraph Client ["Client Layer (Angular SPA)"]
        UI["Angular UI Dashboard (5 Screens)"]:::client
        WS_Client["Native WebSocket Client (STOMP)"]:::client
        Geo_Client["HTML Geolocation API"]:::client
    end

    subgraph Backend ["Backend Layer (Spring Boot)"]
        Controller["IncidentController (REST / WS Endpoint)"]:::server
        Classifier["ClassifierAgent (LLM Classification)"]:::server
        Pipeline["DefaultIncidentPipeline (Router)"]:::server
        
        subgraph Handlers ["Incident Handlers (Strategy Pattern)"]
            FuelH["OutOfFuelHandler"]:::server
            SmokeH["VehicleSmokeHandler"]:::server
            BatteryH["DeadBatteryHandler"]:::server
            TyreH["FlatTyreHandler"]:::server
            AccidentH["AccidentHandler"]:::server
            MedicalH["MedicalEmergencyHandler"]:::server
            MechanicH["NearbyMechanicHandler"]:::server
            TowH["TowRequestHandler"]:::server
        end
        
        Knowledge["KnowledgeService (RAG Component)"]:::server
        IncidentAgent["DefaultIncidentAgent (Spring AI Orchestrator)"]:::server
        
        subgraph Tools ["AI Tools Registry"]
            LocTools["LocationTools (OSM Overpass API)"]:::server
            WeathTools["WeatherTools (Open-Meteo API)"]:::server
            DispTools["Tow & Mechanic Dispatch Tools"]:::server
        end
    end

    subgraph Persistence ["Persistence & LLM Layer"]
        Qdrant[("Qdrant Vector DB (Rules RAG)")]:::database
        Ollama["Ollama / LLM Engine (Llama 3 / Qwen)"]:::database
    end

    subgraph ExternalServices ["External Systems"]
        OSM["OpenStreetMap Overpass API"]:::external
        OpenMeteo["Open-Meteo Weather API"]:::external
    end

    %% Connections
    UI --> Geo_Client
    WS_Client <-->|Bi-directional STOMP / WS| Controller
    Controller --> Classifier
    Classifier -->|Determines Type| Pipeline
    Pipeline -->|Routes Incident| Handlers
    
    Handlers -->|Fetch Domain Context| Knowledge
    Knowledge <-->|Semantic Search & Filter| Qdrant
    
    Handlers -->|Delegate Analysis| IncidentAgent
    IncidentAgent <-->|Invoke Prompt + Context| Ollama
    IncidentAgent -->|Execute Tool Calls| Tools
    
    LocTools <-->|Query Nearby Amenities| OSM
    WeathTools <-->|Query Current Weather| OpenMeteo
    DispTools -->|Mock Dispatch Service| UI
```

---

## 2. Sequence Diagram: End-to-End Processing

The sequence diagram below displays the detailed interaction sequence for an incident request, highlighting how progress updates (`CLASSIFYING`, `CLASSIFIED`, `TOOL_CALL`, `COMPLETE`) are pushed back to the client in real-time.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as Angular SPA
    participant Controller as IncidentController
    participant Classifier as ClassifierAgent
    participant Pipeline as IncidentPipeline
    participant Handler as OutOfFuelHandler
    participant RAG as KnowledgeService
    participant VectorDB as Qdrant DB
    participant Agent as DefaultIncidentAgent
    participant ExtAPI as External APIs (OSM / Meteo)
    participant LLM as Ollama (Llama 3)

    User->>UI: Enter phone, select location, type "Out of gas"
    UI->>UI: Add local "AI Classifying" timeline event
    UI->>Controller: WebSocket SEND: /app/incident.analyze (Payload)
    
    Note over Controller: Start Async Pipeline Execution
    Controller->>UI: WebSocket MESSAGE: {"status": "CLASSIFYING"} (Ignored if local exists)
    
    Controller->>Classifier: Determine incident type from description
    Classifier->>LLM: Categorize text: "Out of gas"
    LLM-->>Classifier: Return "OUT_OF_FUEL"
    Classifier-->>Controller: Return OUT_OF_FUEL
    
    Controller->>UI: WebSocket MESSAGE: {"status": "CLASSIFIED", "type": "OUT_OF_FUEL"}
    UI->>UI: Render checkmark for "Classified as Out of Fuel"
    
    Controller->>Pipeline: Route incident to handler
    Pipeline->>Handler: handle(session, context)
    
    Handler->>RAG: Fetch rules for OUT_OF_FUEL
    RAG->>VectorDB: Query similarity search (filter: incidentType == OUT_OF_FUEL)
    VectorDB-->>RAG: Return markdown rules context
    RAG-->>Handler: Return rules
    
    Handler->>Agent: analyze(session, rules, description, coords)
    
    Note over Agent: LLM decides to check nearby fuel pumps & weather
    Agent->>Controller: Notify Tool Call
    Controller->>UI: WebSocket MESSAGE: {"status": "TOOL_CALL", "tool": "findNearestFuelPump"}
    UI->>UI: Render "Calling: Find Nearest Fuel Pump..."
    
    Agent->>ExtAPI: Call Overpass API (coordinates)
    ExtAPI-->>Agent: Return nearby fuel stations
    
    Agent->>Controller: Notify Tool Call
    Controller->>UI: WebSocket MESSAGE: {"status": "TOOL_CALL", "tool": "getWeather"}
    UI->>UI: Render "Calling: Get Weather..."
    
    Agent->>ExtAPI: Call Open-Meteo API (coordinates)
    ExtAPI-->>Agent: Return "Cloudy, 21.6°C"
    
    Agent->>LLM: Prompt LLM with rules + tools outputs + user description
    LLM-->>Agent: Return JSON Assistance Plan (Recommendations + Dispatches)
    Agent-->>Handler: Return Action Plan
    Handler-->>Controller: Return final payload
    
    Controller->>UI: WebSocket MESSAGE: {"status": "COMPLETE", "result": {...}}
    UI->>UI: Render checkmark for "Analysis complete"
    UI->>UI: Transition to Results Screen (Show map, weather, eta, action buttons)
    
    UI->>Controller: WebSocket UNSUBSCRIBE / Disconnect
```

---

## 3. High-Quality Design Decisions

### Real-Time Event Streaming
- **WebSocket STOMP Channel**: Instead of long-polling, a native bi-directional WebSocket connection communicates events instantly.
- **Granular Progress Timeline**: The UI maps backend stages (`CLASSIFYING` -> `CLASSIFIED` -> `TOOL_CALL` -> `COMPLETE`) straight to interactive checkmarks, enhancing perceived speed.

### Extensible Strategy Pattern
- **Decoupled Handlers**: Handlers inherit from a common base and register themselves to the pipeline dynamically. Adding a new incident type only requires writing a single subclass.

### AI RAG (Retrieval Augmented Generation)
- **Qdrant Embedding Store**: Markdown rules are vectorized using Ollama embeddings and loaded at startup. Handlers fetch matching rules and inject them directly into LLM prompts.
- **Dynamic Tool Execution**: The LLM autonomously decides when to check weather or location details via OpenStreetMap/Open-Meteo APIs, adapting recommendations to real-world contexts.

---

## 4. Data Flow Diagrams (DFD)

### Level 0 DFD: System Context Diagram

The Level 0 Context Diagram shows the boundaries of the Incident Commander system, highlighting key external actors (Driver/User) and external API dependencies.

```mermaid
graph TD
    %% Styling
    classDef actor fill:#1e293b,stroke:#3b82f6,stroke-width:2px,color:#fff;
    classDef system fill:#14532d,stroke:#22c55e,stroke-width:2px,color:#fff;
    classDef external fill:#7c2d12,stroke:#f97316,stroke-width:2px,color:#fff;

    User([User / Driver]):::actor
    System[[Incident Commander System]]:::system
    Ollama[[Ollama LLM Server]]:::external
    OSM[[OpenStreetMap API]]:::external
    Meteo[[Open-Meteo API]]:::external

    User -->|1. Submit Phone, Geolocation & Description| System
    System -->|2. Return Timeline Updates & JSON Assistance Plan| User
    
    System -->|3. Query Text for Classification / Reasoning| Ollama
    Ollama -->|4. Return Category / Plan Recommendation JSON| System

    System -->|5. Coordinate Coordinates| OSM
    OSM -->|6. Return Nearby Fuel / Mechanic Services| System

    System -->|7. Coordinates| Meteo
    Meteo -->|8. Return Current Weather| System
```

### Level 1 DFD: Component Data Flow Diagram

The Level 1 DFD describes the internal processes of the application, showing how data transitions from raw user input into structured recommendations by querying vector stores and calling external APIs.

```mermaid
graph TD
    %% Styling
    classDef actor fill:#1e293b,stroke:#3b82f6,stroke-width:2px,color:#fff;
    classDef process fill:#14532d,stroke:#22c55e,stroke-width:2px,color:#fff;
    classDef datastore fill:#1e1b4b,stroke:#6366f1,stroke-width:2px,color:#fff;

    %% Entities
    User([User / Driver]):::actor
    OSM([OpenStreetMap API]):::actor
    Meteo([Open-Meteo API]):::actor
    Ollama([Ollama LLM Server]):::actor
    Qdrant[(Qdrant Vector DB)]:::datastore

    %% Processes
    P1[[1.0 Receive & Authenticate Session]]:::process
    P2[[2.0 Classify Incident]]:::process
    P3[[3.0 Route to Typed Handler]]:::process
    P4[[4.0 Retrieve Domain Rules RAG]]:::process
    P5[[5.0 Fetch Location & Weather Tools]]:::process
    P6[[6.0 Generate Plan Recommendations]]:::process

    %% Data Stores
    D1[(System Session Cache)]:::datastore

    %% Flows
    User -->|Phone & Description| P1
    P1 -->|Session Registry| D1
    P1 -->|Incident Description| P2
    
    P2 -->|Prompt Content| Ollama
    Ollama -->|Incident Type Hint| P2
    P2 -->|Classified Type| P3
    
    P3 -->|Incident Details| P4
    P4 -->|Semantic Search Filter| Qdrant
    Qdrant -->|Safety Rules context| P4
    P4 -->|Contextual Rules| P6

    P3 -->|Coordinates| P5
    P5 -->|Query Amenities| OSM
    OSM -->|Nearby Services| P5
    P5 -->|Query Temperature| Meteo
    Meteo -->|Weather Info| P5
    P5 -->|Real-Time Context| P6

    P6 -->|Compile Rules + Tools Context| Ollama
    Ollama -->|Assistance Plan JSON| P6
    P6 -->|Final Plan & Dispatch Details| User
```
