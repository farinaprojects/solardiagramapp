package br.com.solardiagram.ui.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.solardiagram.domain.model.*
import br.com.solardiagram.ui.viewmodel.EditorViewModel

@Composable
fun ConnectionPropertiesPanel(
    connection: Connection,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val m = connection.meta
    Card(modifier = modifier.fillMaxHeight().width(320.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Trecho (fio)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onClose) { Text("Fechar") }
            }

            Text("Presets", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { applyPreset(viewModel, connection, presetConduitExposedCu()) }) { Text("Eletrod. aparente") }
                OutlinedButton(onClick = { applyPreset(viewModel, connection, presetConduitEmbeddedCu()) }) { Text("Eletrod. embut.") }
            }

            Divider()

            var label by remember(connection.id) { mutableStateOf(m.label.orEmpty()) }
            var length by remember(connection.id) { mutableStateOf(m.lengthMeters?.toString().orEmpty()) }
            var mm2 by remember(connection.id) { mutableStateOf(m.overrideCableMm2?.toString().orEmpty()) }
            var temp by remember(connection.id) { mutableStateOf(m.ambientTempC.toString()) }

            EnumDrop("Material", m.conductorMaterial.name, ConductorMaterial.entries.map { it.name }) {
                viewModel.updateConnectionMeta(connection.id, m.copy(conductorMaterial = ConductorMaterial.valueOf(it)))
            }
            EnumDrop("Instalação", m.installationMethod.name, InstallationMethod.entries.map { it.name }) {
                viewModel.updateConnectionMeta(connection.id, m.copy(installationMethod = InstallationMethod.valueOf(it)))
            }
            EnumDrop("Isolação", m.insulation.name, InsulationClass.entries.map { it.name }) {
                viewModel.updateConnectionMeta(connection.id, m.copy(insulation = InsulationClass.valueOf(it)))
            }
            EnumDrop("Agrupamento", m.grouping.name, CircuitGrouping.entries.map { it.name }) {
                viewModel.updateConnectionMeta(connection.id, m.copy(grouping = CircuitGrouping.valueOf(it)))
            }
            EnumDrop("Fases", m.phases.name, SystemPhase.entries.map { it.name }) {
                viewModel.updateConnectionMeta(connection.id, m.copy(phases = SystemPhase.valueOf(it)))
            }

            OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Rótulo (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = length, onValueChange = { length = it }, label = { Text("Comprimento (m)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = temp, onValueChange = { temp = it }, label = { Text("Temperatura (°C)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = mm2, onValueChange = { mm2 = it }, label = { Text("Override mm² (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val meta = m.copy(
                        label = label.takeIf { it.isNotBlank() },
                        lengthMeters = length.toDoubleOrNull(),
                        ambientTempC = temp.toDoubleOrNull() ?: m.ambientTempC,
                        overrideCableMm2 = mm2.toDoubleOrNull()
                    )
                    viewModel.updateConnectionMeta(connection.id, meta)
                }) { Text("Aplicar") }

                OutlinedButton(onClick = { viewModel.updateConnectionMeta(connection.id, ConnectionMeta()) }) { Text("Reset") }
            }
        }
    }
}

private fun applyPreset(vm: EditorViewModel, c: Connection, preset: ConnectionMeta) {
    vm.updateConnectionMeta(c.id, c.meta.copy(
        conductorMaterial = preset.conductorMaterial,
        installationMethod = preset.installationMethod,
        insulation = preset.insulation,
        ambientTempC = preset.ambientTempC,
        grouping = preset.grouping,
        phases = preset.phases
    ))
}

private fun presetConduitExposedCu() = ConnectionMeta(
    conductorMaterial = ConductorMaterial.CU,
    installationMethod = InstallationMethod.CONDUIT_EXPOSED,
    insulation = InsulationClass.V750,
    ambientTempC = 30.0,
    grouping = CircuitGrouping.G1,
    phases = SystemPhase.MONO
)

private fun presetConduitEmbeddedCu() = ConnectionMeta(
    conductorMaterial = ConductorMaterial.CU,
    installationMethod = InstallationMethod.CONDUIT_EMBEDDED,
    insulation = InsulationClass.V750,
    ambientTempC = 30.0,
    grouping = CircuitGrouping.G1,
    phases = SystemPhase.MONO
)

@Composable
private fun EnumDrop(title: String, value: String, options: List<String>, onPick: (String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$title: $value") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onPick(opt); expanded = false })
            }
        }
    }
}
