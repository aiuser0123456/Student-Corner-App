package com.studentcorner.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// ── Resource ──────────────────────────────────────────────────────────────────

data class Resource(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",      // Question Bank | Textbook Solutions | Study Notes | Video Tutorial | AI
    val subject: String = "",       // Physics | Chemistry | Mathematics | Biology | History | Computer Science
    val content: String = "",
    val imageUrl: String = "",
    val imageHint: String = "",
    @get:PropertyName("class") @set:PropertyName("class")
    var classLevel: String = "",    // class11 | class12
    val stream: String = "",        // NEET | JEE | MHT-CET | General
    val pdfUrl: String? = null,
    val downloadUrl: String? = null,
    val isDownloadable: Boolean = false,
)

// ── User ─────────────────────────────────────────────────────────────────────

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val isAdmin: Boolean = false,
    val photoURL: String? = null,
    val savedResources: List<String> = emptyList(),
    val downloadedResources: List<String> = emptyList(),
)

// ── ChatCommand ───────────────────────────────────────────────────────────────

data class ChatCommand(
    @DocumentId
    val id: String = "",
    val trigger: String = "",
    val response: String = "",
    val type: String = "exact",     // exact | contains
    val caseSensitive: Boolean = false,
)

// ── UI-layer wrappers ─────────────────────────────────────────────────────────

enum class Category(val label: String) {
    ALL("All"),
    QUESTION_BANK("Question Bank"),
    TEXTBOOK_SOLUTIONS("Textbook Solutions"),
    STUDY_NOTES("Study Notes"),
    VIDEO_TUTORIAL("Video Tutorial"),
    AI("AI"),
}

enum class Subject(val label: String) {
    ALL("All"),
    PHYSICS("Physics"),
    CHEMISTRY("Chemistry"),
    MATHEMATICS("Mathematics"),
    BIOLOGY("Biology"),
    HISTORY("History"),
    COMPUTER_SCIENCE("Computer Science"),
}

enum class Stream(val label: String) {
    ALL("All"),
    NEET("NEET"),
    JEE("JEE"),
    MHT_CET("MHT-CET"),
    GENERAL("General"),
}

enum class ClassLevel(val label: String, val key: String) {
    ALL("All", ""),
    CLASS_11("Class 11", "class11"),
    CLASS_12("Class 12", "class12"),
}

data class ResourceFilter(
    val query: String = "",
    val category: Category = Category.ALL,
    val subject: Subject = Subject.ALL,
    val stream: Stream = Stream.ALL,
    val classLevel: ClassLevel = ClassLevel.ALL,
)

// ── Chat ──────────────────────────────────────────────────────────────────────

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class MessageRole { USER, MODEL }

data class Conversation(
    val id: String,
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)
