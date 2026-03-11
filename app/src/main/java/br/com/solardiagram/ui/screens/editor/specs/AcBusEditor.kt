package br.com.solardiagram.ui.screens.editor.specs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import br.com.solardiagram.domain.model.ElectricalSpecs
import br.com.solardiagram.domain.model.SystemPhase
import androidx.compose.ui.unit.dp

@Composable
fun AcBusEditor(
    specs: ElectricalSpecs.AcBusSpecs,
    onChange: (ElectricalSpecs.AcBusSpecs) -> Unit
) {
    var maxCurrentText by remember(specs) { mutableStateOf(formatNumber(specs.maxBusCurrentA)) }

    //Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.dp(8))) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Barramento AC", style = MaterialTheme.typography.titleSmall)
        Text("Topologia: ${phaseLabel(specs.phases)}")

        OutlinedTextField(
            value = maxCurrentText,
            onValueChange = {
                maxCurrentText = it
                val parsed = it.replace(',', '.').toDoubleOrNull()
                if (parsed != null) {
                    onChange(specs.copy(maxBusCurrentA = parsed))
                }
            },
            label = { Text("Corrente máxima (A)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Neutro: ${if (specs.hasNeutral) "sim" else "não"} • Terra: ${if (specs.hasGround) "sim" else "não"}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun phaseLabel(phase: SystemPhase): String = when (phase) {
    SystemPhase.MONO -> "Monofásico"
    SystemPhase.BI -> "Bifásico"
    SystemPhase.TRI -> "Trifásico"
}

private fun formatNumber(value: Double): String {
    val asLong = value.toLong()
    return if (value == asLong.toDouble()) asLong.toString() else value.toString()
}
