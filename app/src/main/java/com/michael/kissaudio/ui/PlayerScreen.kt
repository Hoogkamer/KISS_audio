package com.michael.kissaudio.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.michael.kissaudio.data.AudioChannel
import com.michael.kissaudio.data.ChannelType
import com.michael.kissaudio.data.PodcastEpisode
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

enum class NavigationDestination {
    MUSIC, RADIO, PODCASTS
}

enum class PodcastNavigation {
    DASHBOARD, SHOW_DETAIL, EPISODE_DETAIL
}

data class PlayerUIState(
    val title: String,
    val artist: String? = null,
    val subtitle: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float? = null,
    val description: String? = null,
    val shuffleEnabled: Boolean = false,
    val repeatEnabled: Boolean = false,
    val canSeek: Boolean = true,
    val canChangeSpeed: Boolean = false,
    val artworkUrl: String? = null,
    val isDownloaded: Boolean = false,
    val downloadProgress: Int? = null,
    val isFinished: Boolean = false,
    val onDownload: (() -> Unit)? = null,
    val onMarkPlayed: (() -> Unit)? = null
)

@Composable
fun UnifiedPlayer(
    state: PlayerUIState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkipBack: () -> Unit = {},
    onSkipForward: () -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onSourceClick: () -> Unit = {}
) {
    var showInfo by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        // Source Scored Header
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).clickable { onSourceClick() },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            color = Color.Transparent
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("SOURCE:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(8.dp))
                Text(state.subtitle?.uppercase() ?: "KISS AUDIO", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Metadata Section with Scored Grid
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            color = Color.Transparent
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Text("ARTIST:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(state.artist?.uppercase() ?: "---", style = MaterialTheme.typography.headlineMedium, maxLines = 2)
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Text("TRACK:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(state.title.uppercase(), style = MaterialTheme.typography.headlineMedium, maxLines = 3)
                    }
                    
                    // Artwork Placeholder / Image
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                    Box(modifier = Modifier.weight(0.6f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        if (state.artworkUrl != null) {
                            AsyncImage(model = state.artworkUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Default.GraphicEq, null, modifier = Modifier.size(48.dp).alpha(0.1f))
                        }
                    }
                }
            }
        }

        // Progress Section - High-Precision Tuner Needle
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (state.canSeek) {
                                val progress = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeek((progress * state.durationMs).toLong())
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                val totalWidth = maxWidth
                val progress = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                
                // Inactive track (hair-thin grey)
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                
                // Active track (bolder black/white)
                Box(modifier = Modifier.fillMaxWidth(progress).height(2.dp).background(MaterialTheme.colorScheme.onBackground))
                
                // The Tuner Needle (Tick Mark - Centered)
                Box(
                    modifier = Modifier
                        .offset(x = totalWidth * progress - 0.5.dp)
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.onBackground)
                )
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(state.positionMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                Text(formatDuration(state.durationMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        // Primary Controls (Grid Aligned)
        Surface(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            color = Color.Transparent
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Icon(Icons.Default.SkipPrevious, null, modifier = Modifier.size(32.dp))
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                
                // The Signature Orange Button
                Box(modifier = Modifier.weight(1.5f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(72.dp).clickable { onPlayPause() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                }
                
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                IconButton(onClick = onNext, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(32.dp))
                }
            }
        }

        // Secondary Controls
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.playbackSpeed != null) {
                TextButton(onClick = { 
                    val nextSpeed = when {
                        state.playbackSpeed < 1.0f -> 1.0f
                        state.playbackSpeed < 1.5f -> 1.5f
                        state.playbackSpeed < 2.0f -> 2.0f
                        else -> 0.8f
                    }
                    onSpeedChange(nextSpeed)
                }, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${state.playbackSpeed}X", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
 else {
                Spacer(Modifier.weight(1f))
            }
            
            // Podcast specific utilities
            if (state.onDownload != null) {
                IconButton(onClick = state.onDownload) {
                    Icon(
                        if (state.isDownloaded) Icons.Default.FileDownloadDone else Icons.Default.FileDownload,
                        null,
                        tint = if (state.isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            if (state.onMarkPlayed != null) {
                IconButton(onClick = state.onMarkPlayed) {
                    Icon(
                        if (state.isFinished) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (state.isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (onToggleShuffle != null) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(Icons.Default.Shuffle, null, tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
            }
            if (onToggleRepeat != null) {
                IconButton(onClick = onToggleRepeat) {
                    Icon(Icons.Default.Repeat, null, tint = if (state.repeatEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
            }
            
            if (!state.description.isNullOrBlank()) {
                IconButton(onClick = { showInfo = !showInfo }) {
                    Icon(Icons.Default.Info, null, tint = if (showInfo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
            }
        }

        // Optional Description Field
        AnimatedVisibility(visible = showInfo && !state.description.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(bottom = 16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text("DESCRIPTION:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    HtmlText(state.description ?: "")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = viewModel()
) {
    val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    
    val musicState by viewModel.musicState.collectAsStateWithLifecycle()
    val radioState by viewModel.radioState.collectAsStateWithLifecycle()
    val podcastState by viewModel.podcastState.collectAsStateWithLifecycle()
    
    val activeChannelId by viewModel.activeChannelId.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val currentMediaId by viewModel.currentMediaId.collectAsStateWithLifecycle()

    var showAddStation by remember { mutableStateOf(false) }
    var showBulkImport by remember { mutableStateOf(false) }
    var showBulkImportPodcasts by remember { mutableStateOf(false) }
    var showAddPodcast by remember { mutableStateOf(false) }
    var showAddMusic by remember { mutableStateOf(false) }
    var showRadioSearch by remember { mutableStateOf(false) }
    var showPodcastSearch by remember { mutableStateOf(false) }
    var showFileBrowser by remember { mutableStateOf(false) }
    val pendingMarkPlayed by viewModel.pendingMarkPlayed.collectAsStateWithLifecycle()
    
    var currentDestination: NavigationDestination by remember { 
        mutableStateOf(
            when(appConfig.lastCategory) {
                "RADIO" -> NavigationDestination.RADIO
                "PODCASTS" -> NavigationDestination.PODCASTS
                else -> NavigationDestination.MUSIC
            }
        )
    }

    var podcastNav: PodcastNavigation by remember { 
        mutableStateOf(
            if (appConfig.lastCategory == "PODCASTS" && appConfig.activePodcastEpisodeId != null) 
                PodcastNavigation.EPISODE_DETAIL 
            else PodcastNavigation.DASHBOARD
        ) 
    }
    var cameFromRecent: Boolean by remember { mutableStateOf(false) }
    var selectedPodcastId: Int by remember { mutableIntStateOf(appConfig.activePodcastChannelId ?: -1) }
    
    LaunchedEffect(podcastState.activeEpisode?.id) {
        val initialId = podcastState.activeEpisode?.id ?: return@LaunchedEffect
        snapshotFlow { podcastState.activeEpisode?.isFinished }
            .drop(1)
            .collect { isFinished ->
                if (isFinished == true && podcastNav == PodcastNavigation.EPISODE_DETAIL) {
                    podcastNav = if (cameFromRecent) PodcastNavigation.DASHBOARD else PodcastNavigation.SHOW_DETAIL
                }
            }
    }

    var isPlayerVisible: Boolean by remember { 
        mutableStateOf(
            when (appConfig.lastCategory) {
                "MUSIC" -> appConfig.activeMusicChannelId != null
                "RADIO" -> false 
                "PODCASTS" -> appConfig.activePodcastEpisodeId != null
                else -> false
            }
        ) 
    }

    LaunchedEffect(appConfig.lastCategory) {
        val newDest = when(appConfig.lastCategory) {
            "RADIO" -> NavigationDestination.RADIO
            "PODCASTS" -> NavigationDestination.PODCASTS
            else -> NavigationDestination.MUSIC
        }
        if (newDest != currentDestination) {
            currentDestination = newDest
            if (newDest == NavigationDestination.PODCASTS && appConfig.activePodcastEpisodeId != null) {
                podcastNav = PodcastNavigation.EPISODE_DETAIL
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { folderUri ->
            val context = viewModel.getApplication<android.app.Application>()
            val docFile = DocumentFile.fromTreeUri(context, folderUri)
            viewModel.setFolder(folderUri, docFile?.name ?: "Unknown Folder")
            isPlayerVisible = true
        }
    }

    val importRadioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            val context = viewModel.getApplication<android.app.Application>()
            try {
                val content = context.contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { it.readText() }
                content?.let { viewModel.bulkAddRadioStations(it) }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    val importPodcastLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            val context = viewModel.getApplication<android.app.Application>()
            try {
                val content = context.contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { it.readText() }
                content?.let { viewModel.bulkAddPodcasts(it) }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    BackHandler(enabled = isPlayerVisible || (currentDestination == NavigationDestination.PODCASTS && podcastNav != PodcastNavigation.DASHBOARD)) {
        if (isPlayerVisible) {
            isPlayerVisible = false
        } else if (currentDestination == NavigationDestination.PODCASTS) {
            podcastNav = when (podcastNav) {
                PodcastNavigation.SHOW_DETAIL -> PodcastNavigation.DASHBOARD
                PodcastNavigation.EPISODE_DETAIL -> if (cameFromRecent) PodcastNavigation.DASHBOARD else PodcastNavigation.SHOW_DETAIL
                else -> PodcastNavigation.DASHBOARD
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = (if (isPlayerVisible && currentDestination == NavigationDestination.MUSIC) {
                            musicState.activeChannel?.name ?: "PLAYER"
                        } else {
                            when(currentDestination) {
                                NavigationDestination.MUSIC -> "MUSIC DECKS"
                                NavigationDestination.RADIO -> "RADIO STATIONS"
                                NavigationDestination.PODCASTS -> {
                                    if (podcastNav == PodcastNavigation.EPISODE_DETAIL) {
                                        "NOW PLAYING"
                                    } else {
                                        when(podcastNav) {
                                            PodcastNavigation.DASHBOARD -> "PODCASTS"
                                            PodcastNavigation.SHOW_DETAIL -> allChannels.find { it.id == selectedPodcastId }?.name ?: "SHOW"
                                            PodcastNavigation.EPISODE_DETAIL -> "EPISODE DETAIL"
                                            else -> "PODCASTS"
                                        }
                                    }
                                }
                            }
                        }).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    if (isPlayerVisible && currentDestination == NavigationDestination.MUSIC) {
                        IconButton(onClick = { isPlayerVisible = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else if (currentDestination == NavigationDestination.PODCASTS && podcastNav != PodcastNavigation.DASHBOARD) {
                        IconButton(onClick = { 
                            podcastNav = when(podcastNav) {
                                PodcastNavigation.SHOW_DETAIL -> PodcastNavigation.DASHBOARD
                                PodcastNavigation.EPISODE_DETAIL -> if (cameFromRecent) PodcastNavigation.DASHBOARD else PodcastNavigation.SHOW_DETAIL
                                else -> PodcastNavigation.DASHBOARD
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.launchClock() }) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Open Clock")
                    }
                    if (currentDestination == NavigationDestination.PODCASTS && podcastNav == PodcastNavigation.SHOW_DETAIL) {
                        IconButton(onClick = { viewModel.markAllAsPlayed(selectedPodcastId) }) {
                            Icon(Icons.Default.DoneAll, "Mark all played")
                        }
                        IconButton(onClick = { viewModel.toggleHidePlayed() }) {
                            Icon(if (appConfig.hidePlayedEpisodes) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle hide played")
                        }
                    }
                    if (currentDestination == NavigationDestination.RADIO) {
                        IconButton(onClick = { showRadioSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Radio")
                        }
                    }
                    if (currentDestination == NavigationDestination.PODCASTS && podcastNav == PodcastNavigation.DASHBOARD) {
                        IconButton(onClick = { showPodcastSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Podcasts")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Column {
                val showMiniPlayer = when (currentDestination) {
                    NavigationDestination.MUSIC -> !isPlayerVisible
                    NavigationDestination.RADIO -> true
                    NavigationDestination.PODCASTS -> !isPlayerVisible && podcastNav != PodcastNavigation.EPISODE_DETAIL
                }
                if (showMiniPlayer) {
                    AnimatedVisibility(
                        visible = activeChannelId != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .clickable { 
                                    val channel = allChannels.find { it.id == activeChannelId }
                                    if (channel?.type == ChannelType.FOLDER) {
                                        currentDestination = NavigationDestination.MUSIC
                                        isPlayerVisible = true
                                    } else if (channel?.type == ChannelType.PODCAST) {
                                        currentDestination = NavigationDestination.PODCASTS
                                        podcastNav = PodcastNavigation.EPISODE_DETAIL
                                        isPlayerVisible = false
                                    } else if (channel?.type == ChannelType.RADIO) {
                                        currentDestination = NavigationDestination.RADIO
                                        isPlayerVisible = false
                                    }
                                }
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            val miniPlayerTitle = when(appConfig.lastCategory) {
                                "MUSIC" -> musicState.currentTrackName
                                "RADIO" -> radioState.streamMetadata ?: "RADIO"
                                "PODCASTS" -> podcastState.currentTrackName.ifEmpty { podcastState.activeEpisode?.title ?: "PODCAST" }
                                else -> "KISS AUDIO"
                            }
                            val miniPlayerArtist = when(appConfig.lastCategory) {
                                "MUSIC" -> musicState.currentTrackArtist
                                "RADIO" -> null
                                "PODCASTS" -> podcastState.currentTrackArtist ?: podcastState.activeEpisode?.podcastTitle
                                else -> null
                            }
                            val miniPlayerProgress = when(appConfig.lastCategory) {
                                "MUSIC" -> if (musicState.durationMs > 0) musicState.positionMs.toFloat() / musicState.durationMs else 0f
                                "PODCASTS" -> if (podcastState.durationMs > 0) podcastState.positionMs.toFloat() / podcastState.durationMs else 0f
                                else -> 0f
                            }

                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Status light
                                Box(modifier = Modifier.size(8.dp).background(if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
                                
                                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                                    Text(miniPlayerTitle.uppercase(), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (!miniPlayerArtist.isNullOrBlank()) Text(miniPlayerArtist.uppercase(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (miniPlayerProgress > 0) LinearProgressIndicator(progress = { miniPlayerProgress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(2.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                }
                                IconButton(onClick = { 
                                    if (currentDestination == NavigationDestination.PODCASTS && podcastState.activeEpisode != null && activeChannelId != podcastState.activeEpisode?.channelId) {
                                        viewModel.playPodcastEpisode(podcastState.activeEpisode!!)
                                    } else {
                                        viewModel.playPause()
                                    }
                                }) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) }
                            }
                        }
                    }
                }
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.outline,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentDestination == NavigationDestination.MUSIC, 
                        onClick = { 
                            if (currentDestination != NavigationDestination.MUSIC) {
                                viewModel.stopAllPlayback()
                                currentDestination = NavigationDestination.MUSIC
                                isPlayerVisible = false
                            }
                        }, 
                        icon = { Icon(Icons.Default.LibraryMusic, null) }, 
                        label = { Text("MUSIC", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onBackground,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.outline,
                            unselectedTextColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    NavigationBarItem(
                        selected = currentDestination == NavigationDestination.RADIO, 
                        onClick = { 
                            if (currentDestination != NavigationDestination.RADIO) {
                                viewModel.stopAllPlayback()
                                currentDestination = NavigationDestination.RADIO
                                isPlayerVisible = false
                            }
                        }, 
                        icon = { Icon(Icons.Default.Radio, null) }, 
                        label = { Text("RADIO", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onBackground,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.outline,
                            unselectedTextColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    NavigationBarItem(
                        selected = currentDestination == NavigationDestination.PODCASTS, 
                        onClick = { 
                            if (currentDestination != NavigationDestination.PODCASTS) {
                                viewModel.stopAllPlayback()
                                currentDestination = NavigationDestination.PODCASTS
                                isPlayerVisible = false
                                podcastNav = PodcastNavigation.DASHBOARD
                            }
                        }, 
                        icon = { Icon(Icons.Default.Podcasts, null) }, 
                        label = { Text("PODCASTS", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onBackground,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.outline,
                            unselectedTextColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (currentDestination) {
                NavigationDestination.MUSIC -> {
                    MusicDashboard(
                        channels = allChannels.filter { it.type == ChannelType.FOLDER },
                        isLoading = musicState.isLoading,
                        activeChannelId = activeChannelId,
                        isPlaying = isPlaying,
                        currentPositionMs = musicState.positionMs,
                        currentDurationMs = musicState.durationMs,
                        isPlayerVisible = isPlayerVisible,
                        activeChannel = musicState.activeChannel,
                        audioFiles = musicState.audioFiles,
                        currentTrackName = musicState.currentTrackName,
                        currentTrackArtist = musicState.currentTrackArtist,
                        currentTrackAlbum = musicState.currentTrackAlbum,
                        currentTrackIndex = musicState.currentTrackIndex,
                        shuffleEnabled = musicState.shuffleEnabled,
                        repeatEnabled = musicState.repeatEnabled,
                        onChannelClick = { viewModel.selectChannel(it); isPlayerVisible = true },
                        onDeleteChannel = { viewModel.deleteChannel(it) },
                        onRenameChannel = { id, name -> viewModel.renameChannel(id, name) },
                        onCreateChannel = { showAddMusic = true },
                        onFolderPick = { 
                            val initialUri = musicState.activeChannel?.folderUri?.let { Uri.parse(it) }
                            folderPickerLauncher.launch(initialUri)
                        },
                        onPlayPause = { 
                            val channel = musicState.activeChannel
                            if (channel != null && activeChannelId != channel.id) {
                                viewModel.selectChannel(channel.id, autoPlay = true)
                            } else {
                                viewModel.playPause()
                            }
                        },
                        onNext = { if (activeChannelId == musicState.activeChannel?.id) viewModel.next() },
                        onPrevious = { if (activeChannelId == musicState.activeChannel?.id) viewModel.previous() },
                        onSeek = { if (activeChannelId == musicState.activeChannel?.id) viewModel.seekTo(it) },
                        onPlayFileIndex = { index -> 
                            viewModel.playFileAtIndex(index)
                            showFileBrowser = false
                        },
                        onToggleFileBrowser = { showFileBrowser = !showFileBrowser },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.toggleRepeat() }
                    )
                }
                NavigationDestination.RADIO -> {
                    RadioDashboard(
                        channels = allChannels.filter { it.type == ChannelType.RADIO },
                        activeChannelId = activeChannelId,
                        isPlaying = isPlaying,
                        streamMetadata = radioState.streamMetadata,
                        onChannelClick = { if (activeChannelId == it && isPlaying) viewModel.stopRadio() else viewModel.selectChannel(it) },
                        onDeleteChannel = { viewModel.deleteChannel(it) },
                        onRenameChannel = { id, name -> viewModel.renameChannel(id, name) },
                        onUrlUpdate = { id, url -> viewModel.setRadioUrl(id, url) },
                        onCreateChannel = { showAddStation = true },
                        onBulkImport = { showBulkImport = true },
                        onPlayPause = { viewModel.playPause() },
                        onStop = { viewModel.stopRadio() }
                    )
                }
                NavigationDestination.PODCASTS -> {
                    when (podcastNav) {
                            PodcastNavigation.DASHBOARD -> {
                                PodcastDashboard(
                                    channels = allChannels.filter { it.type == ChannelType.PODCAST },
                                    recentEpisodes = podcastState.recentEpisodes,
                                    activeView = podcastState.currentView,
                                    isRefreshing = podcastState.isRefreshing,
                                    refreshMessage = podcastState.refreshMessage,
                                    showOnlyInProgress = appConfig.showOnlyInProgressPodcasts,
                                    inProgressChannelIds = podcastState.inProgressChannelIds,
                                    onRefresh = { viewModel.refreshAllPodcasts() },
                                    onToggleInProgress = { viewModel.toggleInProgressFilter() },
                                    onChannelClick = { id -> selectedPodcastId = id; viewModel.selectChannel(id); podcastNav = PodcastNavigation.SHOW_DETAIL },
                                    onEpisodeClick = { viewModel.setActiveEpisode(it); cameFromRecent = true; podcastNav = PodcastNavigation.EPISODE_DETAIL; isPlayerVisible = false },
                                    onPlayEpisode = { viewModel.playPodcastEpisode(it); cameFromRecent = true; podcastNav = PodcastNavigation.EPISODE_DETAIL; isPlayerVisible = false },
                                    onMarkPlayed = { viewModel.markEpisodeAsPlayed(it) },
                                    onSwitchView = { viewModel.setPodcastView(it) },
                                    onCreateChannel = { showAddPodcast = true },
                                    onDeleteChannel = { viewModel.deleteChannel(it) },
                                    onUrlUpdate = { id, url -> viewModel.setRadioUrl(id, url) },
                                    pendingMarkPlayed = pendingMarkPlayed,
                                    onSwipeMarkPlayed = { viewModel.markAsPlayedWithUndo(it) },
                                    onUndoMarkPlayed = { viewModel.undoMarkAsPlayed(it) },
                                    onBulkImport = { showBulkImportPodcasts = true }
                                )
                            }
                            PodcastNavigation.SHOW_DETAIL -> {
                                PodcastShowDetail(
                                    episodes = if (appConfig.hidePlayedEpisodes) podcastState.episodes.filter { !it.isFinished } else podcastState.episodes,
                                    isRefreshing = podcastState.isRefreshing,
                                    refreshMessage = podcastState.refreshMessage,
                                    onRefresh = { viewModel.refreshShow(selectedPodcastId) },
                                    onEpisodeClick = { viewModel.setActiveEpisode(it); cameFromRecent = false; podcastNav = PodcastNavigation.EPISODE_DETAIL; isPlayerVisible = false },
                                    onPlayEpisode = { viewModel.playPodcastEpisode(it); cameFromRecent = false; podcastNav = PodcastNavigation.EPISODE_DETAIL; isPlayerVisible = false },
                                    onDownload = { viewModel.downloadEpisode(it) },
                                    onDelete = { viewModel.deleteEpisodeFile(it) },
                                    onMarkPlayed = { viewModel.markAsPlayedWithUndo(it) },
                                    pendingMarkPlayed = pendingMarkPlayed,
                                    onUndoMarkPlayed = { viewModel.undoMarkAsPlayed(it) }
                                )
                            }
                            PodcastNavigation.EPISODE_DETAIL -> {
                                podcastState.activeEpisode?.let { episode ->
                                    EpisodeDetailScreen(
                                        episode = episode,
                                        isActive = currentMediaId == episode.id.toString() || (isPlaying && appConfig.activePodcastEpisodeId == episode.id),
                                        isPlaying = isPlaying,
                                        playbackSpeed = playbackSpeed,
                                        currentPositionMs = podcastState.positionMs,
                                        durationMs = podcastState.durationMs,
                                        onPlay = { viewModel.playPodcastEpisode(it) },
                                        onPause = { viewModel.playPause() },
                                        onSeek = { viewModel.seekTo(it) },
                                        onSkipBack = { viewModel.skipBack() },
                                        onSkipForward = { viewModel.skipForward() },
                                        onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                                        onDownload = { viewModel.downloadEpisode(it) },
                                        onDelete = { viewModel.deleteEpisodeFile(it) },
                                        onMarkPlayed = { 
                                            viewModel.markEpisodeAsPlayed(it)
                                            podcastNav = if (cameFromRecent) PodcastNavigation.DASHBOARD else PodcastNavigation.SHOW_DETAIL
                                        },
                                        onMarkUnplayed = { viewModel.markEpisodeAsUnplayed(it) },
                                        onSourceClick = { 
                                            podcastNav = if (cameFromRecent) PodcastNavigation.DASHBOARD else PodcastNavigation.SHOW_DETAIL
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
            
        // Overlays
        if (showAddStation) AddChannelScreen(title = "Add Radio Station", nameLabel = "Station Name", urlLabel = "Stream URL (http/https)", onSave = { name, url -> viewModel.createChannel(name, ChannelType.RADIO, url); showAddStation = false }, onDismiss = { showAddStation = false })
        if (showAddPodcast) AddPodcastScreen(onSave = { url -> viewModel.createChannel("", ChannelType.PODCAST, url); showAddPodcast = false }, onDismiss = { showAddPodcast = false })
        if (showAddMusic) AddMusicDeckScreen(onSave = { name -> viewModel.createChannel(name, ChannelType.FOLDER); showAddMusic = false }, onDismiss = { showAddMusic = false })
        if (showRadioSearch) RadioSearchDialog(results = radioState.searchResults, onSearch = { viewModel.searchRadioStations(it) }, onSelect = { result -> viewModel.createChannel(result.name, ChannelType.RADIO, result.url); showRadioSearch = false }, onDismiss = { showRadioSearch = false })
        if (showPodcastSearch) PodcastSearchDialog(results = podcastState.searchResults, onSearch = { viewModel.searchPodcasts(it) }, onSelect = { result -> viewModel.createChannel(result.collectionName, ChannelType.PODCAST, result.feedUrl); showPodcastSearch = false }, onDismiss = { showPodcastSearch = false })
        
        if (showBulkImport) BulkImportDialog(
            title = "Bulk Import Radio Stations",
            onSave = { viewModel.bulkAddRadioStations(it); showBulkImport = false },
            onFilePick = { importRadioLauncher.launch(arrayOf("text/*", "application/*")); showBulkImport = false },
            onDismiss = { showBulkImport = false }
        )

        if (showBulkImportPodcasts) BulkImportDialog(
            title = "Bulk Import Podcasts",
            onSave = { viewModel.bulkAddPodcasts(it); showBulkImportPodcasts = false },
            onFilePick = { importPodcastLauncher.launch(arrayOf("text/*", "application/*")); showBulkImportPodcasts = false },
            onDismiss = { showBulkImportPodcasts = false }
        )


        if (showFileBrowser && musicState.activeChannel != null) {
            FileBrowserDialog(
                files = musicState.audioFiles,
                currentTrackIndex = if (activeChannelId == musicState.activeChannel?.id) musicState.currentTrackIndex else -1,
                onSelect = { index -> 
                    viewModel.playFileAtIndex(index)
                    showFileBrowser = false
                },
                onDismiss = { showFileBrowser = false }
            )
        }
    }

@Composable
fun MusicDashboard(channels: List<AudioChannel>, isLoading: Boolean, activeChannelId: Int?, isPlaying: Boolean, currentPositionMs: Long, currentDurationMs: Long, isPlayerVisible: Boolean, activeChannel: AudioChannel?, audioFiles: List<com.michael.kissaudio.scanner.AudioFile>, currentTrackName: String, currentTrackArtist: String?, currentTrackAlbum: String?, currentTrackIndex: Int, shuffleEnabled: Boolean, repeatEnabled: Boolean, onChannelClick: (Int) -> Unit, onDeleteChannel: (Int) -> Unit, onRenameChannel: (Int, String) -> Unit, onCreateChannel: () -> Unit, onFolderPick: () -> Unit, onPlayFileIndex: (Int) -> Unit, onToggleFileBrowser: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit, onSeek: (Long) -> Unit, onToggleShuffle: () -> Unit, onToggleRepeat: () -> Unit) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("SCANNING FOLDER...", modifier = Modifier.padding(top = 80.dp), style = MaterialTheme.typography.labelSmall)
        }
    } else if (isPlayerVisible && activeChannel != null) {
        val uiState = PlayerUIState(
            title = if (activeChannelId == activeChannel.id) currentTrackName else activeChannel.currentTrackTitle ?: "READY",
            artist = if (activeChannelId == activeChannel.id) currentTrackArtist else activeChannel.currentTrackArtist,
            subtitle = activeChannel.name,
            positionMs = if (activeChannelId == activeChannel.id) currentPositionMs else activeChannel.currentPositionMs,
            durationMs = if (activeChannelId == activeChannel.id) currentDurationMs else activeChannel.currentTrackDurationMs,
            isPlaying = isPlaying && activeChannelId == activeChannel.id,
            shuffleEnabled = shuffleEnabled,
            repeatEnabled = repeatEnabled
        )
        UnifiedPlayer(
            state = uiState,
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onNext = onNext,
            onPrevious = onPrevious,
            onToggleShuffle = onToggleShuffle,
            onToggleRepeat = onToggleRepeat,
            onSourceClick = onToggleFileBrowser
        )
    } else if (channels.isEmpty()) {
        EmptyState("NO MUSIC DECKS YET", Icons.Default.LibraryMusic, onCreateChannel)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(channels) { channel ->
                val isActive = channel.id == activeChannelId
                DeckCard(channel, isActive, isActive && isPlaying, if (isActive) currentPositionMs else channel.currentPositionMs, if (isActive && currentDurationMs > 0) currentDurationMs else channel.currentTrackDurationMs, { onChannelClick(channel.id) }, { onDeleteChannel(channel.id) }, { onRenameChannel(channel.id, it) })
            }
            item { AddButton("ADD NEW MUSIC DECK", onCreateChannel) }
        }
    }
}

@Composable
fun RadioDashboard(channels: List<AudioChannel>, activeChannelId: Int?, isPlaying: Boolean, streamMetadata: String?, onChannelClick: (Int) -> Unit, onDeleteChannel: (Int) -> Unit, onRenameChannel: (Int, String) -> Unit, onUrlUpdate: (Int, String) -> Unit, onCreateChannel: () -> Unit, onBulkImport: () -> Unit, onPlayPause: () -> Unit, onStop: () -> Unit) {
    var showPlayer by remember { mutableStateOf(false) }
    val activeChannel = channels.find { it.id == activeChannelId }

    if (showPlayer && activeChannel != null) {
        val uiState = PlayerUIState(
            title = streamMetadata ?: "LIVE STREAM",
            artist = "RADIO STATION",
            subtitle = activeChannel.name,
            isPlaying = isPlaying,
            canSeek = false
        )
        UnifiedPlayer(
            state = uiState,
            onPlayPause = onPlayPause,
            onSeek = {},
            onNext = {},
            onPrevious = {},
            onSourceClick = { showPlayer = false }
        )
        BackHandler { showPlayer = false }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (channels.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Radio, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Text("NO RADIO STATIONS YET", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(channels) { channel ->
                        val isActive = channel.id == activeChannelId
                        RadioCard(
                            channel = channel,
                            isActive = isActive,
                            isPlaying = isActive && isPlaying,
                            status = if (isActive && isPlaying) streamMetadata ?: "STREAMING..." else "LIVE STATION",
                            onClick = { 
                                if (isActive) {
                                    showPlayer = true
                                } else {
                                    onChannelClick(channel.id)
                                    showPlayer = true
                                }
                            },
                            onDelete = { onDeleteChannel(channel.id) },
                            onRename = { newName -> onRenameChannel(channel.id, newName) },
                            onUrlUpdate = { newUrl -> onUrlUpdate(channel.id, newUrl) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AddButton("ADD NEW", onCreateChannel, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onBulkImport, modifier = Modifier.weight(1f).height(80.dp), shape = RoundedCornerShape(2.dp)) { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudUpload, null)
                        Text("BULK IMPORT", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DeckCard(channel: AudioChannel, isActive: Boolean, isPlaying: Boolean, pos: Long, dur: Long, onClick: () -> Unit, onDelete: () -> Unit, onRename: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(channel.name ?: "") }

    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val backgroundColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { showMenu = true }) }
            .padding(vertical = 4.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(2.dp) // Sharp, precise corners
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Functional Indicator (Braun Orange Circle when active)
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                            CircleShape
                        )
                        .border(
                            1.dp, 
                            if (isActive) Color.Transparent else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                )
                
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(
                        text = channel.name.uppercase(), 
                        style = MaterialTheme.typography.titleLarge,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = channel.folderDisplayName?.uppercase() ?: "NO SOURCE", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                if (isPlaying) {
                    Icon(
                        Icons.Default.GraphicEq, 
                        null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = channel.currentTrackTitle ?: "---", 
                style = MaterialTheme.typography.bodyLarge, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            )
            
            if (!channel.currentTrackArtist.isNullOrBlank()) {
                Text(
                    text = channel.currentTrackArtist.uppercase(), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Precision Progress Bar
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) {
                val progress = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp), 
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(pos), style = MaterialTheme.typography.bodySmall)
                Text(formatDuration(dur), style = MaterialTheme.typography.bodySmall)
            }
        }
        
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("RENAME") }, onClick = { showRenameDialog = true; showMenu = false })
            DropdownMenuItem(text = { Text("DELETE", color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); showMenu = false })
        }
    }
    
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false }, 
            title = { Text("RENAME DECK") }, 
            text = { OutlinedTextField(value = renameInput, onValueChange = { renameInput = it }, singleLine = true) }, 
            confirmButton = { TextButton(onClick = { onRename(renameInput); showRenameDialog = false }) { Text("SAVE") } }
        )
    }
}

@Composable
fun RadioCard(channel: AudioChannel, isActive: Boolean, isPlaying: Boolean, status: String, onClick: () -> Unit, onDelete: () -> Unit, onRename: (String) -> Unit, onUrlUpdate: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(channel.name ?: "") }
    var urlInput by remember { mutableStateOf(channel.streamUrl ?: "") }

    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val backgroundColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { showMenu = true }) }
            .padding(vertical = 4.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Functional Indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                        CircleShape
                    )
                    .border(
                        1.dp, 
                        if (isActive) Color.Transparent else MaterialTheme.colorScheme.outline,
                        CircleShape
                    )
            )

            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    text = channel.name.uppercase(), 
                    style = MaterialTheme.typography.titleLarge,
                    letterSpacing = 1.sp
                )
                Text(
                    text = status.uppercase(), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (isActive && isPlaying) {
                Icon(
                    Icons.Default.GraphicEq, 
                    null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("RENAME") }, onClick = { showRenameDialog = true; showMenu = false })
            DropdownMenuItem(text = { Text("EDIT URL") }, onClick = { showUrlDialog = true; showMenu = false })
            DropdownMenuItem(text = { Text("DELETE", color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); showMenu = false })
        }
    }
    if (showRenameDialog) {
        AlertDialog(onDismissRequest = { showRenameDialog = false }, title = { Text("RENAME STATION") }, text = { OutlinedTextField(value = renameInput, onValueChange = { renameInput = it }, label = { Text("STATION NAME") }, singleLine = true) }, confirmButton = { TextButton(onClick = { onRename(renameInput); showRenameDialog = false }) { Text("SAVE") } })
    }
    if (showUrlDialog) {
        AlertDialog(onDismissRequest = { showUrlDialog = false }, title = { Text("EDIT STREAM URL") }, text = { OutlinedTextField(value = urlInput, onValueChange = { urlInput = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth()) }, confirmButton = { TextButton(onClick = { onUrlUpdate(urlInput); showUrlDialog = false }) { Text("SAVE") } })
    }
}

@Composable
fun PodcastCard(channel: AudioChannel, onClick: () -> Unit, onDelete: () -> Unit, onUrlUpdate: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf(channel.streamUrl ?: "") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { showMenu = true }) }
            .padding(vertical = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = channel.name.uppercase(), 
                style = MaterialTheme.typography.titleLarge,
                letterSpacing = 1.sp
            )
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("EDIT RSS URL") }, onClick = { showUrlDialog = true; showMenu = false })
            DropdownMenuItem(text = { Text("DELETE", color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); showMenu = false })
        }
    }
    if (showUrlDialog) {
        AlertDialog(onDismissRequest = { showUrlDialog = false }, title = { Text("EDIT RSS URL") }, text = { OutlinedTextField(value = urlInput, onValueChange = { urlInput = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth()) }, confirmButton = { TextButton(onClick = { onUrlUpdate(urlInput); showUrlDialog = false }) { Text("SAVE") } })
    }
}

@Composable
fun AddPodcastScreen(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    Surface(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                Text("Subscribe to Podcast", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("RSS Feed URL") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(32.dp))
            Button(onClick = { onSave(url) }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = url.isNotBlank()) { Text("Subscribe") }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Cancel") }
        }
    }
}

@Composable
fun AddChannelScreen(title: String, nameLabel: String, urlLabel: String, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    Surface(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                Text(title, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(nameLabel) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text(urlLabel) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(32.dp))
            Button(onClick = { onSave(name, url) }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = name.isNotBlank() && url.isNotBlank()) { Text("Save") }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Cancel") }
        }
    }
}

@Composable
fun AddMusicDeckScreen(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    Surface(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                Text("New Music Deck", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Deck Name (e.g. Gym Mix)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(32.dp))
            Button(onClick = { onSave(name) }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = name.isNotBlank()) { Text("Create Deck") }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Cancel") }
        }
    }
}

@Composable
fun RadioSearchDialog(results: List<com.michael.kissaudio.ui.RadioStationResult>, onSearch: (String) -> Unit, onSelect: (com.michael.kissaudio.ui.RadioStationResult) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Search Global Radio") },
        text = {
        Column {
            OutlinedTextField(value = query, onValueChange = { query = it; onSearch(it) }, label = { Text("Search...") }, modifier = Modifier.fillMaxWidth(), trailingIcon = { Icon(Icons.Default.Search, null) })
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(results) { result ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(result) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = result.favicon, contentDescription = null, modifier = Modifier.size(32.dp).background(Color.LightGray))
                        Column(modifier = Modifier.padding(start = 12.dp)) { Text(result.name, maxLines = 1); Text(result.country ?: "", style = MaterialTheme.typography.bodySmall) }
                    }
                    HorizontalDivider()
                }
            }
        }
    })
}

@Composable
fun EmptyState(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
            Button(onClick = onClick, modifier = Modifier.padding(top = 16.dp)) { Text("Add New") }
        }
    }
}

@Composable
fun BulkImportDialog(title: String, onSave: (String) -> Unit, onFilePick: () -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Paste stream URLs (one per line or comma-separated) or load a text file.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("http://stream.url/1\nhttp://stream.url/2") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onFilePick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Load from File (Google Drive/Local)")
                }
            }
        },
        confirmButton = { Button(onClick = { if (text.isNotBlank()) onSave(text) }) { Text("Import Pasted") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().height(80.dp)) {
        Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDashboard(
    channels: List<AudioChannel>,
    recentEpisodes: List<com.michael.kissaudio.data.PodcastEpisode>,
    activeView: String,
    isRefreshing: Boolean,
    refreshMessage: String?,
    showOnlyInProgress: Boolean,
    inProgressChannelIds: List<Int>,
    onRefresh: () -> Unit,
    onToggleInProgress: () -> Unit,
    onChannelClick: (Int) -> Unit,
    onEpisodeClick: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onPlayEpisode: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onMarkPlayed: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onSwitchView: (String) -> Unit,
    onCreateChannel: () -> Unit,
    onDeleteChannel: (Int) -> Unit,
    onUrlUpdate: (Int, String) -> Unit,
    pendingMarkPlayed: Set<Int>,
    onSwipeMarkPlayed: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onUndoMarkPlayed: (Int) -> Unit,
    onBulkImport: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = if (activeView == "SHOWS") 0 else 1) {
            Tab(selected = activeView == "SHOWS", onClick = { onSwitchView("SHOWS") }, text = { Text("Shows") })
            Tab(selected = activeView == "RECENT", onClick = { onSwitchView("RECENT") }, text = { Text("Recent") })
        }
        
        if (activeView == "SHOWS") {
            val displayChannels = if (showOnlyInProgress) {
                channels.filter { it.id in inProgressChannelIds }
            } else channels

            Column(modifier = Modifier.fillMaxSize()) {
                FilterChip(
                    selected = showOnlyInProgress,
                    onClick = onToggleInProgress,
                    label = { Text("In Progress") },
                    leadingIcon = { if (showOnlyInProgress) Icon(Icons.Default.Check, null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (displayChannels.isEmpty()) {
                    if (showOnlyInProgress && channels.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No shows in progress", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        EmptyState("No podcasts yet", Icons.Default.Podcasts, onCreateChannel)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(displayChannels) { channel ->
                            PodcastCard(
                                channel = channel,
                                onClick = { onChannelClick(channel.id) },
                                onDelete = { onDeleteChannel(channel.id) },
                                onUrlUpdate = { newUrl -> onUrlUpdate(channel.id, newUrl) }
                            )
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AddButton("Subscribe to RSS", onCreateChannel, modifier = Modifier.weight(1f))
                                OutlinedButton(onClick = onBulkImport, modifier = Modifier.weight(1f).height(80.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CloudUpload, null)
                                        Text("Bulk Import", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    refreshMessage?.let {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                if (recentEpisodes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No recent episodes", color = MaterialTheme.colorScheme.outline)
                            Text("Pull down to refresh", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(recentEpisodes, key = { it.id }) { episode ->
                            if (episode.id in pendingMarkPlayed) {
                                UndoItem(episode.title, onUndo = { onUndoMarkPlayed(episode.id) })
                            } else {
                                EpisodeListItem(
                                    episode = episode,
                                    onClick = { onEpisodeClick(episode) },
                                    onPlay = { onPlayEpisode(episode) },
                                    onDownload = {},
                                    onDelete = {},
                                    onSwipe = { onSwipeMarkPlayed(episode) },
                                    isPending = false,
                                    onUndo = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastShowDetail(
    episodes: List<com.michael.kissaudio.data.PodcastEpisode>,
    isRefreshing: Boolean,
    refreshMessage: String?,
    onRefresh: () -> Unit,
    onEpisodeClick: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onPlayEpisode: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onDownload: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onDelete: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onMarkPlayed: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    pendingMarkPlayed: Set<Int>,
    onUndoMarkPlayed: (Int) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    val filteredEpisodes = remember(searchQuery, episodes) {
        if (searchQuery.isBlank()) episodes
        else episodes.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description?.contains(searchQuery, ignoreCase = true) == true }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text("Search Episodes") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )
        
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                refreshMessage?.let {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredEpisodes, key = { it.id }) { episode ->
                    if (episode.id in pendingMarkPlayed) {
                        UndoItem(episode.title, onUndo = { onUndoMarkPlayed(episode.id) })
                    } else {
                        EpisodeListItem(
                            episode = episode,
                            onClick = { onEpisodeClick(episode) },
                            onPlay = { onPlayEpisode(episode) },
                            onDownload = { onDownload(episode) },
                            onDelete = { onDelete(episode) },
                            onSwipe = { onMarkPlayed(episode) },
                            isPending = false,
                            onUndo = {}
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun UndoItem(title: String, onUndo: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("MARKED AS PLAYED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(title.uppercase(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onUndo) {
                Text("UNDO", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun EpisodeListItem(episode: com.michael.kissaudio.data.PodcastEpisode, onClick: () -> Unit, onPlay: () -> Unit, onDownload: () -> Unit, onDelete: () -> Unit, onSwipe: () -> Unit, isPending: Boolean, onUndo: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                onSwipe()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    ) {
        val alpha = if (episode.isFinished) 0.4f else 1.0f
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            color = MaterialTheme.colorScheme.background // Use solid background to hide swipe-action ghosting
        ) {
            Column {
                Row(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Play button with subtle status
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clickable { onPlay() }) {
                        val progress = if (episode.durationMs > 0) (episode.playbackPositionMs.toFloat() / episode.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        
                        // Circular track status (very thin)
                        CircularProgressIndicator(
                            progress = { progress }, 
                            modifier = Modifier.fillMaxSize(), 
                            strokeWidth = 1.dp, 
                            color = MaterialTheme.colorScheme.primary, 
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                        Icon(
                            if (episode.isFinished) Icons.Default.Check else Icons.Default.PlayArrow, 
                            null, 
                            modifier = Modifier.size(20.dp),
                            tint = if (episode.isFinished) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f).alpha(alpha)) {
                        episode.podcastTitle?.let { title ->
                            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                        }
                        Text(episode.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, fontWeight = FontWeight.Medium)
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            if (episode.isDownloaded) {
                                Icon(Icons.Default.DownloadDone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            val statusText = when {
                                episode.isDownloading -> "DOWNLOADING ${episode.downloadProgress}%"
                                episode.isQueued -> "QUEUED"
                                else -> formatDateToDaysAgo(episode.pubDate).uppercase()
                            }
                            Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    if (!episode.isFinished && episode.durationMs > 0) {
                        Text(
                            text = formatTimeRemaining(episode.durationMs - episode.playbackPositionMs), 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                // Bottom scoring line
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                if (episode.isDownloading || episode.isQueued) {
                    LinearProgressIndicator(
                        progress = { if (episode.isDownloading) episode.downloadProgress / 100f else 0f },
                        modifier = Modifier.fillMaxWidth().height(1.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeDetailScreen(
    episode: com.michael.kissaudio.data.PodcastEpisode,
    isActive: Boolean,
    isPlaying: Boolean,
    playbackSpeed: Float,
    currentPositionMs: Long,
    durationMs: Long,
    onPlay: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onDownload: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onDelete: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onMarkPlayed: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onMarkUnplayed: (com.michael.kissaudio.data.PodcastEpisode) -> Unit,
    onSourceClick: () -> Unit
) {
    val uiState = PlayerUIState(
        title = episode.title,
        artist = episode.podcastTitle ?: "PODCAST",
        subtitle = episode.podcastTitle ?: "PODCAST",
        positionMs = if (isActive) currentPositionMs else episode.playbackPositionMs,
        durationMs = if (isActive && durationMs > 0) durationMs else episode.durationMs,
        isPlaying = isActive && isPlaying,
        playbackSpeed = playbackSpeed,
        description = episode.description,
        canChangeSpeed = true,
        isDownloaded = episode.isDownloaded,
        downloadProgress = if (episode.isDownloading) episode.downloadProgress else null,
        isFinished = episode.isFinished,
        onDownload = { if (!episode.isDownloaded && !episode.isDownloading) onDownload(episode) else if (episode.isDownloaded) onDelete(episode) },
        onMarkPlayed = { if (episode.isFinished) onMarkUnplayed(episode) else onMarkPlayed(episode) }
    )

    UnifiedPlayer(
        state = uiState,
        onPlayPause = { if (isActive && isPlaying) onPause() else onPlay(episode) },
        onSeek = onSeek,
        onNext = onSkipForward,
        onPrevious = onSkipBack,
        onSpeedChange = onSpeedChange,
        onSourceClick = onSourceClick
    )
}

@Composable
fun PodcastListScreen(channels: List<AudioChannel>, onChannelClick: (Int) -> Unit, onCreateChannel: () -> Unit) {
    // Deprecated in favor of PodcastDashboard
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatTimeRemaining(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatDateToDaysAgo(pubDate: Long?): String {
    if (pubDate == null) return "Unknown"
    val diff = System.currentTimeMillis() - pubDate
    val days = diff / (24 * 60 * 60 * 1000)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        else -> "$days days ago"
    }
}

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextColor(context.getColor(android.R.color.tab_indicator_text))
                textSize = 16f
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { it.text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT) }
    )
}

@Composable
fun PodcastSearchDialog(
    results: List<com.michael.kissaudio.podcast.PodcastSearchResult>,
    onSearch: (String) -> Unit,
    onSelect: (com.michael.kissaudio.podcast.PodcastSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Search Podcasts") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; onSearch(it) },
                    label = { Text("Search...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(results) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(result) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = result.artworkUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.LightGray)
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(result.collectionName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                                Text(result.artistName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    )
}

@Composable
fun FileBrowserDialog(
    files: List<com.michael.kissaudio.scanner.AudioFile>,
    currentTrackIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Track") },
        text = {
            if (files.isEmpty()) {
                Text("No tracks found in this folder.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    itemsIndexed(files) { index, file ->
                        val isSelected = index == currentTrackIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(index) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isSelected) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.title ?: file.displayName.removeSuffix(".mp3"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!file.artist.isNullOrBlank()) {
                                    Text(
                                        text = file.artist!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (index < files.size - 1) {
                            HorizontalDivider(modifier = Modifier.alpha(0.5f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
