package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.WorkflowEntity
import com.example.data.model.WorkflowStepEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface StepwiseDao {
    @Query("SELECT * FROM workflows ORDER BY updatedAt DESC")
    fun getAllWorkflows(): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE id = :id LIMIT 1")
    suspend fun getWorkflowById(id: String): WorkflowEntity?

    @Query("SELECT * FROM workflow_steps WHERE workflowId = :workflowId ORDER BY stepNumber ASC")
    fun getStepsForWorkflow(workflowId: String): Flow<List<WorkflowStepEntity>>

    @Query("SELECT * FROM workflow_steps WHERE workflowId = :workflowId ORDER BY stepNumber ASC")
    suspend fun getStepsForWorkflowSync(workflowId: String): List<WorkflowStepEntity>

    @Query("SELECT * FROM workflow_steps WHERE id = :stepId LIMIT 1")
    suspend fun getStepById(stepId: String): WorkflowStepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: WorkflowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<WorkflowStepEntity>)

    @Update
    suspend fun updateWorkflow(workflow: WorkflowEntity)

    @Update
    suspend fun updateStep(step: WorkflowStepEntity)

    @Query("UPDATE workflow_steps SET isCompleted = :isCompleted, isSkipped = :isSkipped WHERE id = :stepId")
    suspend fun updateStepProgress(stepId: String, isCompleted: Boolean, isSkipped: Boolean)

    @Query("UPDATE workflow_steps SET title = :title, plainInstruction = :instruction WHERE id = :stepId")
    suspend fun updateStepContent(stepId: String, title: String, instruction: String)

    @Query("DELETE FROM workflows WHERE id = :id")
    suspend fun deleteWorkflow(id: String)

    @Query("DELETE FROM workflow_steps WHERE id = :stepId")
    suspend fun deleteStep(stepId: String)

    @Query("DELETE FROM workflow_steps WHERE workflowId = :workflowId")
    suspend fun deleteStepsForWorkflow(workflowId: String)

    @Query("SELECT COUNT(*) FROM workflows")
    suspend fun getWorkflowCount(): Int

    @Transaction
    suspend fun saveWorkflowWithSteps(workflow: WorkflowEntity, steps: List<WorkflowStepEntity>) {
        insertWorkflow(workflow)
        deleteStepsForWorkflow(workflow.id)
        insertSteps(steps)
    }

    @Transaction
    suspend fun duplicateWorkflow(workflowId: String): String {
        val original = getWorkflowById(workflowId) ?: throw IllegalArgumentException("Workflow not found")
        val originalSteps = getStepsForWorkflowSync(workflowId)
        val newWorkflowId = UUID.randomUUID().toString()
        val duplicatedWorkflow = original.copy(
            id = newWorkflowId,
            title = "${original.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val duplicatedSteps = originalSteps.map { step ->
            step.copy(
                id = UUID.randomUUID().toString(),
                workflowId = newWorkflowId,
                isCompleted = false,
                isSkipped = false
            )
        }
        saveWorkflowWithSteps(duplicatedWorkflow, duplicatedSteps)
        return newWorkflowId
    }

    @Transaction
    suspend fun resetAllStepsProgress(workflowId: String) {
        val steps = getStepsForWorkflowSync(workflowId)
        val resetSteps = steps.map { it.copy(isCompleted = false, isSkipped = false) }
        insertSteps(resetSteps)
    }
}
