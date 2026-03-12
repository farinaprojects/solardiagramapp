# SolarDiagramApp — Editor Engine

This document describes the architecture of the diagram editor.

The editor is responsible for diagram interaction.

------------------------------------------------------------

# Editor Responsibilities

The editor allows users to:

- place components
- connect cables
- select elements
- manipulate diagrams
- visualize electrical structures

------------------------------------------------------------

# Editor Modules

SnapEngine  
EditorAlignmentEngine  
AutoWireRouter  
DiagramAutoLayoutEngine  
WireRenderer  
EditorHitTest

------------------------------------------------------------

# Editor Constraints

The editor must remain independent from electrical rules.

The editor only manipulates:

- components
- ports
- wires
- layout

------------------------------------------------------------

# Interaction Flow

User interaction follows this sequence:

User Input
→ Editor Interaction
→ ViewModel Command
→ Domain Update


------------------------------------------------------------

# Visual vs Physical Model

Visual elements represent domain entities.

Example:

VisualComponent  
→ represents → ElectricalComponent

------------------------------------------------------------

# Future Editor Improvements

Planned improvements include:

- orthogonal cable routing
- advanced snapping
- automatic layout
- large diagram performance
- zoom-independent rendering