package com.gatheredthoughts.voicenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gatheredthoughts.voicenotes.data.AppDatabase
import com.gatheredthoughts.voicenotes.data.CategorizationRepository
import com.gatheredthoughts.voicenotes.data.NotesRepository
import com.gatheredthoughts.voicenotes.data.QueryRepository
import com.gatheredthoughts.voicenotes.ui.detail.DetailScreen
import com.gatheredthoughts.voicenotes.ui.detail.DetailViewModel
import com.gatheredthoughts.voicenotes.ui.list.ListScreen
import com.gatheredthoughts.voicenotes.ui.list.ListViewModel
import com.gatheredthoughts.voicenotes.ui.navigation.Routes
import com.gatheredthoughts.voicenotes.ui.query.QueryScreen
import com.gatheredthoughts.voicenotes.ui.query.QueryViewModel
import com.gatheredthoughts.voicenotes.ui.record.RecordScreen
import com.gatheredthoughts.voicenotes.ui.record.RecordViewModel
import com.gatheredthoughts.voicenotes.ui.theme.VoiceNotesTheme

class MainActivity : ComponentActivity() {

    private lateinit var notesRepository: NotesRepository
    private lateinit var categorizationRepository: CategorizationRepository
    private lateinit var queryRepository: QueryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getInstance(applicationContext)
        notesRepository = NotesRepository(database.noteDao())
        categorizationRepository = CategorizationRepository()
        queryRepository = QueryRepository()

        enableEdgeToEdge()

        setContent {
            VoiceNotesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    val recordViewModel: RecordViewModel = viewModel(
                        factory = simpleFactory {
                            RecordViewModel(notesRepository, categorizationRepository)
                        }
                    )
                    val listViewModel: ListViewModel = viewModel(
                        factory = simpleFactory {
                            ListViewModel(notesRepository)
                        }
                    )
                    val queryViewModel: QueryViewModel = viewModel(
                        factory = simpleFactory {
                            QueryViewModel(notesRepository, queryRepository)
                        }
                    )

                    NavHost(
                        navController = navController,
                        startDestination = Routes.RECORD
                    ) {
                        composable(Routes.RECORD) {
                            RecordScreen(
                                viewModel = recordViewModel,
                                onNavigateToList = {
                                    navController.navigate(Routes.LIST)
                                },
                                onNoteSaved = {
                                    navController.navigate(Routes.LIST) {
                                        popUpTo(Routes.RECORD) { inclusive = false }
                                    }
                                }
                            )
                        }

                        composable(Routes.LIST) {
                            ListScreen(
                                viewModel = listViewModel,
                                onNavigateToRecord = {
                                    navController.popBackStack()
                                },
                                onNavigateToQuery = {
                                    navController.navigate(Routes.QUERY)
                                },
                                onNavigateToDetail = { noteId ->
                                    navController.navigate(Routes.detail(noteId))
                                }
                            )
                        }

                        composable(Routes.QUERY) {
                            QueryScreen(
                                viewModel = queryViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToDetail = { noteId ->
                                    navController.navigate(Routes.detail(noteId))
                                }
                            )
                        }

                        composable(
                            route = Routes.DETAIL,
                            arguments = listOf(
                                navArgument("noteId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getInt("noteId") ?: return@composable
                            val detailViewModel: DetailViewModel = viewModel(
                                key = "detail-$noteId",
                                factory = simpleFactory {
                                    DetailViewModel(notesRepository)
                                }
                            )
                            DetailScreen(
                                noteId = noteId,
                                viewModel = detailViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNoteDeleted = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

private inline fun <reified T : ViewModel> simpleFactory(
    crossinline create: () -> T
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            return create() as VM
        }
    }
}
