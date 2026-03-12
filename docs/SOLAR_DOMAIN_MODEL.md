# SolarDiagramApp — Solar Domain Model

This document defines the photovoltaic domain model.

------------------------------------------------------------

# System Hierarchy

SolarInstallation  
→ PVArray  
→ PVString  
→ PVModule

------------------------------------------------------------

# PV Module

Represents a solar panel.

Properties:

- nominal power
- Voc
- Vmp
- Isc
- Imp
- efficiency
- temperature coefficient

Terminals:

DC+  
DC-

------------------------------------------------------------

# PV String

Modules connected in series.

Properties:

- number of modules
- string voltage
- string current

------------------------------------------------------------

# PV Array

Collection of strings.

Properties:

- total power
- string count

------------------------------------------------------------

# Inverter

Converts DC to AC.

Types:

Microinverter  
String inverter  
Hybrid inverter  
Off-grid inverter

------------------------------------------------------------

# Battery

Represents energy storage.

Properties:

- nominal voltage
- capacity Ah
- capacity kWh
- depth of discharge

------------------------------------------------------------

# Grid Connection

Represents connection to the electrical grid.

Possible flows:

solar → load  
solar → grid  
solar → battery  
battery → load  
grid → load

------------------------------------------------------------

# Operation Modes

OnGrid  
OffGrid  
Hybrid  
Backup