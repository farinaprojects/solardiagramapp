# SolarDiagramApp — Architecture Documentation

This document describes the architectural structure of SolarDiagramApp.

Its goal is to ensure that the system evolves in a controlled, maintainable, and extensible way while supporting the future transformation of the application into a professional engineering tool.

This document should be updated whenever major architectural decisions are introduced.


------------------------------------------------------------



ARCHITECTURAL PHILOSOPHY


SolarDiagramApp is designed following principles inspired by:

- Clean Architecture
- MVVM
- Separation of Concerns
- Domain-driven modeling

The system must remain:

- modular
- predictable
- extensible
- maintainable

Architectural clarity is more important than rapid feature expansion.

Every new feature must respect the existing architectural structure.

------------------------------------------------------------

HIGH LEVEL ARCHITECTURE

The system is organized in layered architecture.

Logical layers:

UI
↓
Application / Domain
↓
Engine
↓
Model
↓
Persistence

Each layer has specific responsibilities and must not violate layer boundaries.

------------------------------------------------------------

UI LAYER

Location:

app/src/main/java/.../ui

Responsibilities:

- render visual components
- process user interaction
- present editor state
- coordinate interface components
- dispatch user actions

Examples:

- Editor screens
- Toolbar
- Component palette
- Property panels
- Canvas interaction

Important rule:

The UI layer must NOT contain electrical rules or validation logic.

The UI should only represent the current state and dispatch user actions.

------------------------------------------------------------

DOMAIN LAYER

Location:

app/src/main/java/.../domain

Responsibilities:

- business rules
- application behavior
- use cases
- orchestration of operations

Examples:

- project manipulation
- component insertion
- diagram state management
- command execution

The domain layer acts as the bridge between the UI and the internal logic of the system.

------------------------------------------------------------

ENGINE LAYER

Location:

app/src/main/java/.../domain/engine

Responsibilities:

- structural validation
- electrical rule validation
- topology verification
- diagram analysis

Examples:

- connector compatibility validation
- missing connection detection
- invalid component placement
- topology checking

The engine layer contains pure logic and must remain independent from the UI.

------------------------------------------------------------

MODEL LAYER

Location:

app/src/main/java/.../domain/model

Responsibilities:

- represent diagram entities
- define components
- define connectors
- define cables
- represent project structure

Examples of models:

Component  
Connector  
Cable  
Project  
DiagramState  

Models must remain:

- simple
- deterministic
- UI-independent

Models represent the real domain structure of the electrical diagram.

------------------------------------------------------------

PERSISTENCE LAYER

Location:

app/src/main/java/.../data

Responsibilities:

- project storage
- serialization
- project loading and saving
- local repository implementation

Examples:

- JSON project files
- project repository
- future database integration

Persistence must not contain domain logic.

------------------------------------------------------------

EDITOR SUBSYSTEM

The editor is the core interactive subsystem of the application.

Responsibilities:

- render the diagram canvas
- manage interaction with components
- support editing actions
- coordinate rendering and interaction

Core editor files include:

EditorScreen.kt  
EditorCanvasViewport.kt  
EditorCanvasGestureHandler.kt  
EditorHitTest.kt  

The editor subsystem coordinates the visual manipulation of the diagram.

------------------------------------------------------------

RENDERING SYSTEM

The rendering system draws the diagram elements.

Responsibilities:

- component drawing
- connector rendering
- cable drawing
- label rendering

Primary renderer file:

ComponentRenderer.kt

Rendering must remain purely visual and must not implement domain logic.

------------------------------------------------------------

INTERACTION SYSTEM

The interaction system manages user gestures.

Responsibilities:

- dragging components
- selection
- multi-selection
- duplication
- rotation
- movement

Interaction logic should remain separate from rendering logic whenever possible.

------------------------------------------------------------

CONNECTION SYSTEM

The connection system manages cable creation and connector relationships.

Responsibilities:

- detect compatible connectors
- anchor cables to connectors
- maintain cable stability during component movement
- manage reconnection behavior

Future improvements may introduce automatic routing algorithms.

------------------------------------------------------------

COMPONENT SYSTEM

Components represent electrical devices inside the diagram.

Examples include:

- photovoltaic modules
- microinverters
- inverters
- breakers
- busbars
- electrical loads

Each component defines:

- connectors
- connector types
- connector positions
- visual representation

Components should remain electrically meaningful but visually simplified.

------------------------------------------------------------

CONNECTOR SYSTEM

Connectors represent electrical connection points.

Connector attributes include:

- type (L1, L2, L3, N, PE)
- orientation
- position
- compatibility rules

Connectors enable safe diagram logic and connection validation.

------------------------------------------------------------

CABLE SYSTEM

Cables represent connections between connectors.

Responsibilities:

- maintain start and end anchors
- update position when components move
- represent non-connected crossings
- maintain structural integrity

Future improvements may include smart routing.

------------------------------------------------------------

VALIDATION ENGINE

The validation engine analyzes diagram integrity.

Responsibilities:

- detect incomplete connections
- detect incompatible connectors
- detect invalid topology
- report structural issues

Validation messages should be classified by severity:

INFO  
WARNING  
ERROR  

Validation must remain independent from UI.

------------------------------------------------------------

COMMAND SYSTEM (FUTURE)

Future versions should implement a command system.

Purpose:

- undo
- redo
- reversible editing operations

Each editor action should become a command object.

This enables reliable editing history.

------------------------------------------------------------

PERFORMANCE CONSIDERATIONS

As diagrams grow, performance must remain stable.

Key optimization areas:

- rendering efficiency
- hit detection
- cable updates
- recomposition control

Architecture must allow future optimization without major redesign.

------------------------------------------------------------

ARCHITECTURAL RULES

The following rules must always be respected.

UI must not implement domain rules.

Domain must not depend on UI.

Engine must contain pure logic.

Models must remain simple data structures.

Persistence must not implement business rules.

Violating these rules increases coupling and makes the system harder to maintain.

------------------------------------------------------------

FUTURE ARCHITECTURAL EVOLUTION

Future versions of SolarDiagramApp may introduce:

- automated electrical validation
- engineering calculations
- advanced cable routing
- diagram export engines
- technical documentation generation

The architecture is designed to support these expansions.

------------------------------------------------------------

ARCHITECTURE GOVERNANCE

Whenever major structural changes occur:

- this document should be updated
- architectural boundaries should be reviewed
- compatibility with the roadmap should be evaluated

Maintaining architectural clarity is essential for long-term sustainability.

------------------------------------------------------------

FINAL PRINCIPLE

SolarDiagramApp should evolve through disciplined architecture and incremental development.

Features should only be added when they strengthen the structure of the system.

Architecture must remain clear, predictable, and extensible.