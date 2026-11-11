package com.gotest.app.feature.studypackage.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gotest.app.feature.studypackage.ui.packagelist.PackageListRoute
import com.gotest.app.feature.studypackage.ui.practicedetail.PracticeDetailRoute
import com.gotest.app.feature.studypackage.ui.savedpackages.SavedPackagesRoute
import com.gotest.app.feature.studypackage.ui.studypackage.StudyPackageRoute
import com.gotest.app.feature.studypackage.ui.subjectlist.SubjectListRoute
import com.gotest.app.feature.studypackage.ui.topiclist.TopicListRoute
import com.gotest.app.feature.studypackage.ui.tryoutdetail.TryoutDetailRoute
import com.gotest.app.navigation.ExamDestination
import com.gotest.app.navigation.ExamResultDestination
import com.gotest.app.navigation.PackageListDestination
import com.gotest.app.navigation.PracticeDetailDestination
import com.gotest.app.navigation.SavedPackagesDestination
import com.gotest.app.navigation.SearchDestination
import com.gotest.app.navigation.StudyPackageDestination
import com.gotest.app.navigation.SubjectListDestination
import com.gotest.app.navigation.TopicListDestination
import com.gotest.app.navigation.TryoutDetailDestination

/** Paket tab, Paket Belajar, Subjek, Topik, Detail Paket, Detail Tryout and Paket Tersimpan. */
fun NavGraphBuilder.studyPackageScreens(navController: NavController) {
    val openSearch: () -> Unit = { navController.navigate(SearchDestination) }
    val openSavedPackages: () -> Unit = { navController.navigate(SavedPackagesDestination) }
    val startExam: (String) -> Unit = { packageId -> navController.navigate(ExamDestination(packageId)) }
    val openTryout: (String) -> Unit = { packageId -> navController.navigate(TryoutDetailDestination(packageId)) }

    composable<StudyPackageDestination> {
        StudyPackageRoute(
            onSearch = openSearch,
            onSavedPackages = openSavedPackages,
            onCategoryClick = { categoryId -> navController.navigate(SubjectListDestination(categoryId)) },
            onTryoutClick = openTryout,
            onActivityClick = { attemptId -> navController.navigate(ExamResultDestination(attemptId)) },
        )
    }
    composable<PackageListDestination> {
        PackageListRoute(
            onBack = navController::navigateUp,
            onSearch = openSearch,
            onSavedPackages = openSavedPackages,
            onTryoutClick = openTryout,
            onStartExam = startExam,
        )
    }
    composable<SubjectListDestination> {
        SubjectListRoute(
            onBack = navController::navigateUp,
            onSearch = openSearch,
            onSubjectClick = { subjectId -> navController.navigate(TopicListDestination(subjectId)) },
        )
    }
    composable<TopicListDestination> {
        TopicListRoute(
            onBack = navController::navigateUp,
            onSearch = openSearch,
            onTopicClick = { packageId -> navController.navigate(PracticeDetailDestination(packageId)) },
            onStartExam = startExam,
        )
    }
    composable<PracticeDetailDestination> {
        PracticeDetailRoute(
            onBack = navController::navigateUp,
            onStartExam = startExam,
            onOpenSavedPackages = openSavedPackages,
        )
    }
    composable<TryoutDetailDestination> {
        TryoutDetailRoute(
            onBack = navController::navigateUp,
            onStartExam = startExam,
            onOpenSavedPackages = openSavedPackages,
        )
    }
    composable<SavedPackagesDestination> {
        SavedPackagesRoute(
            onBack = navController::navigateUp,
            onOpenTryout = openTryout,
            onStartExam = startExam,
        )
    }
}
