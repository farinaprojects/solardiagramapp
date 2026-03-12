# SolarDiagramApp — Electrical Rules Engine

This document defines the electrical validation rules implemented by the
SolarDiagramApp rule engine.

The goal of the rules engine is to automatically detect design errors
in electrical and photovoltaic installations.

The rules are organized by domains:

1. Topology Rules
2. Electrical Connectivity Rules
3. Voltage Rules
4. Current Rules
5. Cable Rules
6. Protection Rules
7. Solar System Rules
8. Battery System Rules
9. Grid Connection Rules
10. Safety Rules

Each rule must produce a validation result:

VALID
WARNING
ERROR

------------------------------------------------------------

# 1 — Topology Rules

## TR-01 — Circuit Must Have Source

Every electrical circuit must contain at least one power source.

Sources may include:

- inverter
- generator
- battery inverter
- grid connection

------------------------------------------------------------

## TR-02 — Circuit Must Have Load

A circuit must contain at least one load element.

------------------------------------------------------------

## TR-03 — Closed Loop Detection

The system must detect unintended closed electrical loops.

------------------------------------------------------------

## TR-04 — Floating Nodes

Nodes that are not connected to a valid circuit must be flagged.

------------------------------------------------------------

## TR-05 — Invalid Busbar Connections

Busbars must only connect compatible phases.

------------------------------------------------------------

# 2 — Electrical Connectivity Rules

## CR-01 — Connector Compatibility

Only compatible connectors may connect.

Examples:

DC+ ↔ DC+  
AC-L1 ↔ AC-L1  
PE ↔ PE

Invalid examples:

DC ↔ AC  
L1 ↔ N

------------------------------------------------------------

## CR-02 — Maximum Connections

Each connector type may define a maximum number of connections.

------------------------------------------------------------

## CR-03 — Directional Flow

Some components enforce directional energy flow.

Example:

PV module → inverter

------------------------------------------------------------

# 3 — Voltage Rules

## VR-01 — Maximum Component Voltage

Voltage applied to a component must not exceed its rated voltage.

------------------------------------------------------------

## VR-02 — PV String Maximum Voltage

PV string open-circuit voltage must not exceed inverter maximum voltage.

------------------------------------------------------------

## VR-03 — Voltage Drop Limit

Voltage drop in cables must not exceed allowed limits.

Typical limits:

DC circuits: 3%  
AC circuits: 5%

------------------------------------------------------------

## VR-04 — Phase Voltage Compatibility

Single-phase loads must not connect to three-phase lines incorrectly.

------------------------------------------------------------

# 4 — Current Rules

## IR-01 — Cable Current Limit

Cable current must not exceed ampacity.

------------------------------------------------------------

## IR-02 — Breaker Current Limit

Breaker rating must exceed expected current.

------------------------------------------------------------

## IR-03 — Inverter Output Current

Load current must not exceed inverter rated output current.

------------------------------------------------------------

## IR-04 — PV String Current

String current must not exceed inverter MPPT input current.

------------------------------------------------------------

# 5 — Cable Rules

## CRB-01 — Cable Cross Section

Cable cross-section must be adequate for current.

------------------------------------------------------------

## CRB-02 — Cable Temperature Rating

Cable insulation must support expected temperature.

------------------------------------------------------------

## CRB-03 — Parallel Cable Rules

Parallel cables must share equal current distribution.

------------------------------------------------------------

# 6 — Protection Rules

## PR-01 — Overcurrent Protection

Every circuit must include overcurrent protection.

------------------------------------------------------------

## PR-02 — Protection Selectivity

Upstream protection must coordinate with downstream protection.

------------------------------------------------------------

## PR-03 — Surge Protection

PV installations must include surge protection.

------------------------------------------------------------

## PR-04 — Residual Current Protection

RCD must protect circuits with human contact risk.

------------------------------------------------------------

## PR-05 — DC Protection

PV strings must include DC protection devices.

------------------------------------------------------------

# 7 — Solar System Rules

## SR-01 — String Length

Minimum and maximum modules per string must be respected.

------------------------------------------------------------

## SR-02 — MPPT Compatibility

PV strings connected to an MPPT must be electrically compatible.

------------------------------------------------------------

## SR-03 — PV Power Ratio

PV array power must not exceed inverter maximum PV input power.

Typical limit:

120% to 150% of inverter power.

------------------------------------------------------------

## SR-04 — Temperature Voltage Correction

PV voltage must consider temperature coefficient.

------------------------------------------------------------

# 8 — Battery System Rules

## BR-01 — Battery Voltage Compatibility

Battery voltage must match inverter battery input.

------------------------------------------------------------

## BR-02 — Battery Current Limit

Battery discharge current must not exceed rated current.

------------------------------------------------------------

## BR-03 — Battery Depth of Discharge

System must respect maximum depth of discharge.

------------------------------------------------------------

## BR-04 — Charge Controller Compatibility

PV power must not exceed charge controller rating.

------------------------------------------------------------

# 9 — Grid Connection Rules

## GR-01 — Anti-Islanding

Grid-connected systems must include anti-islanding capability.

------------------------------------------------------------

## GR-02 — Grid Phase Matching

Inverter output phase must match grid connection phase.

------------------------------------------------------------

## GR-03 — Bidirectional Metering

Grid-exporting systems must include bidirectional metering.

------------------------------------------------------------

# 10 — Safety Rules

## SRF-01 — Grounding Required

All metallic structures must be grounded.

------------------------------------------------------------

## SRF-02 — PE Continuity

Protective earth must form a continuous path.

------------------------------------------------------------

## SRF-03 — Lightning Protection

Systems exposed to lightning risk must include surge protection.

------------------------------------------------------------

## SRF-04 — Emergency Disconnect

Installations must include emergency disconnection capability.

------------------------------------------------------------

# Rule Engine Architecture

The rule engine operates through modular validators.

Example architecture:

TopologyValidationEngine  
ProtectionValidationEngine  
ElectricalValidationEngine  
SolarValidationEngine  
BatteryValidationEngine

Each validator applies a group of rules.

------------------------------------------------------------

# Validation Output

Each rule produces a validation result:

ERROR → installation unsafe  
WARNING → engineering improvement recommended  
INFO → design note

------------------------------------------------------------

# Long Term Goal

The rules engine will evolve to support:

- automatic design validation
- engineering compliance checks
- photovoltaic system certification
- real-time operational diagnostics