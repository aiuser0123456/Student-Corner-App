# Student Corner – Android App

A native Kotlin / Jetpack Compose Android app that mirrors your **Student Corner** website, giving users a dedicated mobile experience without needing a browser.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| DI | Hilt |
| Backend | Firebase (Auth + Firestore) |
| Images | Coil |
| Architecture | MVVM + Repository pattern |
| Build | Gradle Kotlin DSL |

---

## Project Structure

```
app/src/main/java/com/studentcorner/
├── MainActivity.kt
├── StudentCornerApp.kt          ← Hilt application class
│
├── data/
│   ├── model/Models.kt          ← Resource, User, ChatMessage, etc.
│   └── repository/
│       └── FirebaseRepository.kt ← All Firestore + Auth calls
│
├── di/
│   └── AppModule.kt             ← Hilt module (Firebase singletons)
│
├── ui/
│   ├── Screen.kt                ← Route definitions
│   ├── AppNavGraph.kt           ← NavHost wiring all screens
│   ├── theme/Theme.kt           ← Brand colours (deep blue / purple)
│   └── screens/
│       ├── LoginScreen.kt
│       ├── SignUpScreen.kt
│       ├── ForgotPasswordScreen.kt
│       ├── HomeScreen.kt
│       ├── ResourcesScreen.kt   ← Search + filter chips + cards
│       ├── ResourceDetailScreen.kt
│       ├── SavedScreen.kt
│       ├── AiChatScreen.kt      ← Gemini-powered chat
│       ├── SettingsScreen.kt
│       └── StaticScreens.kt    ← About, Contact, Privacy, Terms
│
├── viewmodel/
│   ├── AuthViewModel.kt
│   ├── ResourcesViewModel.kt
│   └── AiChatViewModel.kt
│
└── util/Result.kt               ← Success / Error / Loading sealed class
```

---

## Setup Instructions

### 1. Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- An active Firebase project (the same one your website uses)

### 2. Add `google-services.json`
1. Open [Firebase Console](https://console.firebase.google.com) → your project → Project Settings
2. Add an **Android app** with package name `com.studentcorner`
3. Download `google-services.json`
4. Place it at **`app/google-services.json`**

### 3. Configure Gemini API (AI Chat)
1. Get a Gemini API key from [Google AI Studio](https://aistudio.google.com)
2. Open `app/src/main/java/com/studentcorner/viewmodel/AiChatViewModel.kt`
3. Replace `"YOUR_GEMINI_API_KEY"` with your key
4. ⚠️ For production, store the key in `local.properties` and access via `BuildConfig`

### 4. Firestore Collections
The app expects the same Firestore structure as your website:
- **`resources`** – documents with the `Resource` schema
- **`users`** – documents keyed by Firebase UID
- **`chatCommands`** – admin-configured auto-responses

### 5. Build & Run
```bash
# In Android Studio:
# 1. Open the StudentCorner/ folder
# 2. Let Gradle sync
# 3. Run on device or emulator (API 26+)

# Or via CLI:
./gradlew assembleDebug
```

---

## Feature Mapping: Website → App

| Website Page | Android Screen |
|---|---|
| `/` (Home) | `HomeScreen` — hero banner + quick actions |
| `/resources` | `ResourcesScreen` — search + filter chips + cards |
| `/resources/[id]` | `ResourceDetailScreen` — cover image, tags, PDF/download |
| `/saved` | `SavedScreen` — bookmarked resources |
| `/ai-tools` | `AiChatScreen` — Gemini chat with conversation history |
| `/login` | `LoginScreen` |
| `/signup` | `SignUpScreen` |
| `/forgot-password` | `ForgotPasswordScreen` |
| `/settings` | `SettingsScreen` — profile edit + sign out |
| `/about` | `AboutScreen` |
| `/contact` | `ContactScreen` — mailto intent |
| `/privacy` | `PrivacyScreen` |
| `/terms` | `TermsScreen` |

---

## Colour Palette (matches the website)

| Token | Hex | Usage |
|---|---|---|
| Primary (Deep Blue) | `#3F51B5` | Buttons, top bar, icons |
| Accent (Purple) | `#7E57C2` | Secondary elements, gradient |
| Background | `#E8EAF6` | App background |

---

## Production Checklist

- [ ] Replace `"YOUR_GEMINI_API_KEY"` with a real key stored in `local.properties`
- [ ] Add a real `google-services.json`
- [ ] Enable ProGuard rules for Firebase, Hilt, Retrofit
- [ ] Add proper app icons in `res/mipmap-*` directories
- [ ] Add a splash screen resource (`res/drawable/ic_splash.xml`)
- [ ] Set up Firebase App Check for security
- [ ] Configure Firestore security rules (already in `firestore.rules` from your website)
