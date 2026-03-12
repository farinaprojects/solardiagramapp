# SolarDiagramApp — Code Structure Guide

This document defines the official code organization guide for SolarDiagramApp.

The purpose of this guide is to ensure that the codebase grows in a
controlled, maintainable and architecturally consistent way.

The system must evolve without turning into a monolithic codebase.

This guide defines:

- package responsibilities
- class placement rules
- dependency direction
- naming conventions
- structural boundaries

------------------------------------------------------------

# Core Principle

The project must be organized around **domain clarity**.

The code structure must reflect the engineering structure of the system.

The main architectural rule is:

UI depends on Application  
Application depends on Domain  
Domain depends on nothing external  
Infrastructure adapts to Domain

------------------------------------------------------------

# Top-Level Package Structure

Recommended package structure:

br.com.solardiagram
├── data
├── domain
├── application
├── ui
├── shared
└── test

------------------------------------------------------------

# 1 — domain/

The domain package contains the engineering core of the system.

This is the most important part of the project.

The domain must contain:

- entities
- value objects
- graph models
- engineering rules
- calculation contracts
- pure business logic

The domain must NOT contain:

- Jetpack Compose code
- Android framework classes
- file I/O
- persistence implementation
- UI state logic

Recommended structure:

domain
├── model
├── electrical
├── solar
├── storage
├── rules
├── validation
├── calculation
└── graph

------------------------------------------------------------

## domain/model/

Contains generic domain entities used across the system.

Examples:

Project
Diagram
ComponentId
PortId
ConnectionId

Use this package for neutral concepts not tied to a specific engineering domain.

------------------------------------------------------------

## domain/electrical/

Contains the core electrical engineering model.

Examples:

ElectricalInstallation
ElectricalCircuit
ElectricalComponent
ElectricalPort
ElectricalProtection
ElectricalFlow

This package represents the electrical truth of the system.

------------------------------------------------------------

## domain/graph/

Contains the graph representation of the electrical system.

Examples:

ElectricalGraph
ElectricalNode
ElectricalEdge
GraphBuilder
GraphTraversal
CircuitSegmentationEngine

All topology-oriented logic should remain here.

------------------------------------------------------------

## domain/solar/

Contains photovoltaic domain concepts.

Examples:

PVModule
PVString
PVArray
Inverter
MPPT
GridConnection

This package must remain separate from generic electrical modeling.

------------------------------------------------------------

## domain/storage/

Contains energy storage domain concepts.

Examples:

Battery
BatteryBank
BMS
BatteryState
BatteryAutonomyCalculator

Battery logic must remain isolated from UI and rendering concerns.

------------------------------------------------------------

## domain/rules/

Contains engineering rules and specifications.

Examples:

ConnectorCompatibilityRules
VoltageRules
CurrentRules
ProtectionRules
SolarRules

Rules should be declarative whenever possible.

------------------------------------------------------------

## domain/validation/

Contains validation engines.

Examples:

TopologyValidationEngine
ProtectionValidationEngine
ComponentRuleValidationEngine
SolarValidationEngine
BatteryValidationEngine

Validation output should be standardized.

------------------------------------------------------------

## domain/calculation/

Contains calculation engines and mathematical services.

Examples:

ElectricalCalculationEngine
VoltageDropEngine
CableSizingEngine
CurrentEstimationEngine
BatteryCalculationEngine
SolarCalculationEngine

These engines should be pure and deterministic whenever possible.

------------------------------------------------------------

# 2 — application/

The application package coordinates use cases.

It acts as the bridge between UI and Domain.

The application layer may contain:

- use cases
- command handlers
- orchestration services
- project workflows
- recalculation triggers

The application layer must NOT contain:

- direct rendering code
- Compose UI
- low-level engineering formulas
- persistence details

Recommended structure:

application
├── usecase
├── command
├── orchestration
└── service

------------------------------------------------------------

## application/usecase/

Contains explicit use cases.

Examples:

AddComponentUseCase
RemoveComponentUseCase
ConnectPortsUseCase
RebuildGraphUseCase
RunValidationUseCase
RunCalculationUseCase

Each use case should represent a clear user or system intention.

------------------------------------------------------------

## application/command/

Contains command pattern classes for editor operations.

Examples:

AddComponentCommand
MoveComponentCommand
RotateComponentCommand
DeleteSelectionCommand

Useful for:

- undo/redo
- reproducible editor operations
- change tracking

------------------------------------------------------------

## application/orchestration/

Contains multi-step flows.

Examples:

ProjectLoadOrchestrator
CalculationPipelineOrchestrator
ValidationPipelineOrchestrator

This package is for workflows that involve multiple domain services.

------------------------------------------------------------

## application/service/

Contains app-level coordinators.

Examples:

ProjectCoordinator
DiagramCoordinator
EditorStateSynchronizer

Use this package carefully.
Do not move domain logic here.

------------------------------------------------------------

# 3 — data/

The data package contains persistence and external representations.

It may contain:

- repositories
- serialization
- DTOs
- local storage
- import/export logic

It must NOT contain:

- Compose UI
- domain rules
- calculation formulas

Recommended structure:

data
├── repository
├── dto
├── mapper
├── persistence
└── serialization

------------------------------------------------------------

## data/repository/

Contains repository implementations.

Examples:

ProjectRepositoryImpl
ComponentCatalogRepositoryImpl

Repositories adapt storage to domain expectations.

------------------------------------------------------------

## data/dto/

Contains raw storage or transfer objects.

Examples:

ProjectDto
DiagramDto
ComponentDto
ConnectionDto

DTOs should not contain behavior.

------------------------------------------------------------

## data/mapper/

Contains mapping between DTOs and domain entities.

Examples:

ProjectMapper
DiagramMapper
ComponentMapper

All data-domain translation should happen here.

------------------------------------------------------------

## data/persistence/

Contains storage mechanisms.

Examples:

FileProjectDataSource
LocalProjectStorage
JsonProjectStore

------------------------------------------------------------

## data/serialization/

Contains serializers and parsers.

Examples:

ProjectJsonSerializer
ProjectJsonParser

------------------------------------------------------------

# 4 — ui/

The ui package contains everything related to interaction and visualization.

It may contain:

- screens
- composables
- viewmodels
- UI state
- renderers
- gestures
- editor interaction logic

It must NOT contain:

- engineering formulas
- graph calculation logic
- electrical rules
- persistence implementation

Recommended structure:

ui
├── screens
├── components
├── editor
├── renderer
├── state
├── viewmodel
└── theme

------------------------------------------------------------

## ui/screens/

Contains screen-level composables.

Examples:

EditorScreen
ProjectListScreen
ProjectDetailsScreen

------------------------------------------------------------

## ui/components/

Contains reusable UI components.

Examples:

Toolbar
MiniToolbar
PropertyPanel
PaletteItem

------------------------------------------------------------

## ui/editor/

Contains interactive editor-specific UI logic.

Examples:

SelectionBox
DragHandler
SnapInteractionAdapter
WireCreationHandler
EditorHitTest

This package may contain visual behavior, but not domain rules.

------------------------------------------------------------

## ui/renderer/

Contains rendering logic.

Examples:

ComponentRenderer
WireRenderer
LabelRenderer
GridRenderer

Renderer code must focus on visual output only.

------------------------------------------------------------

## ui/state/

Contains UI state models.

Examples:

EditorUiState
SelectionState
ToolbarState
PropertyPanelState

------------------------------------------------------------

## ui/viewmodel/

Contains ViewModels.

Examples:

EditorViewModel
ProjectListViewModel
ProjectDetailViewModel

ViewModels coordinate UI actions and application use cases.

------------------------------------------------------------

## ui/theme/

Contains typography, colors, spacing and visual design tokens.

------------------------------------------------------------

# 5 — shared/

The shared package contains reusable cross-cutting structures.

Use this package carefully.

Allowed contents:

- Result wrappers
- error models
- utility math helpers
- shared constants
- logging abstractions

It must NOT become a generic dumping ground.

Recommended structure:

shared
├── result
├── error
├── util
├── logging
└── constants

------------------------------------------------------------

# 6 — test/

The test package mirrors the production structure.

Examples:

test/domain/graph/
test/domain/calculation/
test/application/usecase/
test/ui/viewmodel/

Tests should be organized according to the same boundaries as main code.

------------------------------------------------------------

# Dependency Rules

The allowed dependency flow is:

ui → application → domain  
data → domain  
application → domain  
ui → shared  
application → shared  
data → shared

Forbidden dependencies:

domain → ui  
domain → data  
domain → Android framework  
renderer → calculation engines  
Compose screens → repository implementation

------------------------------------------------------------

# Package Placement Rules

The following rules must be respected.

## Rule 1 — If it contains engineering truth, place it in domain

Examples:

Voltage limits  
Current formulas  
Battery autonomy  
Port compatibility

------------------------------------------------------------

## Rule 2 — If it coordinates actions, place it in application

Examples:

Recalculate after diagram change  
Run full validation pipeline  
Import project and rebuild graph

------------------------------------------------------------

## Rule 3 — If it stores or loads data, place it in data

Examples:

Save project JSON  
Load component definitions  
Export diagram file

------------------------------------------------------------

## Rule 4 — If it draws or interacts visually, place it in ui

Examples:

Drawing component symbols  
Handling drag gestures  
Displaying selected items

------------------------------------------------------------

# Naming Conventions

Use explicit names.

Prefer:

VoltageDropEngine  
ProtectionValidationEngine  
BatteryAutonomyCalculator  
GraphBuilder  
RunCalculationUseCase

Avoid vague names:

Manager  
Helper  
Utils  
Processor  
Handler

unless the responsibility is extremely clear.

------------------------------------------------------------

# Class Size Guidelines

Recommended limits:

- Entity classes: small and cohesive
- Use cases: focused on one action
- Validation engines: grouped by domain
- ViewModels: orchestration only
- Renderers: visual only

If a class grows too much, split by responsibility.

------------------------------------------------------------

# File Organization by Responsibility

Examples of correct placement:

ElectricalGraph.kt
→ domain/graph/

CableSizingEngine.kt
→ domain/calculation/

ProtectionValidationEngine.kt
→ domain/validation/

AddComponentUseCase.kt
→ application/usecase/

ProjectRepositoryImpl.kt
→ data/repository/

ComponentRenderer.kt
→ ui/renderer/

EditorViewModel.kt
→ ui/viewmodel/

------------------------------------------------------------

# Anti-Patterns to Avoid

## 1 — UI deciding engineering behavior

Example of wrong approach:

Editor screen checks breaker sizing rule directly.

Correct approach:

UI triggers application use case  
Use case invokes domain validation

------------------------------------------------------------

## 2 — ViewModel becoming domain layer

ViewModels must not implement formulas or electrical rules.

------------------------------------------------------------

## 3 — Renderer storing engineering state

Renderer should never own the electrical truth.

------------------------------------------------------------

## 4 — Repository returning UI models

Repositories should return domain entities, not composables or screen models.

------------------------------------------------------------

## 5 — God classes

Avoid oversized classes such as:

EditorManager
SystemController
GlobalEngine

Split by responsibility.

------------------------------------------------------------

# Recommended Growth Strategy

When introducing a new subsystem:

1. define domain model
2. define validation rules
3. define calculation rules
4. define use cases
5. expose through ViewModel
6. render in UI

This order preserves architecture.

------------------------------------------------------------

# Example Expansion: Battery System

Correct implementation sequence:

domain/storage/Battery.kt  
domain/storage/BatteryBank.kt  
domain/calculation/BatteryAutonomyCalculator.kt  
domain/validation/BatteryValidationEngine.kt  
application/usecase/AddBatteryUseCase.kt  
ui/renderer/BatteryRenderer.kt  
ui/viewmodel/EditorViewModel integration

------------------------------------------------------------

# Example Expansion: Solar Domain

Correct implementation sequence:

domain/solar/PVModule.kt  
domain/solar/PVString.kt  
domain/solar/Inverter.kt  
domain/calculation/SolarCalculationEngine.kt  
domain/validation/SolarValidationEngine.kt  
application/usecase/AddPVModuleUseCase.kt  
ui/renderer/PVModuleRenderer.kt

------------------------------------------------------------

# Documentation Update Rule

Whenever a structural change is introduced, update at least:

- ARCHITECTURE.md
- CODE_STRUCTURE_GUIDE.md
- DEVELOPMENT_PHASES.md

If the change affects engineering behavior, also update:

- ELECTRICAL_RULES_ENGINE.md
- CALCULATION_ENGINE.md
- DOMAIN_GRAPH_MODEL.md

------------------------------------------------------------

# Strategic Importance

A clear code structure ensures that SolarDiagramApp can evolve into
a professional engineering platform without losing maintainability.

This guide must be treated as a structural rulebook for the project.