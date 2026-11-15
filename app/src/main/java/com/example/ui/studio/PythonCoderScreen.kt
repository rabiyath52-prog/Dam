package com.example.ui.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PythonScript
import com.example.ui.theme.EmeraldMuted
import com.example.ui.theme.EmeraldTerminal
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TerminalBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PythonCoderScreen(
    uiState: PythonCoderUiState,
    templates: List<PythonTemplate>,
    savedScriptsState: AsyncListState<List<PythonScript>>,
    onScriptNameChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onApplyTemplate: (PythonTemplate) -> Unit,
    onRunCode: () -> Unit,
    onReviewCode: () -> Unit,
    onSaveScript: () -> Unit,
    onLoadScript: (PythonScript) -> Unit,
    onDeleteScript: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSavedSheet by remember { mutableStateOf(false) }
    var copiedBanner by remember { mutableStateOf(false) }
    val savedCount = (savedScriptsState as? AsyncListState.Success)?.data?.size ?: 0

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp

        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PythonEditorCard(
                        uiState = uiState,
                        templates = templates,
                        savedCount = savedCount,
                        onScriptNameChange = onScriptNameChange,
                        onCodeChange = onCodeChange,
                        onApplyTemplate = onApplyTemplate,
                        onRunCode = onRunCode,
                        onReviewCode = onReviewCode,
                        onSaveScript = onSaveScript,
                        onOpenSaved = { showSavedSheet = true }
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TerminalOutputCard(
                        uiState = uiState,
                        copiedBanner = copiedBanner,
                        onCopyOutput = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Python Output", uiState.terminalOutput))
                            copiedBanner = true
                        }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PythonEditorCard(
                    uiState = uiState,
                    templates = templates,
                    savedCount = savedCount,
                    onScriptNameChange = onScriptNameChange,
                    onCodeChange = onCodeChange,
                    onApplyTemplate = onApplyTemplate,
                    onRunCode = onRunCode,
                    onReviewCode = onReviewCode,
                    onSaveScript = onSaveScript,
                    onOpenSaved = { showSavedSheet = true }
                )
                TerminalOutputCard(
                    uiState = uiState,
                    copiedBanner = copiedBanner,
                    onCopyOutput = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Python Output", uiState.terminalOutput))
                        copiedBanner = true
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showSavedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSavedSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SavedScriptsSheetContent(
                state = savedScriptsState,
                onSelect = { script ->
                    onLoadScript(script)
                    showSavedSheet = false
                },
                onDelete = onDeleteScript
            )
        }
    }
}

@Composable
private fun PythonEditorCard(
    uiState: PythonCoderUiState,
    templates: List<PythonTemplate>,
    savedCount: Int,
    onScriptNameChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onApplyTemplate: (PythonTemplate) -> Unit,
    onRunCode: () -> Unit,
    onReviewCode: () -> Unit,
    onSaveScript: () -> Unit,
    onOpenSaved: () -> Unit
) {
    val isRunning = uiState.runnerState is ActionUiState.Loading || uiState.runnerState is ActionUiState.Streaming
    val isSaving = uiState.saveState is ActionUiState.Loading
    val lineCount = remember(uiState.code) { uiState.code.lines().size }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianOutline, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Python Code Editor",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldPrimary
                    )
                    Text(
                        text = "$lineCount lines • AI-assisted Python 3.12 runner",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onOpenSaved,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_saved_scripts_button")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Saved ($savedCount)")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                templates.forEach { template ->
                    AssistChip(
                        onClick = { onApplyTemplate(template) },
                        label = { Text(template.title) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = ObsidianSurfaceElevated,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            OutlinedTextField(
                value = uiState.scriptName,
                onValueChange = onScriptNameChange,
                label = { Text("Script filename") },
                singleLine = true,
                colors = ekativaInputColors(),
                textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("script_name_input")
            )

            OutlinedTextField(
                value = uiState.code,
                onValueChange = onCodeChange,
                label = { Text("Python 3 source") },
                minLines = 8,
                maxLines = 16,
                colors = ekativaInputColors(),
                textStyle = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("python_code_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRunCode,
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("run_python_button")
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Running…")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.run_python))
                    }
                }

                OutlinedButton(
                    onClick = onReviewCode,
                    enabled = !isRunning,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("review_python_button")
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.review_python))
                }
            }

            FilledTonalButton(
                onClick = onSaveScript,
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("save_script_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Saving…")
                } else {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save_script))
                }
            }

            AnimatedVisibility(visible = uiState.saveState is ActionUiState.Success) {
                val msg = (uiState.saveState as? ActionUiState.Success)?.message.orEmpty()
                StatusBanner(text = msg, isError = false)
            }
            AnimatedVisibility(visible = uiState.saveState is ActionUiState.Error) {
                val msg = (uiState.saveState as? ActionUiState.Error)?.message.orEmpty()
                StatusBanner(text = msg, isError = true)
            }
        }
    }
}

@Composable
private fun TerminalOutputCard(
    uiState: PythonCoderUiState,
    copiedBanner: Boolean,
    onCopyOutput: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalBlack),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EmeraldTerminal.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = EmeraldTerminal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Console Output",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldTerminal
                    )
                    Surface(
                        color = EmeraldMuted,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "AI Runner",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldTerminal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onCopyOutput,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("copy_python_output_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy console output",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = uiState.runnerState is ActionUiState.Error) {
                val err = (uiState.runnerState as? ActionUiState.Error)?.message.orEmpty()
                StatusBanner(text = err, isError = true)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBackground, RoundedCornerShape(12.dp))
                    .border(1.dp, ObsidianOutline, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = uiState.terminalOutput,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = EmeraldTerminal
                )
            }

            if (copiedBanner) {
                Text(
                    text = "Output copied to clipboard.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SavedScriptsSheetContent(
    state: AsyncListState<List<PythonScript>>,
    onSelect: (PythonScript) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Saved Python Scripts",
            style = MaterialTheme.typography.titleLarge,
            color = GoldPrimary
        )

        when (state) {
            AsyncListState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            }

            is AsyncListState.Error -> {
                StatusBanner(text = state.message, isError = true)
            }

            is AsyncListState.Success -> {
                if (state.data.isEmpty()) {
                    Text(
                        text = "No scripts saved yet. Write Python code and tap Save Script.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        items(state.data, key = { it.id }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(item) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.code.lines().firstOrNull().orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDelete(item.id) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete ${item.name}",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
