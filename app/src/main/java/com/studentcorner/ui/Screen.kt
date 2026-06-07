package com.studentcorner.ui

sealed class Screen(val route: String) {
    // Auth
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ForgotPassword : Screen("forgot_password")

    // Main
    object Home : Screen("home")
    object Resources : Screen("resources")
    object ResourceDetail : Screen("resource/{resourceId}") {
        fun createRoute(id: String) = "resource/$id"
    }
    object Saved : Screen("saved")
    object AiChat : Screen("ai_chat")
    object Settings : Screen("settings")
    object About : Screen("about")
    object Contact : Screen("contact")
    object Privacy : Screen("privacy")
    object Terms : Screen("terms")
}
