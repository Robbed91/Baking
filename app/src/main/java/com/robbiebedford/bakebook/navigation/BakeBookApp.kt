package com.robbiebedford.bakebook.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.robbiebedford.bakebook.ui.screens.BackupScreen
import com.robbiebedford.bakebook.ui.screens.BakeModeScreen
import com.robbiebedford.bakebook.ui.screens.BakingToolsScreen
import com.robbiebedford.bakebook.ui.screens.CollectionsScreen
import com.robbiebedford.bakebook.ui.screens.ConverterScreen
import com.robbiebedford.bakebook.ui.screens.HomeDashboardScreen
import com.robbiebedford.bakebook.ui.screens.LinkScreen
import com.robbiebedford.bakebook.ui.screens.MoreScreen
import com.robbiebedford.bakebook.ui.screens.OccasionPlannerScreen
import com.robbiebedford.bakebook.ui.screens.PantryScreen
import com.robbiebedford.bakebook.ui.screens.PhotoLibraryScreen
import com.robbiebedford.bakebook.ui.screens.PhotoViewerScreen
import com.robbiebedford.bakebook.ui.screens.RecipeDetailScreen
import com.robbiebedford.bakebook.ui.screens.RecipeScreen
import com.robbiebedford.bakebook.ui.screens.ShoppingScreen
import com.robbiebedford.bakebook.ui.screens.SubstitutionsScreen
import com.robbiebedford.bakebook.ui.screens.TimerScreen
import com.robbiebedford.bakebook.viewmodels.BakeBookViewModel

private data class Tab(val route: String, val label: String, val icon: String)
private val tabs = listOf(
    Tab("home", "Home", "H"),
    Tab("recipes", "Recipes", "R"),
    Tab("timers", "Timers", "T"),
    Tab("shopping", "Shopping", "S"),
    Tab("more", "More", "M")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BakeBookApp(viewModel: BakeBookViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BakeBook") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface, contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = isTabSelected(route, tab.route),
                        onClick = { navController.navigate(tab.route) { launchSingleTop = true } },
                        icon = { Text(tab.icon) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeDashboardScreen(
                    viewModel = viewModel,
                    onRecipes = { navController.navigate("recipes") },
                    onShopping = { navController.navigate("shopping") },
                    onTimers = { navController.navigate("timers") },
                    onPhotos = { navController.navigate("photos") }
                )
            }
            composable("recipes") { RecipeScreen(viewModel, onOpen = { navController.navigate("recipe/$it") }) }
            composable("links") { LinkScreen(viewModel) }
            composable("pantry") { PantryScreen(viewModel) }
            composable("photos") { PhotoLibraryScreen(viewModel, onOpen = { navController.navigate("photo/$it") }) }
            composable("shopping") { ShoppingScreen(viewModel) }
            composable("timers") { TimerScreen() }
            composable("more") {
                MoreScreen(
                    onLinks = { navController.navigate("links") },
                    onPhotos = { navController.navigate("photos") },
                    onPantry = { navController.navigate("pantry") },
                    onOccasions = { navController.navigate("planner") },
                    onCollections = { navController.navigate("collections") },
                    onSubstitutions = { navController.navigate("substitutions") },
                    onTools = { navController.navigate("tools") },
                    onConverter = { navController.navigate("converter") },
                    onBackup = { navController.navigate("backup") }
                )
            }
            composable("collections") { CollectionsScreen(viewModel) }
            composable("planner") { OccasionPlannerScreen(viewModel) }
            composable("substitutions") { SubstitutionsScreen(viewModel) }
            composable("tools") { BakingToolsScreen() }
            composable("converter") { ConverterScreen() }
            composable("backup") { BackupScreen(viewModel) }
            composable("recipe/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                RecipeDetailScreen(viewModel, recipeId = it.arguments?.getLong("id") ?: 0L, onBakeMode = { recipeId -> navController.navigate("bakeMode/$recipeId") })
            }
            composable("bakeMode/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                BakeModeScreen(viewModel, recipeId = it.arguments?.getLong("id") ?: 0L)
            }
            composable("photo/{index}", arguments = listOf(navArgument("index") { type = NavType.IntType })) {
                PhotoViewerScreen(viewModel, index = it.arguments?.getInt("index") ?: 0)
            }
        }
    }
}

private fun isTabSelected(route: String, tabRoute: String): Boolean {
    if (route.startsWith(tabRoute)) return true
    return tabRoute == "more" && route in setOf("links", "photos", "pantry", "planner", "collections", "substitutions", "tools", "converter", "backup")
}
