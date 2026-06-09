package com.studentcorner.ui

import android.net.Uri
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.studentcorner.ui.screens.*
import com.studentcorner.viewmodel.*

@Composable
fun AppNavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel          = hiltViewModel()
    val resourcesViewModel: ResourcesViewModel = hiltViewModel()
    val aiChatViewModel: AiChatViewModel       = hiltViewModel()
    val settingsViewModel: SettingsViewModel   = hiltViewModel()

    val authState by authViewModel.uiState.collectAsState()
    val startDest = if (authState.isLoggedIn) Screen.Home.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDest) {

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onSignUpSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(viewModel = authViewModel, onBack = { navController.popBackStack() })
        }

        // ── Main ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                authViewModel = authViewModel,
                onNavigateToResources  = { navController.navigate(Screen.Resources.route) },
                onNavigateToAiChat     = { navController.navigate(Screen.AiChat.route) },
                onNavigateToSaved      = { navController.navigate(Screen.Saved.route) },
                onNavigateToDownloads  = { navController.navigate(Screen.Downloads.route) },
                onNavigateToSettings   = { navController.navigate(Screen.Settings.route) },
                onNavigateToAbout      = { navController.navigate(Screen.About.route) },
            )
        }
        composable(Screen.Resources.route) {
            ResourcesScreen(
                viewModel = resourcesViewModel,
                onResourceClick = { id -> navController.navigate(Screen.ResourceDetail.createRoute(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.ResourceDetail.route,
            arguments = listOf(navArgument("resourceId") { type = NavType.StringType })
        ) { back ->
            val resourceId = back.arguments?.getString("resourceId") ?: ""
            ResourceDetailScreen(
                resourceId = resourceId,
                resourcesViewModel = resourcesViewModel,
                onBack = { navController.popBackStack() },
                onOpenPdf = { _, title ->
                    navController.navigate(Screen.PdfViewer.createRoute(resourceId, title))
                },
            )
        }
        composable(Screen.Saved.route) {
            SavedScreen(
                viewModel = resourcesViewModel,
                onResourceClick = { id -> navController.navigate(Screen.ResourceDetail.createRoute(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Downloads.route) {
            DownloadsScreen(
                viewModel = resourcesViewModel,
                onOpenPdf = { file, title ->
                    // find the resourceId from file name
                    val resourceId = file.nameWithoutExtension
                    navController.navigate(Screen.PdfViewer.createRoute(resourceId, title))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(
                navArgument("resourceId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
            )
        ) { back ->
            val resourceId = back.arguments?.getString("resourceId") ?: ""
            val title = Uri.decode(back.arguments?.getString("title") ?: "PDF")
            val file = resourcesViewModel.getLocalPdfFile(resourceId)
            PdfViewerScreen(
                pdfFile = file,
                title = title,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.AiChat.route) {
            AiChatScreen(viewModel = aiChatViewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                authViewModel = authViewModel,
                settingsViewModel = settingsViewModel,
                onSignOut = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
                onBack = { navController.popBackStack() },
                onNavigateToPrivacy  = { navController.navigate(Screen.Privacy.route) },
                onNavigateToTerms    = { navController.navigate(Screen.Terms.route) },
                onNavigateToContact  = { navController.navigate(Screen.Contact.route) },
            )
        }
        composable(Screen.About.route)   { AboutScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Contact.route) { ContactScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Privacy.route) { PrivacyScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Terms.route)   { TermsScreen(onBack = { navController.popBackStack() }) }
    }
}
