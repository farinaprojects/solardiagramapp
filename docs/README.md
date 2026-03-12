# SolarDiagramApp

SolarDiagramApp is a professional electrical and photovoltaic system
diagramming and analysis platform.

The project started as a diagram editor and is evolving into a full
engineering tool capable of:

- electrical diagram modeling
- photovoltaic system design
- electrical validation
- engineering calculations
- energy flow simulation

The long-term goal is to build a **modern engineering platform for
electrical and solar energy systems**.

---

# Project Status

The project is currently in an **early engineering foundation phase**.

The diagram editor is functional and the electrical domain architecture
has been defined.

The next development stages focus on implementing the **engineering engine**
that will transform the editor into a full electrical analysis platform.

Current capabilities include:

- diagram editor
- component placement
- wiring
- component rendering
- editor interaction tools
- early electrical domain model

Planned capabilities include:

- electrical graph model
- automatic validation
- current and voltage calculation
- cable sizing
- photovoltaic system modeling
- battery system modeling
- energy simulation

---

# Long-Term Vision

SolarDiagramApp aims to become a platform capable of:

- designing electrical installations
- designing photovoltaic systems
- validating engineering constraints
- simulating energy flows
- monitoring real installations

Possible future capabilities:

- electrical load flow analysis
- photovoltaic performance simulation
- battery autonomy analysis
- digital twin of energy systems
- smart energy management

---

# System Architecture

The system architecture follows a layered model.
User
↓
UI Layer
↓
Application Layer
↓
Domain Model
↓
Engineering Engines
↓
Infrastructure

Key architectural principles:

- domain driven design
- separation of concerns
- graph-based electrical modeling
- modular calculation engines

---

# Core Engineering Model

The system models electrical installations as a **graph**.

Graph elements include:

Nodes → electrical connection points  
Edges → cables and conductive paths  
Components → devices connected to the graph

This structure allows:

- circuit discovery
- current propagation
- voltage propagation
- topology validation
- energy flow simulation

---

# Documentation Structure

Project documentation is located in the `/docs` folder.

Each document focuses on a specific aspect of the system.

docs
├ ARCHITECTURE.md
├ SYSTEM_ARCHITECTURE_DIAGRAM.md
├ CODE_STRUCTURE_GUIDE.md
├ DEVELOPMENT_GUIDELINES.md
├ DEVELOPMENT_PHASES.md
├ FIRST_IMPLEMENTATION_TASKS.md
├ DOMAIN_GRAPH_MODEL.md
├ GRAPH_CALCULATION_FLOW.md
├ ELECTRICAL_COMPONENT_SPEC.md
├ COMPONENT_CATALOG.md
├ ELECTRICAL_RULES_ENGINE.md
├ CALCULATION_ENGINE.md
├ SOLAR_DOMAIN_MODEL.md
├ ENGINE_LIMITATIONS_AND_ASSUMPTIONS.md
├ ENGINEERING_ROADMAP.md
├ CHANGELOG.md


These documents define the **engineering foundation of the system**.

Full technical documentation is available in:

docs/INDEX.md

---

# Development Roadmap

Development will occur in structured phases.

Key phases include:

1. Electrical graph integration
2. Topology validation
3. Electrical calculations
4. Solar system modeling
5. Battery system modeling
6. Energy simulation
7. Smart energy management

Each phase builds on the previous one while preserving system stability.

---

# Current Development Focus

The next implementation milestone is:

**Electrical Graph Integration**

Main objectives:

- build ElectricalGraph from diagram
- detect circuits
- propagate voltage
- calculate load currents
- validate cable and protection constraints

This milestone will transform SolarDiagramApp from:

**diagram editor**

into

**engineering analysis tool**

---

# Technology Stack

The project is currently built with:

- Kotlin
- Android
- Jetpack Compose
- MVVM architecture

Future versions may introduce additional technologies for:

- simulation engines
- data analysis
- cloud integration

---

# Contribution Philosophy

Development follows strict engineering discipline.

Key principles:

- stability before speed
- incremental evolution
- domain-first design
- architectural consistency

Large structural changes must always update the documentation.

---

# Strategic Importance

SolarDiagramApp is designed to grow into a **modern open engineering
platform for energy systems**.

The architecture is intentionally built to support long-term expansion
without compromising maintainability.

The project aims to combine:

electrical engineering  
software engineering  
energy systems modeling

into a single platform.
