# SolarDiagramApp — Component Catalog

This document defines the complete list of components supported by the system.

The purpose of this catalog is to standardize:

- component identifiers
- electrical characteristics
- connection interfaces
- validation rules

This ensures that the SolarDiagramApp evolves with a consistent engineering model.

------------------------------------------------------------

# Component Categories

Components are grouped into the following domains:

1. Solar Generation
2. Inverters
3. Energy Storage
4. Electrical Protection
5. Electrical Distribution
6. Loads
7. Measurement
8. Energy Management
9. Auxiliary Systems
10. Infrastructure

------------------------------------------------------------

# 1 — Solar Generation Components

## PV Module

Represents a photovoltaic panel.

Properties:

- nominal power (W)
- Voc
- Vmp
- Isc
- Imp
- efficiency
- temperature coefficient

Connectors:

- DC+
- DC-

------------------------------------------------------------

## PV String

Series connection of modules.

Properties:

- module count
- string voltage
- string current

------------------------------------------------------------

## PV Array

Group of strings.

Properties:

- total power
- number of strings

------------------------------------------------------------

## DC Combiner Box

Combines multiple PV strings.

Properties:

- input strings
- fuse rating
- surge protection

Connectors:

- multiple DC inputs
- DC output

------------------------------------------------------------

## DC Disconnect Switch

Manual isolation switch for DC circuits.

------------------------------------------------------------

# 2 — Inverter Systems

## Microinverter

Installed per module.

Connectors:

- DC input
- AC output
- PE

------------------------------------------------------------

## String Inverter

Connected to multiple strings.

Properties:

- MPPT count
- power rating
- voltage range

Connectors:

- DC input
- AC output
- PE

------------------------------------------------------------

## Hybrid Inverter

Supports batteries and grid.

Connectors:

- PV input
- battery
- grid
- load

------------------------------------------------------------

## Off-grid Inverter

Used in isolated systems.

Properties:

- rated power
- surge power
- battery voltage

------------------------------------------------------------

# 3 — Energy Storage

## Battery

Energy storage unit.

Properties:

- chemistry
- voltage
- capacity
- maximum current
- depth of discharge

------------------------------------------------------------

## Battery Bank

Group of batteries.

Configurations:

- series
- parallel

------------------------------------------------------------

## Battery Management System (BMS)

Monitors and protects battery cells.

Functions:

- voltage monitoring
- temperature monitoring
- balancing
- protection

------------------------------------------------------------

# 4 — Electrical Protection

## Circuit Breaker

Protects against overcurrent.

Types:

- single pole
- double pole
- triple pole

------------------------------------------------------------

## DC Fuse

Used in PV string protection.

------------------------------------------------------------

## Surge Protection Device (SPD)

Protects against lightning and surges.

Types:

- Type I
- Type II
- Type III

------------------------------------------------------------

## Residual Current Device (RCD)

Detects leakage current.

------------------------------------------------------------

## Ground Fault Protection

Detects insulation failure.

------------------------------------------------------------

## Arc Fault Detection

Detects electrical arc events.

------------------------------------------------------------

# 5 — Electrical Distribution

## AC Busbar

Electrical distribution bar.

Variants:

- L1
- L2
- L3
- N
- PE

------------------------------------------------------------

## Electrical Panel

Main distribution board.

------------------------------------------------------------

## Subpanel

Secondary distribution board.

------------------------------------------------------------

## Transfer Switch

Switches between grid and backup.

Types:

- manual
- automatic

------------------------------------------------------------

# 6 — Loads

## Generic Load

Basic electrical load.

------------------------------------------------------------

## Single Phase Load

Connectors:

- L
- N
- PE

------------------------------------------------------------

## Two Phase Load

Connectors:

- L1
- L2
- N
- PE

------------------------------------------------------------

## Three Phase Load

Connectors:

- L1
- L2
- L3
- N
- PE

------------------------------------------------------------

## Critical Load

Load with backup priority.

------------------------------------------------------------

# 7 — Measurement

## Energy Meter

Measures electrical energy.

Properties:

- voltage
- current
- power
- energy

------------------------------------------------------------

## Bidirectional Meter

Used in grid-connected systems.

Measures:

- import
- export

------------------------------------------------------------

## Production Meter

Measures solar production.

------------------------------------------------------------

## Consumption Meter

Measures building consumption.

------------------------------------------------------------

# 8 — Energy Management

## Energy Management System (EMS)

Controls energy flows.

Functions:

- load prioritization
- battery dispatch
- grid export control

------------------------------------------------------------

## Load Controller

Controls non-critical loads.

------------------------------------------------------------

## Smart Relay

Switches loads automatically.

------------------------------------------------------------

# 9 — Auxiliary Systems

## Generator

Backup generator.

------------------------------------------------------------

## EV Charger

Electric vehicle charger.

------------------------------------------------------------

## ATS (Automatic Transfer Switch)

Automatically switches power sources.

------------------------------------------------------------

# 10 — Infrastructure

## Grounding System

Provides electrical grounding.

------------------------------------------------------------

## Lightning Protection

Protects against lightning strikes.

------------------------------------------------------------

## Cable

Electrical conductor.

Properties:

- cross section
- insulation
- maximum current

------------------------------------------------------------

## Conduit

Protects electrical cables.

------------------------------------------------------------

# Total Component Count

The system currently supports approximately:

- 15 solar components
- 10 inverter types
- 10 battery/storage components
- 20 protection devices
- 10 distribution elements
- 10 load types
- 10 measurement devices
- 5 energy management systems
- 5 auxiliary components
- 5 infrastructure components

Total potential components: **~100 components**

------------------------------------------------------------

# Future Extensions

The catalog may expand to include:

- wind generation
- microgrids
- vehicle-to-grid systems
- hydrogen storage
- smart home integration