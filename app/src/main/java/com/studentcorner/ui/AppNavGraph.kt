package com.studentcorner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.studentcorner.ui.screens.*
import com.studentcorner.viewmodel.AiChatViewModel
import com.studentcorner.viewmodel.AuthViewModel
import com.studentcorner.viewmodel.ResourcesViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsState()
    val startDest = if (authState.isLoggedIn) Screen.Home.route else Screen.Login.route

    val resourcesViewModel: ResourcesViewModel = hiltViewModel()
    val aiChatViewModel: AiChatViewModel = hiltViewModel()

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
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
            )
        }

        // ── Main ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                authViewModel = authViewModel,
                onNavigateToResources = { navController.navigate(Screen.Resources.route) },
                onNavigateToAiChat = { navController.navigate(Screen.AiChat.route) },
                onNavigateToSaved = { navController.navigate(Screen.Saved.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
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
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: ""
            ResourceDetailScreen(
                resourceId = resourceId,
                resourcesViewModel = resourcesViewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Saved.route) {
            SavedScreen(
                viewModel = resourcesViewModel,
                onResourceClick = { id -> navController.navigate(Screen.ResourceDetail.createRoute(id)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.AiChat.route) {
            AiChatScreen(
                viewModel = aiChatViewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                authViewModel = authViewModel,
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                onNavigateToTerms = { navController.navigate(Screen.Terms.route) },
                onNavigateToContact = { navController.navigate(Screen.Contact.route) },
            )
        }

        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Contact.route) {
            ContactScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Terms.route) {
            TermsScreen(onBack = { navController.popBackStack() })
        }
    }
}
