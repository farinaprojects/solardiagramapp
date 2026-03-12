# SolarDiagramApp — Development Guidelines

This document defines the development rules of the project.

These guidelines ensure the system evolves with stability.

------------------------------------------------------------

# Core Principles

## Stability before speed

No feature should break existing functionality.

Every change must preserve:

- compilation stability
- behavior of existing features
- architectural integrity

------------------------------------------------------------

# Incremental Evolution

Large changes must be implemented in small steps.

Each phase must be:

- compilable
- testable
- reviewable

------------------------------------------------------------

# Domain First

The domain model always has priority over UI.

Electrical correctness must never depend on UI code.

------------------------------------------------------------

# Editor Safety

The diagram editor must remain stable.

Editor code must avoid:

- heavy logic
- electrical rules
- domain calculations

The editor should only manipulate visual objects.

------------------------------------------------------------

# Code Organization

Domain logic must remain inside:
domain/


UI code must remain inside:


ui/


Persistence code must remain inside:


data/


------------------------------------------------------------

# Validation

Electrical validation must be implemented through engines.

Never hardcode rules inside UI.

------------------------------------------------------------

# Documentation

Any architectural change must update:

- ARCHITECTURE.md
- ROADMAP.md

------------------------------------------------------------

# Long Term Objective

The project must grow into a professional engineering system.

Short-term shortcuts that harm architecture must be avoided.