# Stepwise — Clear Steps. More Independence.

Stepwise transforms long, dense, or confusing instructions into calm, predictable, plain-language actionable checklists. Designed with accessibility at its heart, it empowers users with cognitive clarity, adjustable typography, high contrast, single-step focus mode, offline-first local storage, and intelligent AI breakdown assistance.

***

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and user experience choices were confirmed during Phase 1 clarification:

- **Confirmed Decision 1 (AI Capabilities)**: Integrated Gemini intelligence featuring full plain-language simplification, numbered step generation, ambiguity flagging, and step breakdown assistance using `gemini-3.8-flash` and `gemini-4-argon` (with `ThinkingLevel.HIGH` for deep reasoning on complex multi-clause instructions), along with a deterministic local rule-based fallback when offline or without an API key.
- **Confirmed Decision 2 (Data Persistence)**: Offline-first architecture using Android Room database for instant local saving, editing, step status tracking, and offline reliability, complemented by an optional Firebase Google Sign-In and Firestore cloud synchronization layer.
- **Confirmed Decision 3 (Demo Showcase)**: A built-in real-world demonstration scenario: **"Complicated Government Passport Renewal / Form Filing Instructions"**—demonstrating how confusing legalese and procedural requirements convert into clean, sequential, verified steps.
- **Accessibility & Contrast**: Built-in instant toggles for High-Contrast Mode, Large / Extra Large Text scaling, Reduced Motion, and Single-Step Focus Mode with persistent user preferences.

***

## 1. Overview & Core Concept

- **What It Does**: Users enter raw text instructions, upload guides, or choose the built-in demo. Stepwise breaks the text down into bite-sized, sequential checklist items. Users can review, reorder, edit, and launch into **Step-by-Step Focus Mode**, displaying one active step at a time with Previous, Next, Complete, and Skip controls, plus an expandable summary of overall progress.
- **Target Audience / Persona**: Individuals seeking reduced cognitive friction, people managing executive dysfunction, neurodivergent users, older adults, language learners, or anyone dealing with stressful, high-stakes bureaucratic or technical processes.
- **Key Value**: “Clear steps. More independence.” Eliminates decision paralysis and cognitive overload without diagnosing or patronizing the user.

***

## 2. User Experience & Visual Design

### Key User Flows

1. **Dashboard & Saved Workflows**:
   - Welcome banner with core mission statement and quick actions.
   - Built-in Demo Scenario button: *"Passport Renewal & Form Filing Guide (Sample)"*.
   - List of saved workflows with progress indicators (e.g., "Step 3 of 8 completed"), last opened timestamps, duplicate, and delete actions.
   - Global Accessibility Bar: Instant toggle for font size (Standard / Large / XL), High Contrast (Warm Neutral Cream vs. High-Contrast Deep Navy / Crisp White), and Reduced Motion.
2. **Instruction-to-Checklist Studio**:
   - Clean, uncluttered text area to paste or type raw instructions.
   - Quick "Paste Sample" button for the government filing scenario.
   - Action buttons: "Generate with AI (Gemini)" and "Process Offline (Local Rules)".
   - Ambiguity Detector: Flags uncertain steps (e.g., *"Wait 2-4 weeks or contact agency if status unchanged"*) with a yellow caution pill and a plain-language explanation, without guessing or hallucinating facts.
3. **Checklist Review & Editor**:
   - Reorderable list of extracted steps with step numbering, plain-language description, original source text toggle, and estimated effort/materials required.
   - Add new step, edit step text, delete, or split a step into smaller sub-actions.
   - Primary action: **"Start Step-by-Step Mode"**.
4. **Step-by-Step Focus Mode**:
   - Full-bleed, calm, distraction-free card highlighting ONLY the current step in prominent typography.
   - Clear large buttons: **Previous**, **Skip**, and **Complete Step** (minimum 56dp touch targets with high-contrast active states).
   - Expandable "View All Steps" bottom drawer or side sheet showing overall checklist progress.
   - Dynamic celebration state upon completing all steps, reviewing accomplishments, and offering options to restart or export summary.

### Visual Identity & Theme

- **Aesthetic Direction**: Calm, Scandinavian utilitarianism with warm, soothing ergonomics. Thoughtful breathing room, generous padding, and zero decorative fluff.
- **Color Palette**:
  - *Primary Neutral*: Warm Oatmeal Cream (`#FBF9F5`) in standard mode; Deep Charcoal Navy (`#12161A`) in dark mode.
  - *High-Contrast Mode*: Pure White (`#FFFFFF`) with Jet Black (`#0B0E11`) and high-ratio deep cobalt borders.
  - *Accent & Focus*: Deep Calming Teal (`#0D6E6E`) and Ocean Cyan (`#14919B`), paired with soft Sage Green (`#2E7D32`) for completed actions.
  - *Warning/Ambiguity Pill*: Muted Amber Ochre (`#8D5B00`) on Soft Amber (`#FFF4E5`).
- **Typography & Scale**:
  - Scalable typography with dynamic `sp` units.
  - 3 text-size presets: Default (16sp body), Large (19sp body), and Extra Large (22sp body) with proportional title scaling.
- **Accessibility Invariants**:
  - Minimum 48dp x 48dp touch targets on all interactive elements.
  - WCAG AAA contrast ratio compliance for text and primary controls.
  - Clear semantic labels, `contentDescription` for all icons, and TalkBack live region announcements when moving between steps.

***

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual Generation Engine (Gemini Cloud + Deterministic Local)**:
  - *Approach*: If `GEMINI_API_KEY` is present and network is available, invoke Gemini models (`gemini-3.8-flash` for rapid step generation and `gemini-4-argon` with `ThinkingLevel.HIGH` for deep reasoning on complex procedural manuals). If offline or without an API key, use a robust local regex/heuristic pipeline that parses numbered lists, imperative action verbs, bullet points, and sub-clauses.
  - *Why*: Guarantees the application works 100% offline out-of-the-box for any user, while providing best-in-class AI simplification when connected.
- **Decision 2: Room SQLite as Single Source of Truth with Optional Firestore Sync**:
  - *Approach*: All workflows and step progress persist locally in Room database with state flows. Users can optionally authenticate with Google Sign-In to backup and sync workflows to Firebase Firestore.
  - *Why*: Instant load times, zero latency, full offline capability, zero mandatory login barrier for immediate utility, and seamless multi-device sync for users who want cloud backup.
- **Decision 3: Non-Destructive Step Progress**:
  - *Approach*: Navigating backward to inspect or edit an earlier step never automatically resets subsequent completed steps unless the user explicitly requests a reset.
  - *Why*: Prevents anxiety and catastrophic state loss for users who simply want to double-check previous instructions.

***

## 4. Technical Architecture & Data Strategy

### System Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                             MainActivity                               │
│              (Theme, Accessibility State, Edge-To-Edge)                │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
       ┌────────────────────────────┼────────────────────────────┐
       ▼                            ▼                            ▼
┌──────────────┐             ┌──────────────┐             ┌──────────────┐
│  Dashboard   │             │   Builder    │             │  Step Focus  │
│    Screen    │◄───────────►│    Screen    │◄───────────►│    Screen    │
└──────┬───────┘             └──────┬───────┘             └──────┬───────┘
       │                            │                            │
       └────────────────────────────┼────────────────────────────┘
                                    │
                                    ▼
                     ┌─────────────────────────────┐
                     │      StepwiseViewModel      │
                     │  (StateFlow, Preferences)   │
                     └──────────────┬──────────────┘
                                    │
       ┌────────────────────────────┴────────────────────────────┐
       ▼                                                         ▼
┌───────────────────────────────┐               ┌───────────────────────────────┐
│     StepwiseRepository        │               │      GeminiApiService         │
│  - Local: StepwiseRoomDb     │               │  - gemini-3.8-flash (steps)   │
│  - Cloud: Firebase (Optional) │               │  - gemini-4-argon (thinking)  │
└───────────────────────────────┘               └───────────────────────────────┘
```

### Data Entities & Schema

- **`WorkflowEntity`**:
  - `id: String` (UUID)
  - `title: String`
  - `originalText: String`
  - `category: String` (e.g. Government, Technical, Household, Work)
  - `isSample: Boolean`
  - `isAiGenerated: Boolean`
  - `createdAt: Long`
  - `updatedAt: Long`
- **`StepEntity`**:
  - `id: String` (UUID)
  - `workflowId: String`
  - `stepNumber: Int`
  - `title: String`
  - `instructionPlain: String`
  - `warningNotes: String?`
  - `isAmbiguous: Boolean`
  - `ambiguityReason: String?`
  - `isCompleted: Boolean`
  - `isSkipped: Boolean`
- **`UserSettings` (Preferences / DataStore)**:
  - `textSizeScale: Float` (1.0f, 1.2f, 1.4f)
  - `isHighContrast: Boolean`
  - `reducedMotion: Boolean`
  - `autoSpeakSteps: Boolean`

### Interactive Component & State Mapping

- **Input Validation**: Text area enforces minimum length for meaningful instruction extraction, provides sample presets with 1-tap fill.
- **Step Navigation Controls**: Next / Prev / Complete / Skip trigger atomic database updates and instant state flow updates.
- **Ambiguity Resolver Dialog**: Users can tap on flagged ambiguity badges to see why the instruction was flagged and edit the step wording.
- **Verification & Testing**: Compilation via `compile_applet` and Robolectric unit tests to ensure stability, state preservation, and accessibility compliance.
