package com.studentcorner.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ── About ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("About") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.School, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Student Corner", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("v1.0.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text(
                "Student Corner is your all-in-one study companion for Class 11 & 12 students preparing for NEET, JEE, and MHT-CET. " +
                "Access curated study materials, question banks, textbook solutions, and an AI-powered assistant — all in one place.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Features", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "📚 Comprehensive resource library",
                        "🔍 Smart search and filtering",
                        "🔖 Save resources for offline-ready access",
                        "🤖 AI study assistant",
                        "📖 NEET, JEE & MHT-CET focused content",
                    ).forEach {
                        Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

// ── Contact ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val contactEmail = "support@studentcorner.app"

    Scaffold(topBar = {
        TopAppBar(title = { Text("Contact Us") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Email, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Get in Touch", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text(
                "Have a question, feedback, or need support? We'd love to hear from you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$contactEmail")
                        putExtra(Intent.EXTRA_SUBJECT, "Student Corner Support")
                    }
                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Email Support")
            }
            Spacer(Modifier.height(8.dp))
            Text(contactEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Privacy ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Privacy Policy") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
        ) {
            Text("Privacy Policy", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text("Last updated: January 2025", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            PrivacySection("Information We Collect",
                "We collect information you provide directly to us, such as when you create an account (username, email address) and interact with resources (saved items, downloaded files).")
            PrivacySection("How We Use Information",
                "We use the information we collect to provide and improve our services, personalise your experience, and send you important notifications about your account.")
            PrivacySection("Data Storage",
                "Your data is stored securely using Firebase (Google). We do not sell your personal information to third parties.")
            PrivacySection("Contact",
                "If you have questions about this Privacy Policy, please contact us at support@studentcorner.app.")
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
    Spacer(Modifier.height(4.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(16.dp))
}

// ── Terms ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Terms of Service") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
        ) {
            Text("Terms of Service", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text("Last updated: January 2025", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            PrivacySection("Acceptance of Terms",
                "By using Student Corner, you agree to these terms. If you do not agree, please do not use our services.")
            PrivacySection("Use of Service",
                "Student Corner is intended for educational purposes. You may not use our services for any unlawful purpose or in any way that could damage or impair the service.")
            PrivacySection("User Content",
                "You are responsible for any content you submit. We reserve the right to remove content that violates our guidelines.")
            PrivacySection("Intellectual Property",
                "All content on Student Corner, including text, graphics, and educational resources, is protected by copyright and other intellectual property laws.")
            PrivacySection("Changes to Terms",
                "We may modify these terms at any time. Continued use of Student Corner after changes constitutes acceptance of the new terms.")
        }
    }
}
