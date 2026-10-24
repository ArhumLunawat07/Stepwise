package com.example.data.repository

import com.example.data.ai.GeminiService
import com.example.data.ai.ParsedStepResult
import com.example.data.ai.RuleBasedParser
import com.example.data.local.StepwiseDao
import com.example.data.model.SampleData
import com.example.data.model.WorkflowEntity
import com.example.data.model.WorkflowStepEntity
import com.example.data.model.WorkflowWithSteps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

enum class ChecklistEngine {
    AI_FAST,
    AI_DEEP_THINKING,
    LOCAL_RULE_BASED
}

class StepwiseRepository(
    private val dao: StepwiseDao
) {

    fun getAllWorkflows(): Flow<List<WorkflowEntity>> {
        return dao.getAllWorkflows()
    }

    fun getWorkflowWithSteps(workflowId: String): Flow<WorkflowWithSteps?> {
        return combine(
            dao.getAllWorkflows(),
            dao.getStepsForWorkflow(workflowId)
        ) { workflows, steps ->
            val wf = workflows.firstOrNull { it.id == workflowId }
            if (wf != null) {
                WorkflowWithSteps(wf, steps)
            } else {
                null
            }
        }
    }

    suspend fun ensureDemoSeeded() {
        if (dao.getWorkflowCount() == 0) {
            seedBuiltInDemo()
        }
    }

    suspend fun seedBuiltInDemo(): String {
        val workflowId = UUID.randomUUID().toString()
        val demoWf = WorkflowEntity(
            id = workflowId,
            title = SampleData.DEMO_TITLE,
            rawInstructions = SampleData.DEMO_RAW_TEXT,
            category = SampleData.DEMO_CATEGORY,
            isSample = true,
            isAiGenerated = true,
            aiModelUsed = "gemini-3.8-flash (Sample Showcase)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val demoSteps = SampleData.createDemoSteps(workflowId)
        dao.saveWorkflowWithSteps(demoWf, demoSteps)
        return workflowId
    }

    suspend fun createChecklist(
        title: String,
        rawText: String,
        category: String,
        engine: ChecklistEngine
    ): Pair<String, Boolean> {
        val workflowId = UUID.randomUUID().toString()
        var usedAi = false
        var modelName = "Deterministic Rule-based Parser"
        var steps: List<WorkflowStepEntity>

        if (engine != ChecklistEngine.LOCAL_RULE_BASED && GeminiService.isConfigured) {
            try {
                val useDeep = engine == ChecklistEngine.AI_DEEP_THINKING
                modelName = if (useDeep) "gemini-4-argon (High Thinking)" else "gemini-3.8-flash"
                val parsedAi = GeminiService.generateChecklist(rawText, useDeepThinking = useDeep)
                if (parsedAi.isNotEmpty()) {
                    steps = parsedAi.map { aiStep ->
                        WorkflowStepEntity(
                            id = UUID.randomUUID().toString(),
                            workflowId = workflowId,
                            stepNumber = aiStep.stepNumber,
                            title = aiStep.title,
                            plainInstruction = aiStep.plainInstruction,
                            details = aiStep.details,
                            warningNote = aiStep.warningNote,
                            isAmbiguous = aiStep.isAmbiguous,
                            ambiguityReason = aiStep.ambiguityReason,
                            isCompleted = false,
                            isSkipped = false
                        )
                    }
                    usedAi = true
                } else {
                    steps = RuleBasedParser.parseInstructions(rawText, workflowId)
                    modelName = "Deterministic Rule-based Parser (AI response empty)"
                }
            } catch (e: Exception) {
                // Graceful fallback to local rule-based parsing
                steps = RuleBasedParser.parseInstructions(rawText, workflowId)
                modelName = "Deterministic Rule-based Parser (Offline / API Fallback)"
            }
        } else {
            steps = RuleBasedParser.parseInstructions(rawText, workflowId)
        }

        // If even rule parser yielded nothing, make a single fallback step
        if (steps.isEmpty()) {
            steps = listOf(
                WorkflowStepEntity(
                    id = UUID.randomUUID().toString(),
                    workflowId = workflowId,
                    stepNumber = 1,
                    title = "Review Instructions",
                    plainInstruction = rawText.take(200),
                    details = "Could not automatically separate instructions. Review manually.",
                    warningNote = null,
                    isAmbiguous = true,
                    ambiguityReason = "Instructions were entered without clear sequential divisions."
                )
            )
        }

        val workflow = WorkflowEntity(
            id = workflowId,
            title = title.ifBlank { "Task Checklist (${steps.size} steps)" },
            rawInstructions = rawText,
            category = category.ifBlank { "General" },
            isSample = false,
            isAiGenerated = usedAi,
            aiModelUsed = modelName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        dao.saveWorkflowWithSteps(workflow, steps)
        return workflowId to usedAi
    }

    suspend fun setStepStatus(stepId: String, isCompleted: Boolean, isSkipped: Boolean) {
        dao.updateStepProgress(stepId, isCompleted, isSkipped)
    }

    suspend fun updateStepContent(stepId: String, title: String, instruction: String) {
        dao.updateStepContent(stepId, title, instruction)
    }

    suspend fun breakDownStep(workflowId: String, stepId: String): Boolean {
        val step = dao.getStepById(stepId) ?: return false
        val subActions = GeminiService.breakdownStep(step.title, step.plainInstruction)
        if (subActions.isEmpty()) return false

        // Replace the step with micro-steps or append details
        val existingSteps = dao.getStepsForWorkflowSync(workflowId)
        val newStepsList = mutableListOf<WorkflowStepEntity>()

        var currentNum = 1
        for (item in existingSteps) {
            if (item.id == stepId) {
                // Insert sub-steps in its place
                for (subAction in subActions) {
                    newStepsList.add(
                        WorkflowStepEntity(
                            id = UUID.randomUUID().toString(),
                            workflowId = workflowId,
                            stepNumber = currentNum++,
                            title = subAction.take(50),
                            plainInstruction = subAction,
                            details = "Sub-action broken down from: ${step.title}",
                            warningNote = step.warningNote,
                            isAmbiguous = false,
                            ambiguityReason = null,
                            isCompleted = false,
                            isSkipped = false
                        )
                    )
                }
            } else {
                newStepsList.add(item.copy(stepNumber = currentNum++))
            }
        }

        val wf = dao.getWorkflowById(workflowId) ?: return false
        dao.saveWorkflowWithSteps(wf.copy(updatedAt = System.currentTimeMillis()), newStepsList)
        return true
    }

    suspend fun duplicateWorkflow(workflowId: String): String {
        return dao.duplicateWorkflow(workflowId)
    }

    suspend fun deleteWorkflow(workflowId: String) {
        dao.deleteWorkflow(workflowId)
    }

    suspend fun addStep(workflowId: String, title: String, instruction: String) {
        val steps = dao.getStepsForWorkflowSync(workflowId)
        val nextStepNumber = (steps.maxOfOrNull { it.stepNumber } ?: 0) + 1
        val newStep = WorkflowStepEntity(
            id = UUID.randomUUID().toString(),
            workflowId = workflowId,
            stepNumber = nextStepNumber,
            title = title.ifBlank { "Step $nextStepNumber" },
            plainInstruction = instruction.ifBlank { "Action to complete" }
        )
        dao.insertSteps(listOf(newStep))
        val wf = dao.getWorkflowById(workflowId)
        if (wf != null) {
            dao.updateWorkflow(wf.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteStep(workflowId: String, stepId: String) {
        dao.deleteStep(stepId)
        val remaining = dao.getStepsForWorkflowSync(workflowId)
        val renumbered = remaining.mapIndexed { idx, s -> s.copy(stepNumber = idx + 1) }
        dao.insertSteps(renumbered)
        val wf = dao.getWorkflowById(workflowId)
        if (wf != null) {
            dao.updateWorkflow(wf.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun resetProgress(workflowId: String) {
        dao.resetAllStepsProgress(workflowId)
    }
}
