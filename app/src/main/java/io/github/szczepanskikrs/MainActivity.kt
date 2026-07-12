package io.github.szczepanskikrs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.lifecycle.ViewModelProvider
import io.github.szczepanskikrs.data.HealthTrackerViewModel
import io.github.szczepanskikrs.data.HealthTrackerViewModelFactory
import io.github.szczepanskikrs.ui.screens.ExercisesScreen
import io.github.szczepanskikrs.ui.screens.HeatmapScreen
import io.github.szczepanskikrs.ui.screens.MeasurementsScreen
import io.github.szczepanskikrs.ui.screens.SettingsScreen
import io.github.szczepanskikrs.ui.theme.HealthTrackerTheme
import io.github.szczepanskikrs.utils.NotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this)
        
        // Setup ViewModel
        val factory = HealthTrackerViewModelFactory(this)
        val viewModel = ViewModelProvider(this, factory)[HealthTrackerViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            HealthTrackerTheme(darkTheme = darkTheme) {
                HealthTrackerApp(viewModel)
            }
        }
    }
}

@Composable
fun HealthTrackerApp(viewModel: HealthTrackerViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MEASUREMENTS) }

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val indicatorColor = MaterialTheme.colorScheme.primaryContainer

    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeColor,
            selectedTextColor = activeColor,
            indicatorColor = indicatorColor,
            unselectedIconColor = inactiveColor,
            unselectedTextColor = inactiveColor
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = activeColor,
            selectedTextColor = activeColor,
            indicatorColor = indicatorColor,
            unselectedIconColor = inactiveColor,
            unselectedTextColor = inactiveColor
        )
    )

    NavigationSuiteScaffold(
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surface,
            navigationBarContentColor = MaterialTheme.colorScheme.onSurface,
            navigationRailContainerColor = MaterialTheme.colorScheme.surface,
            navigationRailContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it },
                    colors = itemColors
                )
            }
        }
    ) {
        val screenModifier = Modifier.fillMaxSize()
        when (currentDestination) {
            AppDestinations.MEASUREMENTS -> MeasurementsScreen(viewModel, screenModifier)
            AppDestinations.EXERCISES -> ExercisesScreen(viewModel, screenModifier)
            AppDestinations.HEATMAP -> HeatmapScreen(viewModel, screenModifier)
            AppDestinations.SETTINGS -> SettingsScreen(viewModel, screenModifier)
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    MEASUREMENTS("Pomiary", Icons.Default.Favorite),
    EXERCISES("Treningi", Icons.Default.FitnessCenter),
    HEATMAP("Heatmapa", Icons.Default.CalendarMonth),
    SETTINGS("Ustawienia", Icons.Default.Settings)
}