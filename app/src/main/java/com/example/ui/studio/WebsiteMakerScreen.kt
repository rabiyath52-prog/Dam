package com.example.ui.studio

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.ThemeFinish
import com.example.data.model.WebsiteProject
import com.example.ui.theme.EmeraldTerminal
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PlatinumMetallic
import com.example.ui.theme.SilverMetallic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteMakerScreen(
    uiState: WebsiteMakerUiState,
    savedWebsitesState: AsyncListState<List<WebsiteProject>>,
    onTitleChange: (String) -> Unit,
    onHeadingChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onFinishChange: (ThemeFinish) -> Unit,
    onPhotoHintChange: (String) -> Unit,
    onPhotoSelected: (android.content.Context, android.net.Uri) -> Unit,
    onToggleHtmlSource: () -> Unit,
    onSaveWebsite: () -> Unit,
    onNewDraft: () -> Unit,
    onLoadWebsite: (WebsiteProject) -> Unit,
    onDeleteWebsite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSavedSheet by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onPhotoSelected(context, uri)
            }
        }
    )

    val savedCount = (savedWebsitesState as? AsyncListState.Success)?.data?.size ?: 0

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
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PhotoToWebsiteCard(
                        uiState = uiState,
                        onHintChange = onPhotoHintChange,
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    WebsiteEditorCard(
                        uiState = uiState,
                        savedCount = savedCount,
                        onTitleChange = onTitleChange,
                        onHeadingChange = onHeadingChange,
                        onContentChange = onContentChange,
                        onFinishChange = onFinishChange,
                        onSaveWebsite = onSaveWebsite,
                        onNewDraft = onNewDraft,
                        onOpenSaved = { showSavedSheet = true }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LiveWebsitePreviewCard(
                        uiState = uiState,
                        onToggleHtmlSource = onToggleHtmlSource,
                        onShareHtml = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/html"
                                putExtra(Intent.EXTRA_SUBJECT, uiState.title.ifBlank { "Ekativa Website" })
                                putExtra(Intent.EXTRA_TEXT, uiState.generatedHtml)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Website HTML"))
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PhotoToWebsiteCard(
                    uiState = uiState,
                    onHintChange = onPhotoHintChange,
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                WebsiteEditorCard(
                    uiState = uiState,
                    savedCount = savedCount,
                    onTitleChange = onTitleChange,
                    onHeadingChange = onHeadingChange,
                    onContentChange = onContentChange,
                    onFinishChange = onFinishChange,
                    onSaveWebsite = onSaveWebsite,
                    onNewDraft = onNewDraft,
                    onOpenSaved = { showSavedSheet = true }
                )
                LiveWebsitePreviewCard(
                    uiState = uiState,
                    onToggleHtmlSource = onToggleHtmlSource,
                    onShareHtml = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/html"
                            putExtra(Intent.EXTRA_SUBJECT, uiState.title.ifBlank { "Ekativa Website" })
                            putExtra(Intent.EXTRA_TEXT, uiState.generatedHtml)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Website HTML"))
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
            SavedWebsitesSheetContent(
                state = savedWebsitesState,
                onSelect = { project ->
                    onLoadWebsite(project)
                    showSavedSheet = false
                },
                onDelete = onDeleteWebsite
            )
        }
    }
}

@Composable
private fun PhotoToWebsiteCard(
    uiState: WebsiteMakerUiState,
    onHintChange: (String) -> Unit,
    onPickPhoto: () -> Unit
) {
    val isAnalyzing = uiState.photoAnalysisState is ActionUiState.Loading
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Photo-to-Website",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldPrimary
                    )
                    Text(
                        text = "Upload a product or shop photo—Gemini writes copy and picks a metallic finish.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            uiState.selectedPhotoBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected photo for Gemini website generation",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ObsidianOutline, RoundedCornerShape(12.dp))
                )
            }

            OutlinedTextField(
                value = uiState.photoHint,
                onValueChange = onHintChange,
                label = { Text("Optional shop or brand context") },
                placeholder = { Text("e.g., Traditional gold bridal sets in Kochi") },
                singleLine = true,
                colors = ekativaInputColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("photo_hint_input")
            )

            Button(
                onClick = onPickPhoto,
                enabled = !isAnalyzing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("pick_photo_generate_button")
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.analyzing_photo))
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.generate_from_photo))
                }
            }

            AnimatedVisibility(visible = uiState.photoAnalysisState is ActionUiState.Success) {
                val msg = (uiState.photoAnalysisState as? ActionUiState.Success)?.message.orEmpty()
                StatusBanner(text = msg, isError = false)
            }
            AnimatedVisibility(visible = uiState.photoAnalysisState is ActionUiState.Error) {
                val msg = (uiState.photoAnalysisState as? ActionUiState.Error)?.message.orEmpty()
                StatusBanner(text = msg, isError = true)
            }
        }
    }
}

@Composable
private fun WebsiteEditorCard(
    uiState: WebsiteMakerUiState,
    savedCount: Int,
    onTitleChange: (String) -> Unit,
    onHeadingChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onFinishChange: (ThemeFinish) -> Unit,
    onSaveWebsite: () -> Unit,
    onNewDraft: () -> Unit,
    onOpenSaved: () -> Unit
) {
    val isSaving = uiState.saveState is ActionUiState.Loading
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
                Text(
                    text = if (uiState.editingId == null) "Website Details" else "Editing Saved Site",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onNewDraft,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("new_website_draft_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                    FilledTonalButton(
                        onClick = onOpenSaved,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_saved_websites_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Saved ($savedCount)")
                    }
                }
            }

            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.website_title_label)) },
                placeholder = { Text("ex: Ekativa Gold") },
                singleLine = true,
                colors = ekativaInputColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("website_title_input")
            )

            OutlinedTextField(
                value = uiState.heading,
                onValueChange = onHeadingChange,
                label = { Text(stringResource(R.string.website_heading_label)) },
                placeholder = { Text("ex: Welcome to My Shop") },
                singleLine = true,
                colors = ekativaInputColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("website_heading_input")
            )

            OutlinedTextField(
                value = uiState.content,
                onValueChange = onContentChange,
                label = { Text(stringResource(R.string.website_content_label)) },
                placeholder = { Text("ex: Best Gold Chains in Kerala...") },
                minLines = 3,
                maxLines = 6,
                colors = ekativaInputColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("website_content_input")
            )

            Text(
                text = "Metallic Finish",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeFinish.entries.forEach { finish ->
                    val selected = uiState.themeFinish == finish
                    val swatchColor = when (finish) {
                        ThemeFinish.GOLD -> GoldPrimary
                        ThemeFinish.SILVER -> SilverMetallic
                        ThemeFinish.PLATINUM -> PlatinumMetallic
                    }
                    val containerColor by animateColorAsState(
                        targetValue = if (selected) swatchColor.copy(alpha = 0.22f) else ObsidianSurfaceElevated,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "finishChipBg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (selected) swatchColor else ObsidianOutline,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "finishChipBorder"
                    )

                    Surface(
                        color = containerColor,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onFinishChange(finish) }
                            .testTag("finish_chip_${finish.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = finish.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) swatchColor else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onSaveWebsite,
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("save_website_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Saving to Cloud…")
                } else {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.editingId == null) {
                            stringResource(R.string.save_website)
                        } else {
                            stringResource(R.string.update_website)
                        }
                    )
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
private fun LiveWebsitePreviewCard(
    uiState: WebsiteMakerUiState,
    onToggleHtmlSource: () -> Unit,
    onShareHtml: () -> Unit
) {
    val html = uiState.generatedHtml
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Website Preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldPrimary
                    )
                    Text(
                        text = "${uiState.themeFinish.displayName} theme • Sandboxed HTML render",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onToggleHtmlSource,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_html_source_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Toggle HTML source code",
                            tint = if (uiState.showHtmlSource) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onShareHtml,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_html_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share website HTML",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, ObsidianOutline, RoundedCornerShape(14.dp))
                    .background(ObsidianBackground)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            setBackgroundColor(AndroidColor.TRANSPARENT)
                        }
                    },
                    update = { webView ->
                        if (webView.tag != html) {
                            webView.tag = html
                            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            AnimatedVisibility(visible = uiState.showHtmlSource) {
                Surface(
                    color = ObsidianBackground,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianOutline, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = html,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldTerminal,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedWebsitesSheetContent(
    state: AsyncListState<List<WebsiteProject>>,
    onSelect: (WebsiteProject) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Saved Websites",
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
                        text = "No websites saved yet. Create a draft or analyze a photo, then tap Save Website.",
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
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.themeFinish} • ${item.heading}",
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
                                            contentDescription = "Delete ${item.title}",
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

@Composable
internal fun StatusBanner(
    text: String,
    isError: Boolean
) {
    val bg = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val fg = if (isError) MaterialTheme.colorScheme.onErrorContainer else EmeraldTerminal
    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
internal fun ekativaInputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldPrimary,
    unfocusedBorderColor = ObsidianOutline,
    focusedLabelColor = GoldPrimary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = GoldPrimary,
    focusedContainerColor = ObsidianBackground,
    unfocusedContainerColor = ObsidianBackground
)
