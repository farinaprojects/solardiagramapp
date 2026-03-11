package br.com.solardiagram.ui.screens.editor

sealed interface EditorSelectionContext {
    data object None : EditorSelectionContext
    data class SingleComponent(val name: String) : EditorSelectionContext
    data class MultipleComponents(val count: Int) : EditorSelectionContext
    data object Connection : EditorSelectionContext
}
