package br.com.solardiagram.ui.screens.editor

import androidx.compose.ui.graphics.Color
import br.com.solardiagram.domain.model.PortKind

object ElectricalColorScheme {
    fun colorForPortKind(kind: PortKind): Color = when (kind) {
        PortKind.AC_L -> Color(0xFFD32F2F)
        PortKind.AC_N -> Color(0xFF2E7D32)
        PortKind.AC_PE, PortKind.PE -> Color(0xFF212121)
        PortKind.DC_POS -> Color(0xFFC62828)
        PortKind.DC_NEG -> Color(0xFF1565C0)
    }
}
