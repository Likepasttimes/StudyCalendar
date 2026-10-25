package com.learningcalendar.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.learningcalendar.app.data.StudyTaskRepository
import com.learningcalendar.app.ui.acceptance.AcceptanceScreen
import com.learningcalendar.app.ui.acceptance.AcceptanceViewModel
import com.learningcalendar.app.ui.acceptance.AcceptanceViewModelFactory
import com.learningcalendar.app.ui.day.DayTasksScreen
import com.learningcalendar.app.ui.day.DayTasksViewModel
import com.learningcalendar.app.ui.day.DayTasksViewModelFactory
import com.learningcalendar.app.ui.detail.TaskDetailScreen
import com.learningcalendar.app.ui.detail.TaskDetailViewModel
import com.learningcalendar.app.ui.detail.TaskDetailViewModelFactory
import java.time.LocalDate

private const val CALENDAR_ROUTE = "calendar"
private const val DAY_ROUTE = "day/{date}"
private const val TASK_ROUTE = "task/{taskId}"
private const val ACCEPTANCE_ROUTE = "acceptance/{cardId}"

@Composable
fun LearningCalendarApp(repository: StudyTaskRepository) {
    val navController = rememberNavController()
    val calendarFactory = CalendarViewModelFactory(repository)

    NavHost(
        navController = navController,
        startDestination = CALENDAR_ROUTE,
    ) {
        composable(CALENDAR_ROUTE) {
            val calendarViewModel: CalendarViewModel = viewModel(factory = calendarFactory)
            val state by calendarViewModel.uiState.collectAsStateWithLifecycle()
            val saving by calendarViewModel.saving.collectAsStateWithLifecycle()

            LearningCalendarScreen(
                state = state,
                onDateSelected = calendarViewModel::selectDate,
                onOpenDay = { date ->
                    navController.navigate("day/$date")
                },
                onOpenTask = { taskId ->
                    navController.navigate("task/$taskId")
                },
                isSaving = saving,
                feedback = calendarViewModel.feedback,
                onCreateTodayTask = { title, time, onResult ->
                    calendarViewModel.createTodayTask(title, time, onResult)
                },
                onTaskCompleted = { task, completed, onResult ->
                    calendarViewModel.setTaskCompleted(task, completed, onResult)
                },
                onEnableRepetition = { taskId, onResult ->
                    calendarViewModel.enableRepetition(taskId, onResult)
                },
                onCancelRepetition = { taskId, onResult ->
                    calendarViewModel.cancelRepetition(taskId, onResult)
                },
                onPreviousMonth = calendarViewModel::showPreviousMonth,
                onNextMonth = calendarViewModel::showNextMonth,
                onTodayClick = calendarViewModel::showToday,
            )
        }

        composable(
            route = DAY_ROUTE,
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
        ) { entry ->
            val date = entry.arguments?.getString("date")
                ?.let(LocalDate::parse)
                ?: return@composable
            val dayViewModel: DayTasksViewModel = viewModel(
                key = "day-$date",
                factory = DayTasksViewModelFactory(date, repository),
            )
            val state by dayViewModel.uiState.collectAsStateWithLifecycle()
            val saving by dayViewModel.saving.collectAsStateWithLifecycle()

            DayTasksScreen(
                state = state,
                isSaving = saving,
                feedback = dayViewModel.feedback,
                onBack = { navController.popBackStack() },
                onOpenTask = { taskId ->
                    navController.navigate("task/$taskId")
                },
                onCreateTask = { section, title, time, onResult ->
                    dayViewModel.createTask(section, title, time, onResult)
                },
                onUpdateTask = { taskId, title, time, onResult ->
                    dayViewModel.updateTask(taskId, title, time, onResult)
                },
                onTaskCompleted = { task, completed, onResult ->
                    dayViewModel.setCompleted(task, completed, onResult)
                },
                onEnableRepetition = { taskId, onResult ->
                    dayViewModel.enableRepetition(taskId, onResult)
                },
                onCancelRepetition = { taskId, onResult ->
                    dayViewModel.cancelRepetition(taskId, onResult)
                },
                onDeleteTask = { taskId, onResult ->
                    dayViewModel.deleteTask(taskId, onResult)
                },
            )
        }

        composable(
            route = TASK_ROUTE,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId") ?: return@composable
            val detailViewModel: TaskDetailViewModel = viewModel(
                key = "task-$taskId",
                factory = TaskDetailViewModelFactory(taskId, repository),
            )
            val state by detailViewModel.uiState.collectAsStateWithLifecycle()
            val saving by detailViewModel.saving.collectAsStateWithLifecycle()

            TaskDetailScreen(
                state = state,
                isSaving = saving,
                feedback = detailViewModel.feedback,
                onBack = { navController.popBackStack() },
                onOpenAcceptance = { cardId ->
                    navController.navigate("acceptance/$cardId")
                },
                onUpdateTask = { title, time, onResult ->
                    detailViewModel.updateTask(title, time, onResult)
                },
                onTaskCompleted = { task, completed, onResult ->
                    detailViewModel.setCompleted(task, completed, onResult)
                },
                onEnableRepetition = { onResult ->
                    detailViewModel.enableRepetition(onResult)
                },
                onCancelRepetition = { onResult ->
                    detailViewModel.cancelRepetition(onResult)
                },
                onDeleteTask = { onResult ->
                    detailViewModel.deleteTask(onResult)
                },
                onAddCard = { objective, action, criteria, onResult ->
                    detailViewModel.addCard(objective, action, criteria, onResult)
                },
                onUpdateCard = { cardId, objective, action, criteria, onResult ->
                    detailViewModel.updateCard(cardId, objective, action, criteria, onResult)
                },
                onDeleteCard = { cardId, onResult ->
                    detailViewModel.deleteCard(cardId, onResult)
                },
            )
        }

        composable(
            route = ACCEPTANCE_ROUTE,
            arguments = listOf(navArgument("cardId") { type = NavType.StringType }),
        ) { entry ->
            val cardId = entry.arguments?.getString("cardId") ?: return@composable
            val acceptanceViewModel: AcceptanceViewModel = viewModel(
                key = "acceptance-$cardId",
                factory = AcceptanceViewModelFactory(cardId, repository),
            )
            val state by acceptanceViewModel.uiState.collectAsStateWithLifecycle()
            val saving by acceptanceViewModel.saving.collectAsStateWithLifecycle()

            AcceptanceScreen(
                state = state,
                isSaving = saving,
                feedback = acceptanceViewModel.feedback,
                onBack = { navController.popBackStack() },
                onCompletedChange = acceptanceViewModel::setCompleted,
                onMasterySelected = acceptanceViewModel::selectMastery,
                onImportSnapshot = acceptanceViewModel::importSnapshot,
            )
        }
    }
}
