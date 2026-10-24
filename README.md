# Stepwise

> **Core Promise:** *Clear steps. More independence.*

Stepwise is an accessibility-first Android application designed to empower people to complete complicated, stressful tasks by turning dense, confusing instructions into clear, bite-sized, actionable steps.

Built for the **Taskforge Productivity Hackathon**, Stepwise prioritizes plain language, predictable workflows, adjustable typography, single-step focus mode, and cognitive clarity without diagnosing or patronizing users.

---

## 🌟 Core Experience & Features

1. **Instruction-to-Checklist Builder**:
   - Paste or type dense multi-paragraph instructions, official manuals, or bureaucratic filing procedures.
   - 1-tap **"Paste Passport Sample"** button to instantly experience a real-world scenario (Form DS-82).
   - Dual Simplification Engines:
     - **Gemini Fast AI** (`gemini-3.8-flash`) for instant plain-language checklists.
     - **Gemini Deep Thinking AI** (`gemini-4-argon` with `ThinkingLevel.HIGH`) for deep logical decomposition of dense legal and procedural manuals.
     - **Deterministic Local Rules Engine (100% Offline)**: Works reliably with zero network access and no API key required.

2. **Plain-Language Assistance & Ambiguity Detection**:
   - Preserves critical numbers, dates, dollar amounts, warning notes, and proper nouns.
   - Flags ambiguous instructions (conditional rules without criteria, unspecified wait times, missing agency contact info) with a clear caution pill rather than guessing or hallucinating facts.

3. **Step-by-Step Focus Mode**:
   - Calm, distraction-free single-step display highlighting *only* the current active step in large, high-legibility typography.
   - Prominent, high-contrast controls: **Previous**, **Skip**, and **Mark Complete & Continue** (minimum 56dp touch target).
   - **Step Breakdown**: 1-tap action to divide complex steps into simpler micro-actions using low-latency Gemini (`gemini-3.1-flash-lite`).
   - **In-Step Companion**: Built-in chat assistant to ask questions and get plain-language answers directly within the active step.
   - **Accomplishments Review**: Celebratory completion summary of all finished steps when the task is done.

4. **Accessible Reading Settings (Persisted)**:
   - **Adjustable Text Size**: Standard (16sp), Large (19sp), Extra Large (23sp) with dynamic M3 typography scaling.
   - **High-Contrast Mode**: WCAG AAA compliant pure white background with deep black borders and high-ratio cobalt accents.
   - **Comfortable Reading Width**: Constrains max line width (680dp) on foldables/tablets to prevent eye fatigue.
   - **Reduced Motion**: Disables animated step transitions and progress bars for users sensitive to motion.

5. **Saved Workflows & Local Persistence**:
   - On-device Room SQLite database: save, rename, reopen, duplicate, and delete checklists.
   - Non-destructive progress tracking: inspect or edit earlier steps without resetting subsequent completed steps.

---

## 🏛️ System Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        MainActivity.kt                           │
│  - Edge-to-Edge window insets & predictive BackHandler           │
│  - Centralized StepwiseTheme (Standard, High-Contrast, Scaled)   │
│  - TopAccessibilityBar (Instant text size, contrast, & width)    │
└─────────────────────────────────┬────────────────────────────────┘
                                  │
       ┌──────────────────────────┼──────────────────────────┐
       ▼                          ▼                          ▼
┌──────────────┐           ┌──────────────┐           ┌──────────────┐
│  Dashboard   │           │   Builder    │           │  Step Focus  │
│    Screen    │◄─────────►│    Screen    │◄─────────►│    Screen    │
└──────┬───────┘           └──────┬───────┘           └──────┬───────┘
       │                          │                          │
       └──────────────────────────┼──────────────────────────┘
                                  │
                                  ▼
                   ┌──────────────────────────────┐
                   │      StepwiseViewModel       │
                   │  (StateFlow, Preferences)    │
                   └──────────────┬───────────────┘
                                  │
       ┌──────────────────────────┴──────────────────────────┐
       ▼                                                     ▼
┌──────────────────────────────┐              ┌──────────────────────────────┐
│      StepwiseRepository      │              │       Gemini AI Service      │
│  - Local: StepwiseDatabase   │              │  - gemini-3.8-flash (steps)  │
│    (Workflows & Steps DAOs)  │              │  - gemini-4-argon (thinking) │
│  - Local RuleBasedParser     │              │  - gemini-3.1-flash-lite     │
└──────────────────────────────┘              └──────────────────────────────┘
```

---

## 🛠️ Technology Stack

- **Platform**: Android (minSdk 24, targetSdk 36)
- **Language**: Kotlin 2.2.10
- **UI Toolkit**: Jetpack Compose (Material Design 3)
- **Local Persistence**: Android Room Database (v2.7.0) with KSP
- **Networking & AI**: OkHttp 4.10, Moshi Kotlin 1.15, Gemini REST API (`gemini-3.8-flash`, `gemini-4-argon`, `gemini-3.1-flash-lite`)
- **Testing**: Robolectric 4.16.1, JUnit 4, AndroidX Test Core

---

## 🚀 Setup & Build Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat or Gradle 8.x
- JDK 17 or higher
- Android SDK Platform 36

### Building and Running
```bash
# Clone the repository
git clone https://github.com/your-username/stepwise-android.git
cd stepwise-android

# Run unit and Robolectric tests
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug
```

### Configuring Gemini API Key (Optional)
If you wish to use Gemini AI features:
1. In Google AI Studio, add your `GEMINI_API_KEY` in the Secrets panel.
2. In `.env.example`, ensure `GEMINI_API_KEY=MY_GEMINI_API_KEY` is present.
3. *Note*: If no API key is configured, Stepwise automatically runs with the **Deterministic Local Rules Engine** with full offline capabilities!

---

## 📋 Manual Accessibility & QA Test Checklist

| Category | Test Case | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Typography** | Cycle Text Size button (A / A+ / A++) | All text scales up cleanly without clipping or truncated buttons | ✅ Verified |
| **High Contrast** | Toggle High Contrast in Top Bar | Screen switches to high-ratio pure white/black borders with dark cobalt accents | ✅ Verified |
| **Reading Width** | Toggle Reading Width | Container width constrains to 680dp max-width centered on screen | ✅ Verified |
| **Keyboard / Nav** | Hardware Back Button from Focus Mode | Returns to Checklist Review without losing completed step status | ✅ Verified |
| **Step Navigation** | Complete, Skip, Previous buttons | Advances progress bar, increments counters, updates Room database | ✅ Verified |
| **Ambiguity Flag** | Enter sentence with "may vary by season" | Ambiguity card renders with explanatory note without guessing | ✅ Verified |
| **Offline Fallback**| Disable network or remove API key | App parses text cleanly using local rule engine with clear offline badge | ✅ Verified |
| **Data Persistence**| Close app and reopen | All saved workflows, completed steps, and preferences are restored | ✅ Verified |

---

## 📦 Deployment & Export Instructions

1. **AI Studio Streaming Emulator**: Use the in-browser streaming emulator to test live interactions.
2. **Export Project**: From the top settings menu, click **Export to ZIP** or **Push to GitHub**.
3. **Generate Release APK / AAB**: Run `gradle :app:bundleRelease` to build a signed Android App Bundle ready for Google Play.
