# SolarDiagramApp — Graph Calculation Flow

This document defines how electrical quantities propagate through the
graph model used by SolarDiagramApp.

The calculation flow is responsible for determining:

- voltage distribution
- current distribution
- power flow
- energy flow paths

The calculation operates on the **ElectricalGraph** model.

------------------------------------------------------------

# Calculation Overview

The electrical calculation process follows these steps:

1. Graph Build
2. Topology Validation
3. Source Identification
4. Circuit Segmentation
5. Voltage Propagation
6. Load Current Calculation
7. Current Back-Propagation
8. Cable and Protection Verification
9. Energy Flow Mapping
10. Result Aggregation

Each step produces intermediate values stored in the graph nodes and edges.

------------------------------------------------------------

# Step 1 — Graph Build

The electrical graph is constructed from the diagram model.

Process:

1. create nodes for connection points
2. create edges for wires
3. attach components to nodes via ports
4. finalize connectivity map

Output:

ElectricalGraph

------------------------------------------------------------

# Step 2 — Topology Validation

Before running calculations the graph must be validated.

Checks include:

- floating nodes
- invalid connections
- phase mismatches
- loop detection

Invalid graphs cannot proceed to calculation.

------------------------------------------------------------

# Step 3 — Source Identification

Power sources are detected automatically.

Possible sources:

PV inverters  
grid connection  
generators  
battery inverters

Each source defines an initial voltage node.

Example:

AC inverter → 220V AC  
PV string → DC voltage

------------------------------------------------------------

# Step 4 — Circuit Segmentation

The graph is segmented into circuits.

Algorithm:

1. start from source nodes
2. perform BFS traversal
3. collect reachable nodes
4. mark circuit boundaries

Each circuit is processed independently.

------------------------------------------------------------

# Step 5 — Voltage Propagation

Voltage propagates from source nodes to downstream nodes.

Propagation rule:

V_target = V_source − ΔV_edge

Where:

ΔV_edge = voltage drop across cable.

Voltage propagation continues until:

- load nodes
- open circuit
- protection interruption

------------------------------------------------------------

# Step 6 — Load Current Calculation

For each load node:

Load current is calculated.

Single-phase:

I = P / (V × cosφ)

Three-phase:

I = P / (√3 × V × cosφ)

The load current becomes the demand of the node.

------------------------------------------------------------

# Step 7 — Current Back-Propagation

Currents propagate upstream from loads to sources.

Algorithm:

1. start at load nodes
2. push current to upstream edges
3. aggregate currents at junction nodes

At junction nodes:

I_total = Σ branch currents

This allows calculation of:

- cable currents
- breaker currents
- inverter output currents

------------------------------------------------------------

# Step 8 — Cable and Protection Verification

For each edge:

Check:

I_edge ≤ I_cable

For each protection device:

I_edge ≤ I_breaker

Violations generate validation errors.

------------------------------------------------------------

# Step 9 — Energy Flow Mapping

Energy flow paths are recorded.

Possible flows:

solar → load  
solar → battery  
solar → grid  
battery → load  
grid → load

This enables system simulation.

------------------------------------------------------------

# Step 10 — Result Aggregation

Final results are stored in graph elements.

Nodes store:

voltage  
phase  
connected loads

Edges store:

current  
power flow  
voltage drop

Components store:

power generation  
power consumption  
efficiency

------------------------------------------------------------

# Graph Traversal Algorithms

Two traversal algorithms are used.

Breadth First Search (BFS)

Used for:

voltage propagation  
circuit detection

Depth First Search (DFS)

Used for:

loop detection  
path discovery

------------------------------------------------------------

# Junction Current Rule

At any junction node:

Incoming current equals outgoing current.

Kirchhoff Current Law:

Σ I_in = Σ I_out

------------------------------------------------------------

# Voltage Loop Rule

For any closed loop:

Σ voltage rises = Σ voltage drops

This ensures electrical consistency.

------------------------------------------------------------

# Bidirectional Flow

Some components support bidirectional power flow.

Examples:

battery  
hybrid inverter

Flow direction may change depending on system state.

------------------------------------------------------------

# Iterative Calculations

In complex systems the calculation may require iterations.

Example cases:

battery charging
multiple sources
hybrid systems

Algorithm:

repeat until convergence

------------------------------------------------------------

# Performance Requirements

Expected graph sizes:

small system → 50 nodes  
medium system → 200 nodes  
large system → 1000 nodes

Traversal complexity:

O(N + E)

------------------------------------------------------------

# Future Extensions

Future versions may support advanced power system analysis.

Possible extensions:

AC load flow (Newton-Raphson)
short-circuit analysis
fault current calculation
harmonic propagation
dynamic stability analysis

------------------------------------------------------------

# Strategic Importance

The graph calculation flow is the computational core
of SolarDiagramApp.

All electrical analysis depends on this algorithm.

The diagram editor only visualizes the graph;
the graph model defines the real electrical behavior.