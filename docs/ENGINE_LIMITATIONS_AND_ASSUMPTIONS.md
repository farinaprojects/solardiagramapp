# SolarDiagramApp — Engine Limitations and Assumptions

This document describes the engineering assumptions and limitations of the
SolarDiagramApp calculation engine.

The goal is to clearly define:

- mathematical simplifications
- modeling assumptions
- current analysis scope
- unsupported scenarios

This transparency ensures that the system is used correctly and
that future improvements remain compatible with the original design.

------------------------------------------------------------

# Design Philosophy

The SolarDiagramApp engine is designed as a **practical engineering tool**,
not as a full power system simulation platform.

The objective is to provide:

- reliable electrical validation
- accurate engineering estimations
- simplified power flow analysis

while maintaining performance and usability.

------------------------------------------------------------

# 1 — Electrical Model Simplifications

## Lumped Element Model

Electrical components are modeled as lumped elements.

Distributed effects such as:

- electromagnetic coupling
- transmission line propagation
- skin effect

are not modeled.

This simplification is acceptable for low-voltage installations.

------------------------------------------------------------

# 2 — Resistive Cable Model

Cables are modeled using resistance only.

Cable impedance is simplified to:

R

Reactive impedance (X) is not modeled.

This assumption is acceptable for:

- short cables
- low voltage systems
- residential installations

Future versions may support full impedance modeling.

------------------------------------------------------------

# 3 — Steady State Analysis

The engine performs **steady-state calculations only**.

Transient phenomena are not simulated.

Examples not currently modeled:

- switching transients
- motor startup transients
- lightning surge propagation
- harmonic distortion

------------------------------------------------------------

# 4 — Balanced Three-Phase Assumption

For most calculations, three-phase systems are assumed balanced.

Meaning:

- equal voltage magnitude
- equal phase load distribution

Unbalanced phase analysis is currently limited.

------------------------------------------------------------

# 5 — Simplified Power Flow

The power flow model used is a simplified propagation model.

Voltage and current propagate along the graph using basic equations.

The engine does NOT implement:

- Newton-Raphson load flow
- Gauss-Seidel load flow
- AC optimal power flow

These methods may be added in future versions.

------------------------------------------------------------

# 6 — Solar Production Estimation

Solar production calculations use simplified models.

Production estimation uses:

Peak Power × Sun Hours × Efficiency

This method does not include:

- detailed irradiance models
- shading simulation
- spectral corrections
- temperature time-series simulation

These features may be implemented in later versions.

------------------------------------------------------------

# 7 — Battery Modeling

Battery behavior is simplified.

Current model includes:

- nominal voltage
- energy capacity
- depth of discharge

The engine does not currently simulate:

- battery internal resistance
- dynamic charge curves
- battery aging
- temperature effects

------------------------------------------------------------

# 8 — Energy Flow Simplifications

Energy flow prioritization is rule-based.

Flow decisions follow predefined rules such as:

solar → load → battery → grid

The engine does not currently perform optimization-based dispatch.

------------------------------------------------------------

# 9 — Protection Coordination

Protection devices are validated using rating comparisons.

Full protection coordination analysis is not implemented.

Not modeled:

- time-current curves
- selective coordination analysis
- arc energy calculations

------------------------------------------------------------

# 10 — Short Circuit Analysis

Short circuit calculations are not currently implemented.

The engine does not compute:

- fault currents
- symmetrical components
- fault impedance

Future versions may include:

IEC 60909 short-circuit calculation.

------------------------------------------------------------

# 11 — Harmonic Analysis

The engine does not currently simulate harmonic distortion.

Examples not modeled:

- inverter harmonics
- nonlinear load harmonics
- harmonic resonance

------------------------------------------------------------

# 12 — Grid Interaction

Grid behavior is simplified.

The grid is modeled as an infinite source with constant voltage.

Grid dynamics such as:

- frequency variations
- voltage regulation
- protection trips

are not simulated.

------------------------------------------------------------

# 13 — Thermal Effects

Temperature effects are simplified.

Cable temperature rise due to load is not dynamically simulated.

Battery temperature effects are not modeled.

------------------------------------------------------------

# 14 — Environmental Effects

Environmental conditions are simplified.

Examples not modeled:

- shading changes
- seasonal irradiance variations
- snow accumulation
- dust degradation

------------------------------------------------------------

# 15 — Graph Model Assumptions

The electrical graph assumes:

- deterministic connectivity
- stable topology
- instantaneous propagation

Time-dependent network changes are not modeled.

------------------------------------------------------------

# 16 — Performance Trade-Offs

The calculation engine prioritizes performance.

Target performance:

small systems (<100 nodes) → real-time  
medium systems (<500 nodes) → near real-time  
large systems (<1000 nodes) → seconds

Highly complex simulations are intentionally avoided.

------------------------------------------------------------

# Future Improvements

Future versions of SolarDiagramApp may include:

- AC load flow analysis
- short-circuit calculations
- harmonic analysis
- dynamic simulation
- advanced solar modeling
- detailed battery models

These improvements will be implemented gradually
without breaking the existing architecture.

------------------------------------------------------------

# Engineering Responsibility

Users must understand that the SolarDiagramApp engine provides
engineering assistance, not final certification.

Final electrical designs must always comply with applicable
engineering standards and regulations.

------------------------------------------------------------

# Strategic Role

Documenting assumptions and limitations ensures that:

- calculations remain interpretable
- engineering responsibility is clear
- future development is well guided