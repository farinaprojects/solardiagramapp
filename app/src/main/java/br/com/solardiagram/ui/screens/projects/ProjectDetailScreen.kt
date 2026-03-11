package br.com.solardiagram.ui.screens.projects

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.solardiagram.ui.viewmodel.ProjectDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBack: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenProject: (String) -> Unit
) {
    val vm: ProjectDetailViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(projectId) { vm.load(projectId) }

    // ✅ volta somente quando a exclusão for confirmada pelo VM
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            onBack()
            vm.clearDeletedFlag()
        }
    }

    LaunchedEffect(state.duplicatedProjectId) {
        val newId = state.duplicatedProjectId
        if (!newId.isNullOrBlank()) {
            onOpenProject(newId)
            vm.clearDuplicatedFlag()
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }

    // Campos do duplicar
    var dupName by remember { mutableStateOf("") }
    var dupLocation by remember { mutableStateOf("") }

    val p = state.project

    // Preenche valores padrão quando o projeto carregar
    LaunchedEffect(p?.id) {
        if (p != null) {
            dupName = "${p.name} (Cópia)"
            dupLocation = p.location ?: ""
        }
    }

    Scaffold(
       // topBar = {
         //   TopAppBar(
         //       title = { Text(p?.name ?: "Projeto") }
         //   )
        //}

        topBar = {
            TopAppBar(
                title = { Text(p?.name ?: "Projeto") },
                actions = {
                    TextButton(onClick = onBack) {
                        Text("Voltar")
                    }
                }
            )
        }

    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text("Erro: $it") }

            if (p != null) {
                // ===== Detalhes =====
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Detalhes", style = MaterialTheme.typography.titleMedium)

                        // ✅ DATA (atributo do projeto)
                        Text("Data: ${formatDatePtBr(p.createdAtEpochMs)}")

                        Text("Local: ${p.location ?: "-"}")
                        Text("Componentes: ${p.components.size} | Conexões: ${p.connections.size}")
                    }
                }

                // ===== Ações (coluna centralizada) =====
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenEditor,
                        modifier = Modifier.width(260.dp)
                    ) { Text("Abrir") }

                    OutlinedButton(
                        onClick = { showDuplicateDialog = true },
                        modifier = Modifier.width(260.dp)
                    ) { Text("Duplicar") }

                    OutlinedButton(
                        onClick = { vm.validateNow() },
                        modifier = Modifier.width(260.dp)
                    ) { Text("Validar") }

                    OutlinedButton(
                        onClick = { vm.export(projectId) },
                        modifier = Modifier.width(260.dp)
                    ) { Text("Exportar") }

                    Spacer(Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.width(260.dp)
                    ) { Text("Excluir") }
                }

                state.message?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }

                // (opcional) mostrar issues após validar
                val issues = state.validation?.report?.issues ?: emptyList()
                if (issues.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Issues (top 10)", style = MaterialTheme.typography.titleSmall)
                    issues.take(10).forEach {
                        Text(
                            "${it.severity} • ${it.code}: ${it.message}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // ===== Dialog: Excluir com confirmação =====
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Excluir projeto") },
                text = { Text("Tem certeza que deseja excluir este projeto? Esta ação não pode ser desfeita.") },
                confirmButton = {
                    TextButton(
                        enabled = !state.isLoading, // evita múltiplos cliques
                        onClick = {
                            showDeleteDialog = false
                            vm.delete(projectId) // ✅ remove e a navegação acontece no LaunchedEffect(state.deleted)
                        }
                    ) { Text("Excluir") }
                },
                dismissButton = {
                    TextButton(
                        enabled = !state.isLoading,
                        onClick = { showDeleteDialog = false }
                    ) { Text("Cancelar") }
                }
            )
        }

        // ===== Dialog: Duplicar (novo nome + localização) =====
        if (showDuplicateDialog) {
            AlertDialog(
                onDismissRequest = { showDuplicateDialog = false },
                title = { Text("Duplicar projeto") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = dupName,
                            onValueChange = { dupName = it },
                            label = { Text("Novo nome") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = dupLocation,
                            onValueChange = { dupLocation = it },
                            label = { Text("Nova localização") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = dupName.isNotBlank(),
                        onClick = {
                            showDuplicateDialog = false
                            vm.duplicate(
                                sourceProjectId = projectId,
                                newName = dupName.trim(),
                                newLocation = dupLocation.trim().ifBlank { null }
                            )
                        }
                    ) { Text("Duplicar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDuplicateDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

private fun formatDatePtBr(epochMs: Long): String {
    val df = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    return df.format(Date(epochMs))
}