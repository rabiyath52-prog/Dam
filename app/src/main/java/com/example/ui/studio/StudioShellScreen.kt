package com.example.ui.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.ai.GeminiStudioService
import com.example.data.repository.StudioRepository
import com.example.ui.auth.signOut
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.google.firebase.firestore.FirebaseFirestore

private enum class StudioTab(val labelRes: Int) {
    WEBSITE_MAKER(R.string.tab_website_maker),
    PYTHON_CODER(R.string.tab_python_coder)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioShellScreen(
    currentUserId: String,
    userDisplayName: String?,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: StudioViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudioViewModel(
                    repository = StudioRepository(db),
                    aiService = GeminiStudioService(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val websiteState by viewModel.websiteState.collectAsStateWithLifecycle()
    val pythonState by viewModel.pythonState.collectAsStateWithLifecycle()
    val savedWebsitesState by viewModel.savedWebsites.collectAsStateWithLifecycle()
    val savedScriptsState by viewModel.savedScripts.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(StudioTab.WEBSITE_MAKER) }
    var showAccountMenu by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = ObsidianBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianSurface,
                        titleContentColor = GoldPrimary
                    ),
                    windowInsets = WindowInsets.statusBars,
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.tagline),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        Box {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small,
                                onClick = { showAccountMenu = true },
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("account_menu_button")
                            ) {
                                Text(
                                    text = (userDisplayName?.firstOrNull()
                                        ?: userEmail?.firstOrNull()
                                        ?: 'E').uppercaseChar().toString(),
                                    color = GoldPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showAccountMenu,
                                onDismissRequest = { showAccountMenu = false }
                            ) {
                                if (!userEmail.isNullOrBlank()) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = userDisplayName ?: "Signed in",
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                                Text(
                                                    text = userEmail,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = { showAccountMenu = false }
                                    )
                                }
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = null
                                        )
                                    },
                                    text = { Text(stringResource(R.string.sign_out)) },
                                    onClick = {
                                        showAccountMenu = false
                                        signOut(credentialManager, scope)
                                    }
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = when (tab) {
                                            StudioTab.WEBSITE_MAKER ->
                                                if (selected) Icons.Filled.Language else Icons.Outlined.Language
                                            StudioTab.PYTHON_CODER ->
                                                if (selected) Icons.Filled.Code else Icons.Outlined.Code
                                        },
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = GoldPrimary,
                                    indicatorColor = GoldPrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Spacer(Modifier.size(16.dp))
                        StudioTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = when (tab) {
                                            StudioTab.WEBSITE_MAKER ->
                                                if (selected) Icons.Filled.Language else Icons.Outlined.Language
                                            StudioTab.PYTHON_CODER ->
                                                if (selected) Icons.Filled.Code else Icons.Outlined.Code
                                        },
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = GoldPrimary,
                                    indicatorColor = GoldPrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.name}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        StudioTab.WEBSITE_MAKER -> WebsiteMakerScreen(
                            uiState = websiteState,
                            savedWebsitesState = savedWebsitesState,
                            onTitleChange = viewModel::updateTitle,
                            onHeadingChange = viewModel::updateHeading,
                            onContentChange = viewModel::updateContent,
                            onFinishChange = viewModel::updateThemeFinish,
                            onPhotoHintChange = viewModel::updatePhotoHint,
                            onPhotoSelected = viewModel::analyzePhotoAndGenerateWebsite,
                            onToggleHtmlSource = viewModel::toggleHtmlSource,
                            onSaveWebsite = viewModel::saveCurrentWebsite,
                            onNewDraft = viewModel::startNewWebsiteDraft,
                            onLoadWebsite = viewModel::loadWebsite,
                            onDeleteWebsite = viewModel::deleteWebsite
                        )

                        StudioTab.PYTHON_CODER -> PythonCoderScreen(
                            uiState = pythonState,
                            templates = viewModel.pythonTemplates,
                            savedScriptsState = savedScriptsState,
                            onScriptNameChange = viewModel::updateScriptName,
                            onCodeChange = viewModel::updatePythonCode,
                            onApplyTemplate = viewModel::applyPythonTemplate,
                            onRunCode = viewModel::runPythonCode,
                            onReviewCode = viewModel::reviewPythonCode,
                            onSaveScript = viewModel::saveCurrentScript,
                            onLoadScript = viewModel::loadSavedScript,
                            onDeleteScript = viewModel::deleteScript
                        )
                    }
                }
            }
        }
    }
}
