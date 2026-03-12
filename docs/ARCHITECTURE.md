# SolarDiagramApp — System Architecture

This document defines the architectural structure of SolarDiagramApp.

Its purpose is to guarantee that the project evolves in a controlled,
stable and extensible way while preserving the long-term vision of the system.

SolarDiagramApp is not intended to be only a graphical editor.

The long-term goal is to evolve into a **professional engineering platform
for electrical and photovoltaic systems**.

------------------------------------------------------------

# Architectural Philosophy

The system follows principles inspired by:

- Clean Architecture
- Domain Driven Design
- MVVM (Android UI)
- Separation of Concerns

The architecture must remain:

- modular
- deterministic
- extensible
- testable

Architectural clarity is always preferred over rapid feature addition.

------------------------------------------------------------

# High Level Architecture

The system is organized into layers.
UI Layer
↓
Application Layer
↓
Domain Layer
↓
Calculation Engines
↓
Persistence Layer


------------------------------------------------------------

# UI Layer

Responsible only for presentation and interaction.

Main components:

- Jetpack Compose UI
- ViewModels
- Navigation
- Editor Screen

The UI must never contain electrical rules.

------------------------------------------------------------

# Application Layer

Coordinates interactions between UI and domain.

Responsibilities:

- project loading
- project saving
- command orchestration
- undo/redo management

------------------------------------------------------------

# Domain Layer

The domain layer contains the electrical model.

This is the most important part of the system.

Key entities:

ElectricalInstallation  
ElectricalCircuit  
ElectricalGraph  
ElectricalNode  
ElectricalEdge  
ElectricalFlow

This graph structure allows modeling of real electrical systems.

------------------------------------------------------------

# Electrical Engines

The system already contains several electrical calculation engines.

Examples:

CableSizingEngine  
AmpacityEngine  
VoltageDropEngine  
CurrentEstimationEngine  
BreakerLoadValidationEngine

These engines will expand in future phases.

------------------------------------------------------------

# Validation Engines

Validation is separated into multiple specialized validators.

Examples:

ElectricalValidationEngine  
CircuitValidationEngine  
ProtectionValidationEngine  
TopologyValidationEngine  
ComponentRuleValidationEngine

Each validator is responsible for a specific domain rule.

------------------------------------------------------------

# Component System

Components are defined using a separation between:

Physical model  
Visual representation

Main classes:

ComponentPhysicalCatalog  
ComponentVisualRegistry  
ComponentVisualSpec  
ComponentFactory

This allows the editor to remain independent from engineering logic.

------------------------------------------------------------

# Future Domain Extensions

Future domains will extend the system:

Solar domain  
Battery systems  
Energy simulation  
Telemetry  
Energy management

These domains will integrate with the electrical graph.

------------------------------------------------------------

# Long Term Vision

SolarDiagramApp should evolve into:

- an electrical diagram editor
- a photovoltaic system designer
- an engineering validation tool
- a simulation platform
- a digital twin system

The architecture must always support this evolution.