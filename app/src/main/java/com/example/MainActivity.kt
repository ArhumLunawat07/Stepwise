package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.StepwiseViewModel
import com.example.ui.components.StepAssistantSheet
import com.example.ui.components.TopAccessibilityBar
import com.example.ui.screens.ChecklistReviewScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InstructionBuilderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StepFocusScreen
import com.example.ui.theme.StepwiseTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StepwiseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val accessibilitySettings by viewModel.accessibilitySettings.collectAsStateWithLifecycle()
            val allWorkflows by viewModel.allWorkflows.collectAsStateWithLifecycle()
            val activeWorkflowWithSteps by viewModel.activeWorkflowWithSteps.collectAsStateWithLifecycle()
            val currentStepIndex by viewModel.currentStepIndex.collectAsStateWithLifecycle()
            val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
            val statusMessage by viewModel.generationStatusMessage.collectAsStateWithLifecycle()
            val notification by viewModel.notification.collectAsStateWithLifecycle()
            val isAssistantOpen by viewModel.isAssistantOpen.collectAsStateWithLifecycle()
            val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
            val isChatThinking by viewModel.isChatThinking.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(notification) {
                notification?.let { notif ->
                    snackbarHostState.showSnackbar(
                        message = notif.message,
                        duration = SnackbarDuration.Short
                    )
                    viewModel.clearNotification()
                }
            }

            // Predictive / Hardware Back Button Handling
            BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
                when (currentScreen) {
                    AppScreen.STEP_FOCUS -> viewModel.navigateTo(AppScreen.REVIEW)
                    AppScreen.REVIEW -> viewModel.navigateTo(AppScreen.DASHBOARD)
                    AppScreen.BUILDER -> viewModel.navigateTo(AppScreen.DASHBOARD)
                    AppScreen.SETTINGS -> viewModel.navigateTo(AppScreen.DASHBOARD)
                    AppScreen.DASHBOARD -> Unit
                }
            }

            StepwiseTheme(
                isHighContrast = accessibilitySettings.isHighContrast,
                textScale = accessibilitySettings.textSize.scale,
                reducedMotion = accessibilitySettings.reducedMotion
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAccessibilityBar(
                            settings = accessibilitySettings,
                            onHomeClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                            onCycleTextSize = { viewModel.cycleTextSize() },
                            onToggleHighContrast = { viewModel.toggleHighContrast() },
                            onToggleReadingWidth = { viewModel.toggleComfortableWidth() },
                            onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (currentScreen) {
                            AppScreen.DASHBOARD -> {
                                DashboardScreen(
                                    workflows = allWorkflows,
                                    onOpenWorkflow = { id -> viewModel.openWorkflow(id, startInStepMode = false) },
                                    onResumeWorkflow = { id -> viewModel.resumeWorkflow(id) },
                                    onCreateNewClick = { viewModel.navigateTo(AppScreen.BUILDER) },
                                    onDuplicateWorkflow = { id -> viewModel.duplicateWorkflow(id) },
                                    onDeleteWorkflow = { id -> viewModel.deleteWorkflow(id) },
                                    comfortableWidth = accessibilitySettings.readingWidthComfortable
                                )
                            }

                            AppScreen.BUILDER -> {
                                InstructionBuilderScreen(
                                    isGenerating = isGenerating,
                                    statusMessage = statusMessage,
                                    isGeminiConfigured = viewModel.isGeminiAvailable,
                                    onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                    onSubmit = { title, text, category, engine ->
                                        viewModel.createChecklist(title, text, category, engine)
                                    },
                                    comfortableWidth = accessibilitySettings.readingWidthComfortable
                                )
                            }

                            AppScreen.REVIEW -> {
                                ChecklistReviewScreen(
                                    workflowWithSteps = activeWorkflowWithSteps,
                                    onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                    onStartStepMode = { stepIdx ->
                                        viewModel.selectStepByIndex(stepIdx)
                                        viewModel.navigateTo(AppScreen.STEP_FOCUS)
                                    },
                                    onToggleStep = { stepId -> viewModel.toggleStepCompletion(stepId) },
                                    onUpdateStepContent = { stepId, title, inst ->
                                        viewModel.updateStepContent(stepId, title, inst)
                                    },
                                    onAddStep = { title, inst -> viewModel.addStep(title, inst) },
                                    onDeleteStep = { stepId -> viewModel.deleteStep(stepId) },
                                    onResetProgress = { viewModel.resetProgress() },
                                    comfortableWidth = accessibilitySettings.readingWidthComfortable
                                )
                            }

                            AppScreen.STEP_FOCUS -> {
                                StepFocusScreen(
                                    workflowWithSteps = activeWorkflowWithSteps,
                                    currentStepIndex = currentStepIndex,
                                    onPreviousStep = { viewModel.goToPreviousStep() },
                                    onNextStep = { viewModel.goToNextStep() },
                                    onCompleteStep = { viewModel.markCurrentStepCompleted() },
                                    onSkipStep = { viewModel.markCurrentStepSkipped() },
                                    onSelectStepByIndex = { idx -> viewModel.selectStepByIndex(idx) },
                                    onBreakDownStep = { viewModel.breakDownCurrentStep() },
                                    onOpenAssistant = { viewModel.openAssistant() },
                                    onReviewAllSteps = { viewModel.navigateTo(AppScreen.REVIEW) },
                                    onResetProgress = { viewModel.resetProgress() },
                                    onHome = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                    isGenerating = isGenerating,
                                    comfortableWidth = accessibilitySettings.readingWidthComfortable
                                )
                            }

                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    settings = accessibilitySettings,
                                    onSetTextSize = { opt -> viewModel.setTextSize(opt) },
                                    onToggleHighContrast = { viewModel.toggleHighContrast() },
                                    onToggleComfortableWidth = { viewModel.toggleComfortableWidth() },
                                    onToggleReducedMotion = { viewModel.toggleReducedMotion() },
                                    onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                    comfortableWidth = accessibilitySettings.readingWidthComfortable
                                )
                            }
                        }

                        // Gemini In-Step Companion Bottom Sheet
                        val activeStep = activeWorkflowWithSteps?.steps?.getOrNull(currentStepIndex)
                        StepAssistantSheet(
                            isOpen = isAssistantOpen,
                            stepTitle = activeStep?.title ?: "Current Step",
                            messages = chatMessages,
                            isThinking = isChatThinking,
                            onSendMessage = { q -> viewModel.sendAssistantMessage(q) },
                            onDismiss = { viewModel.closeAssistant() }
                        )
                    }
                }
            }
        }
    }
}
