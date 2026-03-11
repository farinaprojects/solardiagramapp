package br.com.solardiagram.ui.screens.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import br.com.solardiagram.ui.viewmodel.ProjectListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectListViewModel,
    onOpen: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("Novo Projeto") }
    var newLocation by remember { mutableStateOf("Foz do Iguaçu/PR") }

    // O carregamento inicial já ocorre no init do ViewModel.
    // Aqui refrescamos somente quando a tela volta a ficar visível novamente.
    val lifecycleOwner = LocalLifecycleOwner.current
    var firstResumeHandled by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, viewModel, firstResumeHandled) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (firstResumeHandled) {
                    viewModel.refresh()
                } else {
                    firstResumeHandled = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projetos") },
                actions = {
                    TextButton(onClick = { showDialog = true }) { Text("Novo") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(12.dp)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            state.error?.let {
                Text("Erro: $it")
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(
                    items = state.projects,
                    key = { it.id }
                ) { p ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(p.id) }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)

                            val subtitle = formatProjectSubtitle(
                                createdAtEpochMs = p.createdAtEpochMs,
                                location = (p.location ?: "-")
                            )
                            Text(subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    Button(onClick = {
                        showDialog = false
                        viewModel.createNewProject(newName, newLocation) { id ->
                            onOpen(id)
                        }
                    }) { Text("Criar") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDialog = false }) { Text("Cancelar") }
                },
                title = { Text("Novo projeto") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Nome") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newLocation,
                            onValueChange = { newLocation = it },
                            label = { Text("Local") },
                            singleLine = true
                        )
                    }
                }
            )
        }
    }
}

private fun formatProjectSubtitle(createdAtEpochMs: Long, location: String): String {
    val df = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    val date = df.format(Date(createdAtEpochMs))
    return "$date - $location"
}
