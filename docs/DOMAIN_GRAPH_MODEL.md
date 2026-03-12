# SolarDiagramApp — Domain Graph Model

This document defines the electrical graph model used internally by SolarDiagramApp.

The electrical installation is represented as a **directed graph of electrical components and connections**.

This graph structure allows the system to perform:

- electrical validation
- topology analysis
- current propagation
- voltage propagation
- fault detection
- energy flow simulation

------------------------------------------------------------

# Conceptual Model

An electrical installation is modeled as a graph.

Graph = Nodes + Edges

Nodes represent connection points.
Edges represent electrical conductors or internal component links.

------------------------------------------------------------

# Core Graph Entities

The core graph elements are:

ElectricalGraph  
ElectricalNode  
ElectricalEdge  
ElectricalComponent  
ElectricalPort  
ElectricalFlow

------------------------------------------------------------

# ElectricalGraph

Represents the entire electrical installation.

Responsibilities:

- store nodes
- store edges
- maintain connectivity
- perform traversal algorithms
- support analysis engines

Properties:

nodes : List<ElectricalNode>  
edges : List<ElectricalEdge>  
components : List<ElectricalComponent>

------------------------------------------------------------

# ElectricalNode

A node represents an electrical connection point.

Examples:

- busbars
- junctions
- terminals
- connection points

Properties:

nodeId  
connectedEdges  
voltage  
phase

Nodes allow the graph to connect multiple components.

------------------------------------------------------------

# ElectricalEdge

An edge represents a conductive path.

Examples:

- cables
- wires
- internal connections
- busbars

Properties:

edgeId  
sourceNode  
targetNode  
resistance  
length  
current

Edges allow current flow between nodes.

------------------------------------------------------------

# ElectricalComponent

A component is a physical device.

Examples:

- PV module
- inverter
- breaker
- load
- battery

Components expose connection points called ports.

------------------------------------------------------------

# ElectricalPort

Ports represent the electrical terminals of components.

Examples:

DC+  
DC−  
L1  
L2  
L3  
N  
PE

Each port is connected to a node in the graph.

------------------------------------------------------------

# Graph Construction

Graph creation occurs through the following process:

1. Components are created.
2. Ports are attached to components.
3. Ports connect to nodes.
4. Wires connect nodes.

This produces the final electrical graph.

------------------------------------------------------------

# Example Graph

Example circuit:

PV Module → Inverter → Breaker → Load

Graph representation:

Nodes:

N1 PV positive  
N2 PV negative  
N3 inverter AC output  
N4 breaker output  
N5 load terminal

Edges:

PV cable  
AC cable  
breaker internal connection

------------------------------------------------------------

# Graph Traversal

Graph traversal allows the system to analyze the installation.

Algorithms used:

Depth First Search (DFS)  
Breadth First Search (BFS)

Applications:

circuit discovery  
island detection  
loop detection

------------------------------------------------------------

# Circuit Identification

A circuit is a connected subgraph containing:

- one power source
- one or more loads
- conductive paths

Circuit detection algorithm:

1. identify power sources
2. traverse graph downstream
3. collect reachable nodes

------------------------------------------------------------

# Voltage Propagation

Voltage propagates from sources through edges.

Example:

Inverter output → 220V AC

Propagation steps:

1. assign voltage to source node
2. propagate along edges
3. apply voltage drops
4. update downstream nodes

------------------------------------------------------------

# Current Propagation

Current flows from sources to loads.

Steps:

1. identify load power
2. calculate load current
3. propagate current upstream
4. aggregate branch currents

------------------------------------------------------------

# Energy Flow Graph

Energy flow may occur through multiple paths.

Possible flows:

solar → load  
solar → battery  
solar → grid  
battery → load  
grid → load

The graph enables dynamic flow analysis.

------------------------------------------------------------

# Island Detection

An island is a subgraph disconnected from the grid.

Island detection is required for:

- anti-islanding protection
- off-grid operation

Algorithm:

remove grid node  
check connected components

------------------------------------------------------------

# Loop Detection

Unintended loops must be detected.

Loops can cause:

- incorrect power calculations
- unsafe connections

Cycle detection algorithms are used.

------------------------------------------------------------

# Graph Validation

The graph must satisfy:

- no floating nodes
- no incompatible connections
- valid phase matching
- valid circuit topology

These checks are implemented in the validation engines.

------------------------------------------------------------

# Graph Update Model

Graph updates occur when:

components are added  
components are removed  
connections are modified

After each modification:

1. graph rebuild
2. validation
3. recalculation

------------------------------------------------------------

# Performance Considerations

Large installations may contain:

hundreds of nodes  
hundreds of edges

Graph operations must remain efficient.

Recommended complexity:

Traversal: O(N + E)

------------------------------------------------------------

# Long Term Graph Capabilities

The graph model will support future features:

short circuit analysis  
fault propagation  
load flow analysis  
harmonic analysis  
power quality analysis

------------------------------------------------------------

# Strategic Importance

The graph model is the core of SolarDiagramApp.

Every system capability depends on the correctness of this model.

The diagram editor is only a visual interface for this underlying graph.