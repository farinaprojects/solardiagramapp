# SolarDiagramApp — Editor Engine Architecture

This document describes the internal architecture of the SolarDiagramApp diagram editor engine.

The editor engine is responsible for enabling interactive diagram editing, including component manipulation, cable creation, selection, and visual rendering.

This document exists to ensure that the editor evolves in a structured way without introducing instability into the system.


------------------------------------------------------------


EDITOR ENGINE PURPOSE

The editor engine provides the interactive environment where users build electrical diagrams.

Its responsibilities include:


- rendering the diagram
- handling user interactions
- managing component manipulation
- managing cable connections
- coordinating selection and editing actions
- maintaining diagram consistency during editing

The editor engine must remain predictable and stable because it is the central interaction layer of the application.

------------------------------------------------------------

EDITOR ENGINE CORE PRINCIPLES

The editor must follow several core principles.

1. Deterministic behavior  
User interactions must produce consistent and predictable results.

2. Separation of responsibilities  
Rendering, interaction, and domain logic must remain separated.

3. Stability before complexity  
New interaction features should only be introduced when existing behaviors are stable.

4. Stateless rendering whenever possible  
Rendering should depend only on the current diagram state.

------------------------------------------------------------

EDITOR ENGINE SUBSYSTEMS

The editor engine is composed of several internal subsystems.

These subsystems must remain logically separated.

Subsystems include:

- Canvas Viewport
- Rendering System
- Interaction System
- Hit Detection System
- Selection System
- Connection System
- Command System (future)

------------------------------------------------------------

CANVAS VIEWPORT

The canvas viewport defines the visual space where the diagram is displayed.

Responsibilities include:

- managing zoom level
- managing pan position
- transforming world coordinates to screen coordinates
- defining visible diagram region

The viewport should remain independent from component logic.

Primary responsibilities:

- coordinate transformation
- viewport boundaries
- visual navigation

Example file:

EditorCanvasViewport.kt

------------------------------------------------------------

RENDERING SYSTEM

The rendering system is responsible for drawing all diagram elements.

Rendering responsibilities include:

- component drawing
- connector visualization
- cable drawing
- label rendering
- selection highlights
- editor overlays

Rendering must be purely visual and must not introduce domain logic.

Primary renderer file:

ComponentRenderer.kt

The renderer should rely only on:

- component state
- connector positions
- cable paths
- current selection state

------------------------------------------------------------

INTERACTION SYSTEM

The interaction system handles all user gestures and editing actions.

Responsibilities include:

- dragging components
- rotating components
- duplicating components
- moving selections
- initiating connections
- selecting elements

Interaction logic must translate gestures into editor actions.

Example file:

EditorCanvasGestureHandler.kt

Interactions must update the editor state without directly manipulating rendering logic.

------------------------------------------------------------

HIT DETECTION SYSTEM

Hit detection determines which diagram element is under the pointer or gesture.

Responsibilities include:

- detecting component selection
- detecting connector interaction
- detecting cable interaction
- prioritizing overlapping elements

Example file:

EditorHitTest.kt

Hit detection should operate using diagram geometry rather than visual representation.

------------------------------------------------------------

SELECTION SYSTEM

The selection system manages element selection within the editor.

Responsibilities include:

- selecting components
- multi-selection
- selection box logic
- maintaining selection state
- providing visual feedback

The selection system must support:

- single selection
- multi-selection
- group operations

Selection state must remain consistent during drag and editing operations.

------------------------------------------------------------

CONNECTION SYSTEM

The connection system manages cables and connector relationships.

Responsibilities include:

- detecting compatible connectors
- creating cables
- anchoring cables to connectors
- updating cable positions when components move
- maintaining cable integrity

Connections should remain valid even after component movement or rotation.

Future improvements may include automatic cable routing.

------------------------------------------------------------

COMMAND SYSTEM (FUTURE)

A command system should be introduced to support undo and redo functionality.

Each editor operation should be represented as a command.

Examples of commands:

- AddComponentCommand
- MoveComponentCommand
- ConnectCableCommand
- DeleteComponentCommand

Commands should support:

- execute
- undo
- redo

This will provide a reliable editing history.

------------------------------------------------------------

EDITOR STATE

The editor engine operates on a central editor state.

The editor state includes:

- diagram components
- connectors
- cables
- selection state
- viewport state
- interaction state

The editor state should be the single source of truth for rendering and interaction.

------------------------------------------------------------

COORDINATE SYSTEM

The editor uses two coordinate systems.

World Coordinates

Represent diagram geometry independent of screen.

Screen Coordinates

Represent positions on the display.

The viewport is responsible for converting between these systems.

This separation allows:

- zoom
- pan
- consistent diagram geometry

------------------------------------------------------------

COMPONENT MANIPULATION

Component manipulation includes:

- drag movement
- rotation
- duplication
- deletion
- alignment operations

When components move:

- connectors must update
- cables must update
- selection state must remain valid

Component manipulation must never break cable integrity.

------------------------------------------------------------

CABLE MANAGEMENT

Cables represent connections between connectors.

Cable responsibilities include:

- maintain start connector
- maintain end connector
- update visual path
- maintain diagram consistency

Cables should automatically update their geometry when connected components move.

------------------------------------------------------------

EDITOR PERFORMANCE

Editor performance must remain stable as diagrams grow.

Potential performance bottlenecks include:

- repeated recomposition
- inefficient hit detection
- cable redraw frequency
- large diagram rendering

Future optimizations may include:

- spatial indexing
- selective rendering updates
- caching geometry

------------------------------------------------------------

EDITOR ENGINE EVOLUTION

Future versions of the editor engine may introduce:

- automatic cable routing
- alignment tools
- grid snapping
- smart connector suggestions
- advanced selection tools

These features should only be introduced once the current editor behavior is stable.

------------------------------------------------------------

ARCHITECTURAL RULES

The editor engine must respect these rules.

Rendering must not contain domain logic.

Interaction must not directly manipulate rendering.

Domain rules must not be implemented in UI components.

Editor state must remain the single source of truth.

Violating these rules increases instability and technical debt.

------------------------------------------------------------

FINAL PRINCIPLE

The editor engine is the most complex subsystem of SolarDiagramApp.

Its development must prioritize:

- stability
- predictability
- maintainability

New features must never compromise the reliability of the editing experience.