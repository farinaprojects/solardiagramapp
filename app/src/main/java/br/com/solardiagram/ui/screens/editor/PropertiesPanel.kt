package br.com.solardiagram.ui.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.ElectricalSpecs
import br.com.solardiagram.ui.viewmodel.EditorViewModel
import br.com.solardiagram.ui.screens.editor.specs.AcBusEditor


/**
 * Substitua este arquivo inteiro.
 *
 * O painel agora:
 * - Mantém drafts de Nome + Specs
 * - Não tem mais "Aplicar nome" nem "Aplicar specs" em cada editor
 * - Possui UM único botão "Aplicar" que salva Nome + Specs e fecha o painel
 * - Mantém botão "Excluir componente" com confirmação (opcional, mas já incluído)
 *
 * Requisitos no EditorViewModel:
 * - fun updateComponent(c: Component)
 * - fun deleteComponent(componentId: String)
 * - fun clearSelection()  // limpa selectedComponentId = null (para fechar painel)
 */
@Composable
fun PropertiesPanel(
    component: Component,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxHeight().width(280.dp)) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Propriedades", style = MaterialTheme.typography.titleMedium)



            // Drafts (nome + specs)
            var name by remember(component.id) { mutableStateOf(component.name) }
            var draftSpecs by remember(component.id) { mutableStateOf(component.specs) }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Divider()
            Text("Tipo: ${component.type}", style = MaterialTheme.typography.bodyMedium)

            // Editors: agora NÃO têm botão aplicar, só atualizam o draftSpecs.
            when (val s = draftSpecs) {
                is ElectricalSpecs.PvModuleSpecs ->
                    PvModuleEditor(s) { draftSpecs = it }

                is ElectricalSpecs.MicroInverterSpecs ->
                    MicroInverterEditor(s) { draftSpecs = it }

                is ElectricalSpecs.StringInverterSpecs ->
                    StringInverterEditor(s) { draftSpecs = it }

                is ElectricalSpecs.AcBusSpecs ->
                    AcBusEditor(s) { draftSpecs = it }


                is ElectricalSpecs.QdgSpecs ->
                    QdgEditor(s) { draftSpecs = it }

                is ElectricalSpecs.BreakerSpecs ->
                    BreakerEditor(s) { draftSpecs = it }

                is ElectricalSpecs.DpsSpecs ->
                    DpsEditor(s) { draftSpecs = it }

                is ElectricalSpecs.GroundingSpecs ->
                    GroundEditor(s) { draftSpecs = it }

                is ElectricalSpecs.LoadSpecs ->
                    LoadEditor(s) { draftSpecs = it }


            }

            Divider()

            // Botão único "Aplicar" (salva Nome + Specs) e fecha o painel
            val changed = name != component.name || draftSpecs != component.specs
            Button(
                onClick = {
                    val updated = component.copy(
                        name = name,
                        specs = draftSpecs
                    )
                    viewModel.updateComponent(updated)
                    viewModel.clearSelection() // fecha o painel
                },
                enabled = changed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aplicar")
            }

            Button(
                onClick = { viewModel.clearSelection() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fechar")
            }


            // Excluir componente com confirmação (mantido conforme sua decisão)
            var askDelete by remember(component.id) { mutableStateOf(false) }

            Button(
                onClick = { askDelete = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Excluir componente")
            }

            if (askDelete) {
                AlertDialog(
                    onDismissRequest = { askDelete = false },
                    title = { Text("Confirmar exclusão") },
                    text = {
                        Text(
                            "Deseja excluir este componente?\n" +
                                    "Todas as conexões ligadas a ele também serão removidas."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                askDelete = false
                                viewModel.deleteComponent(component.id)
                                viewModel.clearSelection()
                            }
                        ) { Text("Excluir") }
                    },
                    dismissButton = {
                        TextButton(onClick = { askDelete = false }) { Text("Cancelar") }
                    }
                )
            }

        }
    }
}



/* ===========================
   Editors (SEM botão aplicar)
   =========================== */

// Cole/ substitua TODOS os editors abaixo no mesmo arquivo do PropertiesPanel.
// Padrão: igual ao MicroInverterEditor que você aprovou:
// - mantém texto local (String) para cada campo
// - a cada mudança chama onSpecsChange(s.copy(...)) usando fallback para valor anterior

@Composable
private fun PvModuleEditor(
    s: ElectricalSpecs.PvModuleSpecs,
    onSpecsChange: (ElectricalSpecs.PvModuleSpecs) -> Unit
) {
    Text("Módulo FV", style = MaterialTheme.typography.titleSmall)

    var pMax by remember(s) { mutableStateOf(s.pMaxW.toString()) }
    var voc  by remember(s) { mutableStateOf(s.vOcV.toString()) }
    var isc  by remember(s) { mutableStateOf(s.iScA.toString()) }

    NumField("Pmax (W)", pMax) {
        pMax = it
        onSpecsChange(s.copy(pMaxW = it.toDoubleOrNull() ?: s.pMaxW))
    }
    NumField("Voc (V)", voc) {
        voc = it
        onSpecsChange(s.copy(vOcV = it.toDoubleOrNull() ?: s.vOcV))
    }
    NumField("Isc (A)", isc) {
        isc = it
        onSpecsChange(s.copy(iScA = it.toDoubleOrNull() ?: s.iScA))
    }
}

@Composable
private fun MicroInverterEditor(
    s: ElectricalSpecs.MicroInverterSpecs,
    onSpecsChange: (ElectricalSpecs.MicroInverterSpecs) -> Unit
) {
    Text("Microinversor", style = MaterialTheme.typography.titleSmall)
    Text("Saída AC bifásica: L1 + N + L2", style = MaterialTheme.typography.bodySmall)
    Text("Entradas DC: ${s.dcInputPairs}", style = MaterialTheme.typography.bodySmall)

    var acV by remember(s) { mutableStateOf(s.acVoltageV.toString()) }
    var acP by remember(s) { mutableStateOf(s.acNominalPowerW.toString()) }

    NumField("Tensão AC (V)", acV) {
        acV = it
        onSpecsChange(s.copy(acVoltageV = it.toDoubleOrNull() ?: s.acVoltageV))
    }
    NumField("Potência AC (W)", acP) {
        acP = it
        onSpecsChange(s.copy(acNominalPowerW = it.toDoubleOrNull() ?: s.acNominalPowerW))
    }
}

@Composable
private fun StringInverterEditor(
    s: ElectricalSpecs.StringInverterSpecs,
    onSpecsChange: (ElectricalSpecs.StringInverterSpecs) -> Unit
) {
    Text("Inversor String", style = MaterialTheme.typography.titleSmall)

    var acV by remember(s) { mutableStateOf(s.acVoltageV.toString()) }
    var acP by remember(s) { mutableStateOf(s.acNominalPowerW.toString()) }

    NumField("Tensão AC (V)", acV) {
        acV = it
        onSpecsChange(s.copy(acVoltageV = it.toDoubleOrNull() ?: s.acVoltageV))
    }
    NumField("Potência AC (W)", acP) {
        acP = it
        onSpecsChange(s.copy(acNominalPowerW = it.toDoubleOrNull() ?: s.acNominalPowerW))
    }
}

@Composable
private fun QdgEditor(
    s: ElectricalSpecs.QdgSpecs,
    onSpecsChange: (ElectricalSpecs.QdgSpecs) -> Unit
) {
    Text("QDG", style = MaterialTheme.typography.titleSmall)

    var bus by remember(s) { mutableStateOf(s.maxBusCurrentA.toString()) }

    NumField("Barramento (A)", bus) {
        bus = it
        onSpecsChange(s.copy(maxBusCurrentA = it.toDoubleOrNull() ?: s.maxBusCurrentA))
    }
}

@Composable
private fun BreakerEditor(
    s: ElectricalSpecs.BreakerSpecs,
    onSpecsChange: (ElectricalSpecs.BreakerSpecs) -> Unit
) {
    Text("Disjuntor", style = MaterialTheme.typography.titleSmall)

    var rated by remember(s) { mutableStateOf(s.ratedCurrentA.toString()) }

    NumField("Corrente (A)", rated) {
        rated = it
        onSpecsChange(s.copy(ratedCurrentA = it.toDoubleOrNull() ?: s.ratedCurrentA))
    }
}

@Composable
private fun DpsEditor(
    s: ElectricalSpecs.DpsSpecs,
    onSpecsChange: (ElectricalSpecs.DpsSpecs) -> Unit
) {
    Text("DPS", style = MaterialTheme.typography.titleSmall)

    var maxV by remember(s) { mutableStateOf(s.maxVoltageV.toString()) }

    NumField("Tensão máx (V)", maxV) {
        maxV = it
        onSpecsChange(s.copy(maxVoltageV = it.toDoubleOrNull() ?: s.maxVoltageV))
    }
}

@Composable
private fun GroundEditor(
    s: ElectricalSpecs.GroundingSpecs,
    onSpecsChange: (ElectricalSpecs.GroundingSpecs) -> Unit
) {
    Text("Terra", style = MaterialTheme.typography.titleSmall)

    var isMain by remember(s) { mutableStateOf(s.isMainEarthPoint) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(
            checked = isMain,
            onCheckedChange = {
                isMain = it
                onSpecsChange(s.copy(isMainEarthPoint = it))
            }
        )
        Text("BEP (ponto principal)")
    }
}

@Composable
private fun LoadEditor(
    s: ElectricalSpecs.LoadSpecs,
    onSpecsChange: (ElectricalSpecs.LoadSpecs) -> Unit
) {
    Text("Carga", style = MaterialTheme.typography.titleSmall)

    var power by remember(s) { mutableStateOf(s.powerW.toString()) }

    NumField("Potência (W)", power) {
        power = it
        onSpecsChange(s.copy(powerW = it.toDoubleOrNull() ?: s.powerW))
    }
}

/* ===========================
   Campo numérico (helper)
   =========================== */

@Composable
private fun NumField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}