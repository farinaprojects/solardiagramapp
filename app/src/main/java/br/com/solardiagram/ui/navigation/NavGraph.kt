package br.com.solardiagram.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.solardiagram.ui.screens.editor.EditorHostScreen
import br.com.solardiagram.ui.screens.projects.ProjectDetailScreen
import br.com.solardiagram.ui.screens.projects.ProjectListScreen
import br.com.solardiagram.ui.viewmodel.ProjectListViewModel

object Routes {
    const val LIST = "projects"
    const val DETAIL = "project/{id}"
    const val EDITOR = "editor/{id}"

    fun detail(id: String) = "project/$id"
    fun editor(id: String) = "editor/$id"
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.LIST, modifier = modifier) {

        composable(Routes.LIST) { backStackEntry ->
            val vm: ProjectListViewModel = viewModel(backStackEntry)

            ProjectListScreen(
                viewModel = vm,
                onOpen = { projectId ->
                    nav.navigate(Routes.detail(projectId))
                }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { bs ->
            val id = bs.arguments?.getString("id") ?: return@composable

            ProjectDetailScreen(
                projectId = id,
                onBack = { nav.popBackStack() },
                onOpenEditor = { nav.navigate(Routes.editor(id)) },
                onOpenProject = { newId ->
                    nav.navigate(Routes.detail(newId))
                }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { bs ->
            val id = bs.arguments?.getString("id") ?: return@composable
            EditorHostScreen(
                projectId = id,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
