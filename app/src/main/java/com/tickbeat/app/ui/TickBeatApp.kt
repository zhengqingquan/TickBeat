package com.tickbeat.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tickbeat.app.data.BeatmapRepository
import com.tickbeat.app.metronome.MetronomeViewModel
import com.tickbeat.app.ui.eartrain.EarTrainScreen
import com.tickbeat.app.ui.eartrain.EarTrainViewModel
import com.tickbeat.app.ui.follow.FollowScreen
import com.tickbeat.app.ui.follow.FollowViewModel
import com.tickbeat.app.ui.imitate.ImitateScreen
import com.tickbeat.app.ui.imitate.ImitateViewModel
import com.tickbeat.app.ui.settings.LatencySettingsScreen
import com.tickbeat.app.ui.settings.LatencySettingsViewModel
import com.tickbeat.app.ui.sightread.SightReadScreen
import com.tickbeat.app.ui.sightread.SightReadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class DrawerDest(
    val route: String,
    val title: String,
    val icon: ImageVector
)

private object Routes {
    const val Metronome = "metronome"
    const val Follow = "follow"
    const val Imitate = "imitate"
    const val SightRead = "sightread"
    const val EarTrain = "eartrain"
    const val Latency = "latency"
}

private val drawerDestinations = listOf(
    DrawerDest(Routes.Metronome, "节拍器", Icons.Filled.Timer),
    DrawerDest(Routes.Follow, "跟拍", Icons.Filled.TouchApp),
    DrawerDest(Routes.Imitate, "节奏模仿", Icons.Filled.RecordVoiceOver),
    DrawerDest(Routes.SightRead, "视奏节奏", Icons.Filled.MusicNote),
    DrawerDest(Routes.EarTrain, "听辨纠错", Icons.Filled.Hearing),
    DrawerDest(Routes.Latency, "设置延迟补偿", Icons.Filled.Tune)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickBeatApp() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var route by rememberSaveable { mutableStateOf(Routes.Metronome) }
    // Metronome first; remaining pages mount right after first paint so drawer switches are warm.
    var visited by rememberSaveable { mutableStateOf(listOf(Routes.Metronome)) }
    val title = drawerDestinations.find { it.route == route }?.title ?: "TickBeat"
    val allRoutes = drawerDestinations.map { it.route }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            BeatmapRepository.get(context).all()
        }
        // Paint metronome first, then warm one destination per frame to avoid a bulk hitch
        // that makes the first drawer open feel sticky.
        withFrameMillis { }
        for (dest in allRoutes) {
            if (dest !in visited) {
                visited = visited + dest
                withFrameMillis { }
            }
        }
    }

    fun navigateTo(target: String) {
        if (target != route) {
            if (target !in visited) visited = visited + target
            route = target
        }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.background
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "TickBeat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Text(
                    text = "节奏训练",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                drawerDestinations.forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.title) },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        selected = route == dest.route,
                        onClick = { navigateTo(dest.route) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "菜单")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                KeepAlivePage(visible = route == Routes.Metronome, mounted = Routes.Metronome in visited) {
                    val vm: MetronomeViewModel = viewModel()
                    StopWhenHidden(visible = route == Routes.Metronome, stop = vm::stop)
                    MetronomeScreen(viewModel = vm)
                }
                KeepAlivePage(visible = route == Routes.Follow, mounted = Routes.Follow in visited) {
                    val vm: FollowViewModel = viewModel()
                    StopWhenHidden(visible = route == Routes.Follow, stop = vm::stop)
                    FollowScreen(viewModel = vm)
                }
                KeepAlivePage(visible = route == Routes.Imitate, mounted = Routes.Imitate in visited) {
                    val vm: ImitateViewModel = viewModel()
                    StopWhenHidden(visible = route == Routes.Imitate, stop = vm::stop)
                    ImitateScreen(viewModel = vm)
                }
                KeepAlivePage(visible = route == Routes.SightRead, mounted = Routes.SightRead in visited) {
                    val vm: SightReadViewModel = viewModel()
                    StopWhenHidden(visible = route == Routes.SightRead, stop = vm::stop)
                    SightReadScreen(viewModel = vm)
                }
                KeepAlivePage(visible = route == Routes.EarTrain, mounted = Routes.EarTrain in visited) {
                    val vm: EarTrainViewModel = viewModel()
                    StopWhenHidden(visible = route == Routes.EarTrain, stop = vm::stop)
                    EarTrainScreen(viewModel = vm)
                }
                KeepAlivePage(visible = route == Routes.Latency, mounted = Routes.Latency in visited) {
                    val vm: LatencySettingsViewModel = viewModel()
                    StopWhenHidden(visible = route == Routes.Latency, stop = vm::stop)
                    LatencySettingsScreen(viewModel = vm)
                }
            }
        }
    }
}

@Composable
private fun KeepAlivePage(
    visible: Boolean,
    mounted: Boolean,
    content: @Composable () -> Unit
) {
    if (!mounted) return
    // Keep composition for instant switches, but collapse hidden pages so the drawer
    // animation only transforms the active screen (alpha-0 full-size layers were janky,
    // especially with NumberPicker AndroidViews).
    Box(
        modifier = Modifier
            .zIndex(if (visible) 1f else 0f)
            .then(if (visible) Modifier.fillMaxSize() else Modifier.size(0.dp))
    ) {
        content()
    }
}

@Composable
private fun StopWhenHidden(visible: Boolean, stop: () -> Unit) {
    LaunchedEffect(visible) {
        if (!visible) stop()
    }
}
