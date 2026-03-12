# SolarDiagramApp — Calculation Engine

This document defines the mathematical and engineering calculation models
used by SolarDiagramApp.

The calculation engine is responsible for performing electrical analysis,
dimensioning, and validation of electrical and photovoltaic systems.

The calculation engine is divided into multiple specialized modules.

------------------------------------------------------------

# Calculation Engine Modules

The calculation engine is composed of:

Electrical Calculation Engine
Cable Calculation Engine
Protection Calculation Engine
Solar Calculation Engine
Battery Calculation Engine
Energy Simulation Engine

------------------------------------------------------------

# 1 — Electrical Calculation Engine

Responsible for fundamental electrical calculations.

------------------------------------------------------------

## Current Calculation

Current is calculated from power and voltage.

Formula:

I = P / V

Where:

I = current (A)
P = power (W)
V = voltage (V)

For AC circuits:

I = P / (V × cosφ)

Where:

cosφ = power factor

------------------------------------------------------------

## Three Phase Current

For three-phase systems:

I = P / (√3 × V × cosφ)

Where:

√3 ≈ 1.732

------------------------------------------------------------

## Apparent Power

Apparent power:

S = V × I

------------------------------------------------------------

## Reactive Power

Reactive power:

Q = S × sinφ

------------------------------------------------------------

# 2 — Voltage Drop Calculation

Voltage drop must be calculated for each cable.

------------------------------------------------------------

## Single Phase Voltage Drop

ΔV = (2 × L × I × R) / 1000

Where:

ΔV = voltage drop
L = cable length (m)
I = current (A)
R = cable resistance (Ω/km)

------------------------------------------------------------

## Three Phase Voltage Drop

ΔV = (√3 × L × I × R) / 1000

------------------------------------------------------------

## Voltage Drop Percentage

Voltage drop percentage:

ΔV% = (ΔV / V) × 100

Recommended limits:

DC circuits ≤ 3%
AC circuits ≤ 5%

------------------------------------------------------------

# 3 — Cable Sizing Engine

Determines minimum cable cross-section.

------------------------------------------------------------

## Ampacity Rule

Cable current capacity must satisfy:

Icable ≥ Iload × safety factor

Typical safety factor:

1.25

------------------------------------------------------------

## Thermal Correction

Cable capacity must consider:

- ambient temperature
- installation method
- grouping of cables

------------------------------------------------------------

# 4 — Protection Calculation Engine

Responsible for breaker and protection sizing.

------------------------------------------------------------

## Breaker Sizing

Breaker rating must satisfy:

Iload ≤ Ibreaker ≤ Icable

------------------------------------------------------------

## Coordination Rule

Upstream breaker must have greater rating than downstream breaker.

------------------------------------------------------------

# 5 — Solar Calculation Engine

Calculations related to photovoltaic systems.

------------------------------------------------------------

# PV Module Parameters

Each PV module contains:

Voc
Vmp
Isc
Imp
Pmax

------------------------------------------------------------

## PV String Voltage

Total string voltage:

Vstring = N × Vmodule

Where:

N = number of modules

------------------------------------------------------------

## PV String Current

String current equals module current:

Istring = Imp

------------------------------------------------------------

## PV Array Power

Total array power:

Parray = Nmodules × Pmodule

------------------------------------------------------------

## Temperature Correction

PV voltage increases in low temperatures.

Corrected voltage:

Vcorrected = Voc × (1 + temperature coefficient × ΔT)

Where:

ΔT = difference from STC temperature.

------------------------------------------------------------

## Inverter Compatibility

PV string voltage must satisfy:

Vmin_MPPT ≤ Vstring ≤ Vmax_MPPT

------------------------------------------------------------

# 6 — Battery Calculation Engine

Responsible for battery sizing and autonomy.

------------------------------------------------------------

## Battery Capacity

Capacity in Wh:

Capacity = Vbattery × Ah

------------------------------------------------------------

## Battery Energy

Energy in kWh:

E = (V × Ah) / 1000

------------------------------------------------------------

## Battery Autonomy

Autonomy time:

Autonomy = Battery Energy / Load Power

------------------------------------------------------------

## Depth of Discharge

Usable energy:

Eusable = Ebattery × DoD

Example:

DoD = 0.8 (80%)

------------------------------------------------------------

# 7 — Energy Simulation Engine

Responsible for estimating energy production and consumption.

------------------------------------------------------------

## Solar Production

Daily energy production:

E = Psystem × Hsun × η

Where:

Psystem = installed power
Hsun = sun hours
η = system efficiency

Typical efficiency:

0.75 to 0.85

------------------------------------------------------------

## Annual Production

Eyear = Edaily × 365

------------------------------------------------------------

# 8 — Load Profile Calculation

Loads may have different time profiles.

Examples:

Residential
Commercial
Industrial

The system must support hourly consumption curves.

------------------------------------------------------------

# 9 — Energy Flow Calculation

Energy may flow through multiple paths:

solar → load
solar → battery
solar → grid
battery → load
grid → load

Energy flow simulation must consider:

- priority rules
- inverter limits
- battery state of charge

------------------------------------------------------------

# 10 — Future Calculation Modules

Future versions may include:

Short-circuit calculation
Protection coordination
Power quality analysis
Harmonic analysis
Fault current analysis

------------------------------------------------------------

# Engine Architecture

Calculation modules must remain independent.

Example structure:

ElectricalCalculationEngine
SolarCalculationEngine
BatteryCalculationEngine
EnergySimulationEngine

Each module must operate on the domain model.

------------------------------------------------------------

# Long-Term Vision

The calculation engine will evolve into a full engineering analysis engine capable of:

automatic system dimensioning
electrical validation
energy simulation
system optimization