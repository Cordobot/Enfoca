package com.cordobot.enfoca

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cordobot.enfoca.data.reminder.Notifications
import com.cordobot.enfoca.presentation.editor.HabitEditorScreen
import com.cordobot.enfoca.presentation.focus.FocusScreen
import com.cordobot.enfoca.presentation.habits.HabitsScreen
import com.cordobot.enfoca.presentation.theme.EnfocaTheme
import dagger.hilt.android.AndroidEntryPoint

private const val HABITS = "habits"
private const val FOCUS = "focus"
private const val EDITOR = "editor?id={id}"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // A tap on the Pomodoro notification opens the timer tab
    private val openFocus = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        openFocus.value = intent.getBooleanExtra(Notifications.EXTRA_OPEN_FOCUS, false)
        setContent {
            EnfocaTheme {
                RequestNotificationPermission()
                EnfocaApp(openFocus.value, onFocusOpened = { openFocus.value = false })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFocus.value = intent.getBooleanExtra(Notifications.EXTRA_OPEN_FOCUS, false)
    }
}

@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

@Composable
private fun EnfocaApp(openFocus: Boolean, onFocusOpened: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    LaunchedEffect(openFocus) {
        if (openFocus) {
            navController.navigateTab(FOCUS)
            onFocusOpened()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (route != EDITOR) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    NavigationBarItem(
                        selected = route == HABITS,
                        onClick = { navController.navigateTab(HABITS) },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                        label = { Text("Hábitos") }
                    )
                    NavigationBarItem(
                        selected = route == FOCUS,
                        onClick = { navController.navigateTab(FOCUS) },
                        icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                        label = { Text("Enfoque") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HABITS,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(HABITS) {
                HabitsScreen(
                    onAddHabit = { navController.navigate("editor") },
                    onEditHabit = { id -> navController.navigate("editor?id=$id") }
                )
            }
            composable(FOCUS) { FocusScreen() }
            composable(
                EDITOR,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L }),
                enterTransition = { slideInVertically { it } + fadeIn() },
                popExitTransition = { slideOutVertically { it } + fadeOut() }
            ) {
                HabitEditorScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateTab(route: String) = navigate(route) {
    popUpTo(HABITS) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
