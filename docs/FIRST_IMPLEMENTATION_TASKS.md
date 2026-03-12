# SolarDiagramApp — First Implementation Tasks

This document defines the first concrete development tasks to implement
the architecture defined in the project documentation.

Tasks are grouped by development phases.

Each task must:

- compile successfully
- be testable
- maintain system stability

------------------------------------------------------------

# Phase A — Electrical Graph Consolidation

Goal:
Ensure the electrical graph is fully integrated with the editor.

Tasks:

A1 — Graph Builder

Create a GraphBuilder class responsible for constructing
ElectricalGraph from diagram components.

Responsibilities:

- read diagram components
- create ElectricalNodes
- create ElectricalEdges
- map ports to nodes

------------------------------------------------------------

A2 — Graph Synchronization

Implement automatic graph rebuild when:

- component added
- component removed
- connection created
- connection deleted

------------------------------------------------------------

A3 — Port Mapping

Ensure every visual component port maps to an ElectricalPort.

VisualPort → ElectricalPort → ElectricalNode

------------------------------------------------------------

A4 — Graph Debug Viewer

Add debug functionality to inspect the graph.

Possible output:

- node count
- edge count
- component list

------------------------------------------------------------

# Phase B — Topology Validation

Goal:
Detect invalid electrical topologies.

Tasks:

B1 — Floating Node Detection

Detect nodes without connections.

------------------------------------------------------------

B2 — Loop Detection

Detect unintended closed electrical loops.

Use DFS cycle detection.

------------------------------------------------------------

B3 — Invalid Connection Detection

Reject invalid port connections.

Examples:

DC ↔ AC  
Phase ↔ Neutral mismatch

------------------------------------------------------------

B4 — Circuit Detection

Implement algorithm to detect circuits.

Steps:

- identify power sources
- traverse graph
- group reachable nodes

------------------------------------------------------------

# Phase C — Voltage Propagation

Goal:
Propagate voltage across the electrical graph.

Tasks:

C1 — Source Voltage Assignment

Assign voltage to source nodes:

- inverter output
- grid connection
- generator

------------------------------------------------------------

C2 — Voltage Propagation Engine

Implement BFS voltage propagation.

Rule:

V_target = V_source − voltage_drop

------------------------------------------------------------

C3 — Voltage Storage

Store node voltage values.

ElectricalNode.voltage

------------------------------------------------------------

# Phase D — Current Calculation

Goal:
Compute load and branch currents.

Tasks:

D1 — Load Current Calculation

Calculate load current:

I = P / V

------------------------------------------------------------

D2 — Three Phase Current Calculation

Implement:

I = P / (√3 × V × cosφ)

------------------------------------------------------------

D3 — Current Back Propagation

Aggregate currents upstream.

At junction:

I_total = sum(branch currents)

------------------------------------------------------------

# Phase E — Cable Validation

Goal:
Verify cable electrical limits.

Tasks:

E1 — Cable Current Check

Check:

I_edge ≤ I_cable

------------------------------------------------------------

E2 — Voltage Drop Calculation

Implement voltage drop formula.

------------------------------------------------------------

E3 — Cable Warning System

Warn when voltage drop exceeds limits.

------------------------------------------------------------

# Phase F — Protection Validation

Goal:
Ensure circuits have adequate protection.

Tasks:

F1 — Breaker Rating Validation

Check:

I_load ≤ I_breaker

------------------------------------------------------------

F2 — Missing Protection Detection

Warn when circuit has no breaker.

------------------------------------------------------------

F3 — Protection Selectivity

Check upstream breaker rating.

------------------------------------------------------------

# Phase G — Solar Components

Goal:
Introduce photovoltaic components.

Tasks:

G1 — PV Module Component

Add PV module component type.

------------------------------------------------------------

G2 — PV String Calculation

Calculate string voltage and current.

------------------------------------------------------------

G3 — Inverter Compatibility

Check PV string voltage against inverter MPPT limits.

------------------------------------------------------------

# Phase H — Battery Systems

Goal:
Add energy storage support.

Tasks:

H1 — Battery Component

Add battery component model.

------------------------------------------------------------

H2 — Battery Bank Model

Support series and parallel batteries.

------------------------------------------------------------

H3 — Battery Autonomy Calculation

Implement autonomy formula.

------------------------------------------------------------

# Phase I — Calculation Integration

Goal:
Integrate all calculation engines.

Tasks:

I1 — Calculation Pipeline

Implement calculation pipeline:

graph build
validation
voltage propagation
current calculation

------------------------------------------------------------

I2 — Calculation Trigger

Trigger calculations when:

- diagram changes
- project loads
- manual recalculation

------------------------------------------------------------

# Phase J — Result Visualization

Goal:
Display calculation results.

Tasks:

J1 — Node Voltage Display

Show voltage near nodes.

------------------------------------------------------------

J2 — Cable Current Display

Show current in wires.

------------------------------------------------------------

J3 — Validation Messages

Display warnings and errors.

------------------------------------------------------------

# Initial Implementation Target

The goal of these tasks is to achieve the first milestone:

"A fully functional electrical analysis engine operating on
the diagram graph."

Capabilities of the milestone:

- graph construction
- topology validation
- voltage propagation
- current calculation
- cable validation
- protection validation
- basic solar modeling

------------------------------------------------------------

# Development Strategy

Recommended approach:

Implement tasks sequentially.

After each phase:

- compile
- test
- commit changes

Avoid implementing multiple phases simultaneously.

------------------------------------------------------------

# Strategic Importance

These tasks transform SolarDiagramApp from:

"diagram editor"

into

"electrical engineering tool."