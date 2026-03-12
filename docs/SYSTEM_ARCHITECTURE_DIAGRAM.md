# SolarDiagramApp — System Architecture Diagram

This document presents the complete architecture of SolarDiagramApp.

The architecture is divided into five main layers:

1. User Interface Layer
2. Application Layer
3. Domain Model Layer
4. Engineering Engines Layer
5. Infrastructure Layer

------------------------------------------------------------

# High Level Architecture

                ┌──────────────────────────────┐
                │           USER               │
                └──────────────┬───────────────┘
                               │
                               ▼
                ┌──────────────────────────────┐
                │        UI LAYER              │
                │                              │
                │  Editor Screen               │
                │  Component Palette           │
                │  Toolbar                     │
                │  Properties Panel            │
                │  Project Explorer            │
                └──────────────┬───────────────┘
                               │
                               ▼
                ┌──────────────────────────────┐
                │      APPLICATION LAYER       │
                │                              │
                │  ProjectController           │
                │  DiagramController           │
                │  Command System              │
                │  Undo / Redo Manager         │
                │  Save / Load Manager         │
                └──────────────┬───────────────┘
                               │
                               ▼
                ┌──────────────────────────────┐
                │       DOMAIN LAYER           │
                │                              │
                │  ElectricalInstallation      │
                │  ElectricalCircuit           │
                │  ElectricalComponent         │
                │  ElectricalPort              │
                │  ElectricalGraph             │
                │  ElectricalNode              │
                │  ElectricalEdge              │
                │  ElectricalFlow              │
                └──────────────┬───────────────┘
                               │
                               ▼
                ┌──────────────────────────────┐
                │    ENGINE CALCULATION LAYER  │
                │                              │
                │  Validation Engines          │
                │  Calculation Engines         │
                │  Graph Analysis Engines      │
                │  Simulation Engines          │
                └──────────────┬───────────────┘
                               │
                               ▼
                ┌──────────────────────────────┐
                │      INFRASTRUCTURE          │
                │                              │
                │  Persistence                 │
                │  File Storage                │
                │  Data Serialization          │
                │  Project Export              │
                └──────────────────────────────┘

------------------------------------------------------------

# Editor Architecture

The diagram editor is responsible for graphical manipulation.

              ┌─────────────────────────┐
              │        Editor UI        │
              └───────────┬─────────────┘
                          │
                          ▼
              ┌─────────────────────────┐
              │      Editor Engine      │
              │                         │
              │ SnapEngine              │
              │ AlignmentEngine         │
              │ AutoWireRouter          │
              │ SelectionManager        │
              │ HitTestEngine           │
              └───────────┬─────────────┘
                          │
                          ▼
              ┌─────────────────────────┐
              │     Renderer Engine     │
              │                         │
              │ ComponentRenderer       │
              │ WireRenderer            │
              │ LabelRenderer           │
              └───────────┬─────────────┘
                          │
                          ▼
              ┌─────────────────────────┐
              │     Domain Mapping      │
              │                         │
              │ VisualComponent →       │
              │ ElectricalComponent     │
              └─────────────────────────┘

------------------------------------------------------------

# Electrical Graph Model

The electrical model is represented as a graph.

             ┌─────────────────────────┐
             │     ElectricalGraph     │
             └────────────┬────────────┘
                          │
             ┌────────────▼────────────┐
             │                         │
      ┌──────▼──────┐           ┌──────▼──────┐
      │ Electrical  │           │ Electrical  │
      │    Node     │           │    Edge     │
      └──────┬──────┘           └──────┬──────┘
             │                         │
             ▼                         ▼
      ┌──────────────┐          ┌──────────────┐
      │ Component    │          │ Wire / Cable │
      │ Ports        │          │ Connections  │
      └──────────────┘          └──────────────┘

Nodes represent electrical connection points.

Edges represent conductive paths.

------------------------------------------------------------

# Component Model

Each component connects to the graph via ports.

         ┌──────────────────────────┐
         │   ElectricalComponent    │
         └─────────────┬────────────┘
                       │
            ┌──────────▼──────────┐
            │  Electrical Ports   │
            └──────────┬──────────┘
                       │
                       ▼
                 ElectricalNode

Examples of components:

PV Module  
Inverter  
Battery  
Breaker  
Load  
Busbar

------------------------------------------------------------

# Calculation Engine Architecture

Calculation engines operate on the graph.

         ┌─────────────────────────────┐
         │   ElectricalCalculation     │
         └──────────────┬──────────────┘
                        │
     ┌──────────────────┼──────────────────┐
     ▼                  ▼                  ▼

VoltagePropagation   CurrentAggregation   PowerFlow

                        │
                        ▼

                 EnergyFlowEngine

------------------------------------------------------------

# Validation Engine

Validation engines ensure system safety.

Validation modules:

TopologyValidationEngine  
ProtectionValidationEngine  
ComponentRuleValidationEngine  
SolarValidationEngine  
BatteryValidationEngine

Each validator analyzes the graph and produces warnings or errors.

------------------------------------------------------------

# Solar Domain Integration

Solar components integrate into the electrical graph.

PV Module → DC Graph  
Inverter → DC to AC conversion  
Battery → bidirectional storage  
Grid → external source

Energy flows through the same graph model.

------------------------------------------------------------

# Simulation Layer

Future simulation engines will use the graph.

Simulation modules may include:

EnergyProductionSimulation  
BatteryAutonomySimulation  
LoadProfileSimulation

------------------------------------------------------------

# Data Flow

User Action
↓
Editor Interaction
↓
Domain Model Update
↓
Graph Rebuild
↓
Validation
↓
Calculation
↓
Results Display

------------------------------------------------------------

# Strategic Architecture

The SolarDiagramApp architecture is designed so that:

- UI is only a visual layer
- the domain contains electrical truth
- calculations operate on the graph
- validation guarantees safety

This separation ensures the system remains scalable and maintainable.

------------------------------------------------------------

# Long Term System Vision

The architecture enables future capabilities:

Electrical design automation  
Photovoltaic system engineering  
Energy system simulation  
Digital twin of installations  
Smart energy management


------------------------------------------------------------

Visual resumo da arquitetura

Se transformarmos tudo em uma visão simples:

USER
│
▼
EDITOR UI
│
▼
APPLICATION CONTROLLERS
│
▼
DOMAIN MODEL
│
▼
ELECTRICAL GRAPH
│
▼
ENGINE LAYER
│
├ Validation
├ Calculation
├ Simulation
│
▼
RESULTS / ANALYSIS
