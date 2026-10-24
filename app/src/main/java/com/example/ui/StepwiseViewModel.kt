package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ChatMessage
import com.example.data.ai.GeminiService
import com.example.data.local.StepwiseDatabase
import com.example.data.model.AccessibilitySettings
import com.example.data.model.TextSizeOption
import com.example.data.model.WorkflowEntity
import com.example.data.model.WorkflowStepEntity
import com.example.data.model.WorkflowWithSteps
import com.example.data.repository.ChecklistEngine
import com.example.data.repository.StepwiseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    DASHBOARD,
    BUILDER,
    REVIEW,
    STEP_FOCUS,
    SETTINGS
}

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

class StepwiseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StepwiseRepository = StepwiseRepository(
        StepwiseDatabase.getDatabase(application).stepwiseDao()
    )

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedWorkflowId = MutableStateFlow<String?>(null)
    val selectedWorkflowId: StateFlow<String?> = _selectedWorkflowId.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _activeWorkflowWithSteps = MutableStateFlow<WorkflowWithSteps?>(null)
    val activeWorkflowWithSteps: StateFlow<WorkflowWithSteps?> = _activeWorkflowWithSteps.asStateFlow()

    private val _accessibilitySettings = MutableStateFlow(AccessibilitySettings())
    val accessibilitySettings: StateFlow<AccessibilitySettings> = _accessibilitySettings.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationStatusMessage = MutableStateFlow("")
    val generationStatusMessage: StateFlow<String> = _generationStatusMessage.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    // Assistant Chat State
    private val _isAssistantOpen = MutableStateFlow(false)
    val isAssistantOpen: StateFlow<Boolean> = _isAssistantOpen.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatThinking = MutableStateFlow(false)
    val isChatThinking: StateFlow<Boolean> = _isChatThinking.asStateFlow()

    // Ambiguity Explanation Modal
    private val _inspectedStep = MutableStateFlow<WorkflowStepEntity?>(null)
    val inspectedStep: StateFlow<WorkflowStepEntity?> = _inspectedStep.asStateFlow()

    val allWorkflows: StateFlow<List<WorkflowEntity>> = repository.getAllWorkflows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isGeminiAvailable: Boolean = GeminiService.isConfigured

    init {
        viewModelScope.launch {
            repository.ensureDemoSeeded()
        }
    }

    private var workflowJob: Job? = null

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun openWorkflow(workflowId: String, startInStepMode: Boolean = false) {
        _selectedWorkflowId.value = workflowId
        workflowJob?.cancel()
        var isInitial = true
        workflowJob = viewModelScope.launch {
            repository.getWorkflowWithSteps(workflowId).collect { wfWithSteps ->
                _activeWorkflowWithSteps.value = wfWithSteps
                if (wfWithSteps != null) {
                    if (isInitial || _currentStepIndex.value >= wfWithSteps.steps.size) {
                        _currentStepIndex.value = wfWithSteps.nextPendingStepIndex
                        isInitial = false
                    }
                }
            }
        }
        _currentScreen.value = if (startInStepMode) AppScreen.STEP_FOCUS else AppScreen.REVIEW
    }

    fun resumeWorkflow(workflowId: String) {
        openWorkflow(workflowId, startInStepMode = true)
    }

    fun createChecklist(
        title: String,
        rawText: String,
        category: String,
        engine: ChecklistEngine
    ) {
        if (rawText.isBlank()) {
            _notification.value = UiNotification("Please enter or paste instructions first.", isError = true)
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _generationStatusMessage.value = when (engine) {
                ChecklistEngine.AI_DEEP_THINKING -> "Analyzing dense instructions with Gemini 4 Argon deep thinking..."
                ChecklistEngine.AI_FAST -> "Simplifying instructions into plain-language steps..."
                ChecklistEngine.LOCAL_RULE_BASED -> "Parsing instructions with local deterministic rules..."
            }
            try {
                val (newId, usedAi) = repository.createChecklist(title, rawText, category, engine)
                _isGenerating.value = false
                _notification.value = UiNotification(
                    if (usedAi) "Checklist created with Gemini AI!" else "Checklist created with offline rules!"
                )
                openWorkflow(newId, startInStepMode = false)
            } catch (e: Exception) {
                _isGenerating.value = false
                _notification.value = UiNotification("Failed to create checklist: ${e.message}", isError = true)
            }
        }
    }

    fun markCurrentStepCompleted() {
        val wf = _activeWorkflowWithSteps.value ?: return
        val currentIdx = _currentStepIndex.value
        val step = wf.steps.getOrNull(currentIdx) ?: return

        viewModelScope.launch {
            repository.setStepStatus(step.id, isCompleted = true, isSkipped = false)
            // Advance to next step if available
            if (currentIdx + 1 < wf.steps.size) {
                _currentStepIndex.value = currentIdx + 1
            }
        }
    }

    fun markCurrentStepSkipped() {
        val wf = _activeWorkflowWithSteps.value ?: return
        val currentIdx = _currentStepIndex.value
        val step = wf.steps.getOrNull(currentIdx) ?: return

        viewModelScope.launch {
            repository.setStepStatus(step.id, isCompleted = false, isSkipped = true)
            if (currentIdx + 1 < wf.steps.size) {
                _currentStepIndex.value = currentIdx + 1
            }
        }
    }

    fun toggleStepCompletion(stepId: String) {
        val wf = _activeWorkflowWithSteps.value ?: return
        val step = wf.steps.firstOrNull { it.id == stepId } ?: return
        viewModelScope.launch {
            repository.setStepStatus(stepId, isCompleted = !step.isCompleted, isSkipped = false)
        }
    }

    fun goToPreviousStep() {
        if (_currentStepIndex.value > 0) {
            _currentStepIndex.value -= 1
        }
    }

    fun goToNextStep() {
        val maxIdx = (_activeWorkflowWithSteps.value?.steps?.size ?: 1) - 1
        if (_currentStepIndex.value < maxIdx) {
            _currentStepIndex.value += 1
        }
    }

    fun selectStepByIndex(index: Int) {
        val maxIdx = (_activeWorkflowWithSteps.value?.steps?.size ?: 1) - 1
        if (index in 0..maxIdx) {
            _currentStepIndex.value = index
        }
    }

    fun updateStepContent(stepId: String, title: String, instruction: String) {
        viewModelScope.launch {
            repository.updateStepContent(stepId, title, instruction)
            _notification.value = UiNotification("Step updated successfully.")
        }
    }

    fun addStep(title: String, instruction: String) {
        val wfId = _selectedWorkflowId.value ?: return
        viewModelScope.launch {
            repository.addStep(wfId, title, instruction)
            _notification.value = UiNotification("New step added.")
        }
    }

    fun deleteStep(stepId: String) {
        val wfId = _selectedWorkflowId.value ?: return
        viewModelScope.launch {
            repository.deleteStep(wfId, stepId)
            _notification.value = UiNotification("Step removed.")
        }
    }

    fun breakDownCurrentStep() {
        val wf = _activeWorkflowWithSteps.value ?: return
        val currentIdx = _currentStepIndex.value
        val step = wf.steps.getOrNull(currentIdx) ?: return

        viewModelScope.launch {
            _isGenerating.value = true
            _generationStatusMessage.value = "Breaking step into micro-actions with low-latency Gemini..."
            try {
                val success = repository.breakDownStep(wf.workflow.id, step.id)
                _isGenerating.value = false
                if (success) {
                    _notification.value = UiNotification("Step divided into simpler micro-actions!")
                } else {
                    _notification.value = UiNotification("Could not break down step.", isError = true)
                }
            } catch (e: Exception) {
                _isGenerating.value = false
                _notification.value = UiNotification("Error breaking down step: ${e.message}", isError = true)
            }
        }
    }

    fun duplicateWorkflow(workflowId: String) {
        viewModelScope.launch {
            try {
                val newId = repository.duplicateWorkflow(workflowId)
                _notification.value = UiNotification("Checklist duplicated.")
                openWorkflow(newId, startInStepMode = false)
            } catch (e: Exception) {
                _notification.value = UiNotification("Failed to duplicate: ${e.message}", isError = true)
            }
        }
    }

    fun deleteWorkflow(workflowId: String) {
        viewModelScope.launch {
            try {
                repository.deleteWorkflow(workflowId)
                if (_selectedWorkflowId.value == workflowId) {
                    _selectedWorkflowId.value = null
                    _activeWorkflowWithSteps.value = null
                    _currentScreen.value = AppScreen.DASHBOARD
                }
                _notification.value = UiNotification("Checklist deleted.")
            } catch (e: Exception) {
                _notification.value = UiNotification("Failed to delete: ${e.message}", isError = true)
            }
        }
    }

    fun resetProgress() {
        val wfId = _selectedWorkflowId.value ?: return
        viewModelScope.launch {
            repository.resetProgress(wfId)
            _currentStepIndex.value = 0
            _notification.value = UiNotification("Progress reset to step 1.")
        }
    }

    fun inspectStepAmbiguity(step: WorkflowStepEntity?) {
        _inspectedStep.value = step
    }

    // Accessibility Controls
    fun cycleTextSize() {
        val current = _accessibilitySettings.value.textSize
        val next = when (current) {
            TextSizeOption.STANDARD -> TextSizeOption.LARGE
            TextSizeOption.LARGE -> TextSizeOption.EXTRA_LARGE
            TextSizeOption.EXTRA_LARGE -> TextSizeOption.STANDARD
        }
        _accessibilitySettings.value = _accessibilitySettings.value.copy(textSize = next)
    }

    fun setTextSize(option: TextSizeOption) {
        _accessibilitySettings.value = _accessibilitySettings.value.copy(textSize = option)
    }

    fun toggleHighContrast() {
        val current = _accessibilitySettings.value.isHighContrast
        _accessibilitySettings.value = _accessibilitySettings.value.copy(isHighContrast = !current)
    }

    fun toggleComfortableWidth() {
        val current = _accessibilitySettings.value.readingWidthComfortable
        _accessibilitySettings.value = _accessibilitySettings.value.copy(readingWidthComfortable = !current)
    }

    fun toggleReducedMotion() {
        val current = _accessibilitySettings.value.reducedMotion
        _accessibilitySettings.value = _accessibilitySettings.value.copy(reducedMotion = !current)
    }

    // In-Step Assistant Chat
    fun openAssistant() {
        _isAssistantOpen.value = true
        if (_chatMessages.value.isEmpty()) {
            val step = _activeWorkflowWithSteps.value?.steps?.getOrNull(_currentStepIndex.value)
            val welcomeText = if (step != null) {
                "Hello! I am your Stepwise Task Companion. I'm here to clarify anything about \"${step.title}\". What can I help with?"
            } else {
                "Hello! I am your Stepwise Task Companion. How can I help clarify your task?"
            }
            _chatMessages.value = listOf(ChatMessage(role = "model", text = welcomeText))
        }
    }

    fun closeAssistant() {
        _isAssistantOpen.value = false
    }

    fun sendAssistantMessage(userQuestion: String) {
        if (userQuestion.isBlank()) return
        val currentMsgs = _chatMessages.value.toMutableList()
        currentMsgs.add(ChatMessage(role = "user", text = userQuestion))
        _chatMessages.value = currentMsgs

        val step = _activeWorkflowWithSteps.value?.steps?.getOrNull(_currentStepIndex.value)
        val stepTitle = step?.title ?: "General Task"
        val stepInst = step?.plainInstruction ?: ""

        viewModelScope.launch {
            _isChatThinking.value = true
            val reply = GeminiService.askAssistant(
                question = userQuestion,
                currentStepTitle = stepTitle,
                currentStepInstruction = stepInst,
                history = currentMsgs
            )
            _isChatThinking.value = false
            val updated = _chatMessages.value.toMutableList()
            updated.add(ChatMessage(role = "model", text = reply))
            _chatMessages.value = updated
        }
    }

    fun clearNotification() {
        _notification.value = null
    }
}
