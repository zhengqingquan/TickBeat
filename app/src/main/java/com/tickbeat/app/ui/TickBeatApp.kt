package com.tickbeat.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tickbeat.app.metronome.MetronomeViewModel
import com.tickbeat.app.ui.eartrain.EarTrainScreen
import com.tickbeat.app.ui.follow.FollowScreen
import com.tickbeat.app.ui.imitate.ImitateScreen
import com.tickbeat.app.ui.sightread.SightReadScreen
import kotlinx.coroutines.launch

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
}

private val drawerDestinations = listOf(
    DrawerDest(Routes.Metronome, "节拍器", Icons.Filled.Timer),
    DrawerDest(Routes.Follow, "跟拍", Icons.Filled.TouchApp),
    DrawerDest(Routes.Imitate, "节奏模仿", Icons.Filled.RecordVoiceOver),
    DrawerDest(Routes.SightRead, "视奏节奏", Icons.Filled.MusicNote),
    DrawerDest(Routes.EarTrain, "听辨纠错", Icons.Filled.Hearing)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickBeatApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Routes.Metronome
    val title = drawerDestinations.find { it.route == route }?.title ?: "TickBeat"

    fun navigateTo(target: String) {
        scope.launch { drawerState.close() }
        if (target == route) return
        navController.navigate(target) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
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
                        modifier = Modifier.padding(horizontal = 8.dp)
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
            NavHost(
                navController = navController,
                startDestination = Routes.Metronome,
                modifier = Modifier.padding(padding)
            ) {
                composable(Routes.Metronome) {
                    val vm: MetronomeViewModel = viewModel()
                    MetronomeScreen(viewModel = vm)
                }
                composable(Routes.Follow) {
                    FollowScreen()
                }
                composable(Routes.Imitate) {
                    ImitateScreen()
                }
                composable(Routes.SightRead) {
                    SightReadScreen()
                }
                composable(Routes.EarTrain) {
                    EarTrainScreen()
                }
            }
        }
    }
}
