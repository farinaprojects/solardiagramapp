# SolarDiagramApp — Development Guidelines

This document defines the engineering guidelines, architectural rules, and development workflow for the SolarDiagramApp project.

The goal is to ensure the project grows with:

- architectural stability
- incremental evolution
- technical coherence with electrical systems
- potential to evolve into a professional product

These guidelines apply to all future development phases of the system.



------------------------------------------------------------

CORE DEVELOPMENT PRINCIPLES

1. Stability Before Speed

No modification should break existing functionality.

Every change must preserve:

- successful compilation
- existing feature behavior
- structural consistency of the codebase

When uncertainty exists, prioritize minimal safe changes over large refactors.

------------------------------------------------------------

2. Incremental Evolution

The system evolves in small, controlled phases.

Each phase must:

1. address a clearly defined objective
2. maintain compatibility with the current codebase
3. allow immediate testing after implementation

Avoid introducing multiple major changes in a single iteration.

------------------------------------------------------------

3. Architectural Consistency

The project follows a layered architecture inspired by Clean Architecture and MVVM.

Architecture flow:

UI
↓
Domain
↓
Engine
↓
Model

Layer Responsibilities

UI
- rendering
- user interaction
- visual layout

Domain
- application logic
- use cases
- domain rules

Engine
- structural validations
- electrical logic
- diagram consistency checks

Model
- core data structures
- components
- connectors
- cables

Architectural Rules

- Electrical logic must never exist in the UI layer.
- Validation logic must live inside the Engine layer.
- Models must remain simple and deterministic.

------------------------------------------------------------

ELECTRICAL REPRESENTATION RULES

Components must represent real electrical elements.

Examples

Photovoltaic Module
Purpose: DC energy generation

Microinverter
Purpose: DC to AC conversion

Breaker
Purpose: electrical protection

Busbar
Purpose: connection junction

Load
Purpose: electrical consumption

Each component must define:

- electrical connectors
- connector type (L, N, PE, etc.)
- geometric placement
- graphical representation

Visual simplification is allowed, but electrical meaning must remain accurate.

------------------------------------------------------------

EDITOR SYSTEM RULES

The diagram editor is composed of multiple subsystems:

- Canvas rendering engine
- Component renderer
- Gesture system
- Hit detection system
- Connection system

Core files include:

ComponentRenderer.kt  
EditorScreen.kt  
EditorCanvasViewport.kt  
EditorCanvasGestureHandler.kt  
EditorHitTest.kt  

Changes to these files must be handled carefully to avoid breaking editor behavior.

------------------------------------------------------------

SAFE MODIFICATION RULES

Before implementing changes, verify potential impact on:

- drag behavior
- component rotation
- selection system
- cable rendering
- connector alignment
- canvas navigation

If a change affects one subsystem, verify the others remain stable.

------------------------------------------------------------

COMPONENT CREATION RULES

When adding new components to the system, ensure:

1. connectors are explicitly defined
2. connector labels are clear (L1, L2, N, PE)
3. connector offsets are geometrically correct
4. graphical representation matches component function

Components should remain visually simple but electrically meaningful.

------------------------------------------------------------

VISUAL CONSISTENCY GUIDELINES

Components should maintain:

- consistent connector sizes
- consistent spacing
- clear labeling
- balanced proportions

Avoid components visually dominating the diagram unless functionally necessary.

------------------------------------------------------------

CABLE RENDERING RULES

Cables must:

- follow predictable routing
- avoid unnecessary crossings
- clearly indicate non-connected crossings when applicable
- remain visually secondary to components

Future versions may introduce cable routing optimization.

------------------------------------------------------------

DEVELOPMENT WORKFLOW

The development workflow follows a structured iteration cycle.

STEP 1 — Project Update

The current state of the project is updated and provided for analysis.

Possible formats:

- project archive (.zip)
- Git repository

------------------------------------------------------------

STEP 2 — Technical Analysis

The current project state is analyzed with focus on:

- architecture
- editor stability
- component rendering
- electrical coherence
- potential regressions

------------------------------------------------------------

STEP 3 — Controlled Modifications

Only necessary files are modified.

Changes must:

- preserve project structure
- compile without errors
- maintain compatibility with existing functionality

------------------------------------------------------------

STEP 4 — Delivery for Testing

The updated project is returned as:

- modified source files
or
- updated project archive

------------------------------------------------------------

STEP 5 — Validation

The project is compiled and tested to confirm:

- editor stability
- rendering correctness
- interaction behavior

Once validated, the next iteration begins.

------------------------------------------------------------

REFACTORING POLICY

Large structural refactors should be avoided unless absolutely necessary.

Preferred approach:

- small targeted improvements
- progressive architectural refinement
- backward compatibility with current code

------------------------------------------------------------

LONG-TERM PROJECT DIRECTION

The system is being developed to support future capabilities including:

- professional solar electrical diagram creation
- automated diagram validation
- engineering documentation generation
- electrical rule checking
- export of technical diagrams

These capabilities guide architectural decisions throughout development.

------------------------------------------------------------

FINAL GUIDELINE

Development should prioritize clarity, stability, and long-term maintainability.

Progress should occur through careful iteration rather than rapid structural changes.