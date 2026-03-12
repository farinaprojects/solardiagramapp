# SolarDiagramApp — Electrical Component Specification

This document defines how each electrical component behaves within the
SolarDiagramApp electrical graph model.

Each component specification defines:

- component identifier
- electrical ports
- electrical behavior
- validation rules
- calculation interactions

Components connect to the graph through **ports**.

Each port is mapped to an **ElectricalNode**.

------------------------------------------------------------

# Component Structure

All components must follow the base structure.

Component Properties:

componentId  
componentType  
ports  
electricalProperties

------------------------------------------------------------

# Port Definition

Ports represent electrical terminals.

Each port defines:

portId  
portType  
phase  
direction

Example port types:

DC_POSITIVE  
DC_NEGATIVE  
AC_PHASE  
NEUTRAL  
PROTECTIVE_EARTH

------------------------------------------------------------

# Connection Rules

Ports must only connect to compatible ports.

Examples:

DC+ ↔ DC+  
AC L1 ↔ AC L1  
PE ↔ PE

Invalid connections must be rejected by validation engines.

------------------------------------------------------------

# 1 — PV Module

Component Type:

PV_MODULE

Ports:

DC+  
DC−

Electrical Behavior:

Generates DC voltage when irradiated.

Graph Behavior:

Acts as **energy source node**.

Key Parameters:

Voc  
Vmp  
Isc  
Imp  
Pmax

------------------------------------------------------------

# 2 — PV String

Component Type:

PV_STRING

Ports:

STRING_POSITIVE  
STRING_NEGATIVE

Behavior:

Series connection of modules.

Voltage:

Vstring = N × Vmodule

Current:

Istring = Imp

------------------------------------------------------------

# 3 — DC Combiner Box

Component Type:

DC_COMBINER

Ports:

DC_IN_1  
DC_IN_2  
DC_IN_3  
DC_IN_N  
DC_OUT

Behavior:

Combines multiple strings into a single output.

Internal graph:

Multiple inputs → single node → output

------------------------------------------------------------

# 4 — Microinverter

Component Type:

MICROINVERTER

Ports:

DC+  
DC−  
AC_L  
AC_N  
PE

Behavior:

Converts DC from module into AC.

Graph Behavior:

DC source → AC generator.

------------------------------------------------------------

# 5 — String Inverter

Component Type:

STRING_INVERTER

Ports:

PV_POS  
PV_NEG  
AC_L1  
AC_L2  
AC_L3  
AC_N  
PE

Behavior:

Converts DC from PV strings into AC.

Acts as AC power source.

------------------------------------------------------------

# 6 — Hybrid Inverter

Component Type:

HYBRID_INVERTER

Ports:

PV_INPUT  
BATTERY_POS  
BATTERY_NEG  
GRID_AC  
LOAD_AC  
PE

Behavior:

Bidirectional power flow.

Possible energy flows:

PV → load  
PV → battery  
battery → load  
grid → battery

------------------------------------------------------------

# 7 — Battery

Component Type:

BATTERY

Ports:

BATTERY_POS  
BATTERY_NEG

Behavior:

Energy storage element.

Graph Behavior:

Bidirectional energy node.

------------------------------------------------------------

# 8 — Circuit Breaker

Component Type:

BREAKER

Ports:

LINE_IN  
LINE_OUT

Behavior:

Interrupts current when rating exceeded.

Graph Behavior:

Edge interruption element.

------------------------------------------------------------

# 9 — Surge Protection Device

Component Type:

SPD

Ports:

LINE  
GROUND

Behavior:

Diverts surge currents to ground.

------------------------------------------------------------

# 10 — Busbar

Component Type:

BUSBAR

Ports:

MULTIPLE_PHASE_PORTS

Behavior:

Electrical junction.

Graph Behavior:

Node aggregator.

------------------------------------------------------------

# 11 — Load

Component Type:

LOAD

Ports:

L  
N  
PE

Behavior:

Consumes electrical energy.

Current calculation:

I = P / V

------------------------------------------------------------

# 12 — Energy Meter

Component Type:

ENERGY_METER

Ports:

LINE_IN  
LINE_OUT

Behavior:

Measures energy flow.

Graph Behavior:

Transparent measurement node.

------------------------------------------------------------

# 13 — Grid Connection

Component Type:

GRID_CONNECTION

Ports:

L1  
L2  
L3  
N  
PE

Behavior:

External power source.

------------------------------------------------------------

# 14 — Generator

Component Type:

GENERATOR

Ports:

AC_OUTPUT  
PE

Behavior:

Backup energy source.

------------------------------------------------------------

# Graph Behavior Summary

Components may behave as:

SOURCE  
LOAD  
CONVERTER  
JUNCTION  
PROTECTION  
MEASUREMENT

------------------------------------------------------------

# Graph Interaction

Each component interacts with the graph as follows:

SOURCE → injects voltage  
LOAD → consumes current  
CONVERTER → transforms energy  
JUNCTION → distributes current  
PROTECTION → interrupts flow

------------------------------------------------------------

# Validation Hooks

Each component may define validation rules.

Examples:

PV module → voltage compatibility  
inverter → MPPT compatibility  
breaker → current rating

------------------------------------------------------------

# Future Component Extensions

Future components may include:

EV chargers  
wind generators  
hydrogen storage  
microgrid controllers

------------------------------------------------------------

# Engineering Importance

Component specifications ensure:

consistent behavior in the graph  
correct electrical analysis  
predictable simulation results