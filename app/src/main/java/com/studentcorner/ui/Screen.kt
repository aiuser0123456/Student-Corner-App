package com.studentcorner.ui

sealed class Screen(val route: String) {
    object Login           : Screen("login")
    object SignUp          : Screen("signup")
    object ForgotPassword  : Screen("forgot_password")
    object Home            : Screen("home")
    object Resources       : Screen("resources")
    object ResourceDetail  : Screen("resource/{resourceId}") {
        fun createRoute(id: String) = "resource/$id"
    }
    object Saved           : Screen("saved")
    object Downloads       : Screen("downloads")
    object AiChat          : Screen("ai_chat")
    object Settings        : Screen("settings")
    object About           : Screen("about")
    object Contact         : Screen("contact")
    object Privacy         : Screen("privacy")
    object Terms           : Screen("terms")
    object PdfViewer       : Screen("pdf_viewer/{resourceId}/{title}") {
        fun createRoute(resourceId: String, title: String) =
            "pdf_viewer/$resourceId/${android.net.Uri.encode(title)}"
    }
}
