package br.com.solardiagram.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.solardiagram.ui.viewmodel.CanvasHighlights
import br.com.solardiagram.ui.viewmodel.EditorHostViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorHostScreen(
    projectId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hostVm: EditorHostViewModel = viewModel()
    val hostState by hostVm.state.collectAsState()

    LaunchedEffect(projectId) {
        hostVm.load(projectId)
    }

    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(hostState.lastMessage) {
        hostState.lastMessage?.let {
            snackbar.showSnackbar(it)
            hostVm.clearMessage()
        }
    }

    LaunchedEffect(hostState.error) {
        hostState.error?.let {
            snackbar.showSnackbar(it)
            hostVm.clearError()
        }
    }

    val issuesOn = hostState.showIssues
    val issueSymbol = if (issuesOn) "⚠" else "○"

    val issueContainerColor =
        if (issuesOn) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

    val issueContentColor =
        if (issuesOn) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    val saveStatusText = remember(
        hostState.isSaving,
        hostState.hasUnsavedChanges,
        hostState.lastSavedAtEpochMs
    ) {
        when {
            hostState.isSaving -> "Salvando..."
            hostState.hasUnsavedChanges -> "Alterações pendentes"
            hostState.lastSavedAtEpochMs != null -> {
                val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                "Salvo às ${formatter.format(Date(hostState.lastSavedAtEpochMs!!))}"
            }
            else -> "Sem alterações"
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                tonalElevation = 1.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ) {
                                Text(
                                    text = saveStatusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = if (hostState.hasUnsavedChanges || hostState.isSaving) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                tonalElevation = 2.dp,
                                shadowElevation = 0.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CompactTopActionButton(
                                        symbol = "💾",
                                        onClick = { hostVm.save() }
                                    )

                                    CompactTopActionButton(
                                        symbol = "✔",
                                        onClick = { hostVm.validateNow() }
                                    )

                                    CompactTopActionButton(
                                        symbol = "↔",
                                        onClick = { hostVm.autoConnectNow() }
                                    )

                                    CompactTopActionButton(
                                        symbol = if (hostState.autoWireOnAdd) "A+" else "A-",
                                        onClick = { hostVm.toggleAutoWireOnAdd() }
                                    )

                                    CompactTopActionButton(
                                        symbol = "⇢",
                                        onClick = { hostVm.autoLayoutNow() }
                                    )

                                    CompactTopActionButton(
                                        symbol = "MD",
                                        onClick = { hostVm.exportTechnicalReport() }
                                    )

                                    TextButton(
                                        onClick = { hostVm.toggleIssues() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(issueContainerColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = issueSymbol,
                                                color = issueContentColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            tonalElevation = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            CompactTopActionButton(
                                symbol = "←",
                                onClick = onBack
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        val editorVm = hostVm.getEditorVm()
        val editorState = hostState.editorState

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                // linha vazia mantida só para preservar a estrutura de layout do host,
                // sem ocupar praticamente espaço visual
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF3F4F6))
            ) {
                when {
                    hostState.isLoading -> {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }

                    editorVm == null || editorState == null -> {
                        Text(
                            text = "Carregando editor...",
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    else -> {
                        val effectiveHighlights =
                            if (hostState.showIssues) hostState.highlights else CanvasHighlights()

                        val summary = buildList {
                            addAll(hostState.insights?.toSummaryPairs().orEmpty())
                            addAll(
                                hostState.validation?.report?.issues
                                    ?.take(3)
                                    ?.map { "${it.severity} • ${it.code}" to it.message }
                                    .orEmpty()
                            )
                        }

                        EditorScreen(
                            viewModel = editorVm,
                            highlights = effectiveHighlights,
                            validationSummary = summary,
                            autoWireOnAdd = hostState.autoWireOnAdd,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTopActionButton(
    symbol: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Text(
            text = symbol,
            fontWeight = FontWeight.SemiBold
        )
    }
}