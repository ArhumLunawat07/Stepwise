package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.RuleBasedParser
import com.example.data.local.StepwiseDatabase
import com.example.data.model.SampleData
import com.example.data.model.WorkflowEntity
import com.example.data.model.WorkflowStepEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies Stepwise app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Stepwise", appName)
    }

    @Test
    fun `rule-based parser extracts sequential steps and flags ambiguity`() {
        val rawText = """
            1. Complete Form DS-82 using black ink.
            2. Attach 2x2 color photo with 4 staples.
            3. Processing times may vary depending on seasonal volume.
        """.trimIndent()

        val steps = RuleBasedParser.parseInstructions(rawText, "test-wf-id")
        assertEquals(3, steps.size)

        // Step 1: title should start with action verb
        assertTrue(steps[0].title.contains("Complete", ignoreCase = true) || steps[0].title.contains("Form", ignoreCase = true))
        assertFalse(steps[0].isAmbiguous)

        // Step 3: ambiguity detected for "may vary" and "seasonal volume"
        assertTrue(steps[2].isAmbiguous)
        assertNotNull(steps[2].ambiguityReason)
    }

    @Test
    fun `room database stores and retrieves workflows and steps`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = StepwiseDatabase.getDatabase(context)
        val dao = db.stepwiseDao()

        val wfId = UUID.randomUUID().toString()
        val wf = WorkflowEntity(
            id = wfId,
            title = "Test Passport Renewal",
            rawInstructions = SampleData.DEMO_RAW_TEXT,
            category = "Government"
        )
        val steps = listOf(
            WorkflowStepEntity(
                id = UUID.randomUUID().toString(),
                workflowId = wfId,
                stepNumber = 1,
                title = "Check eligibility",
                plainInstruction = "Confirm passport issued after age 16"
            )
        )

        dao.saveWorkflowWithSteps(wf, steps)

        val retrievedWf = dao.getWorkflowById(wfId)
        assertNotNull(retrievedWf)
        assertEquals("Test Passport Renewal", retrievedWf?.title)

        val retrievedSteps = dao.getStepsForWorkflowSync(wfId)
        assertEquals(1, retrievedSteps.size)
        assertEquals("Check eligibility", retrievedSteps[0].title)

        // Test status update
        dao.updateStepProgress(retrievedSteps[0].id, isCompleted = true, isSkipped = false)
        val updatedStep = dao.getStepById(retrievedSteps[0].id)
        assertTrue(updatedStep?.isCompleted == true)
    }

    @Test
    fun `repository supports adding and deleting steps`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = StepwiseDatabase.getDatabase(context)
        val repo = com.example.data.repository.StepwiseRepository(db.stepwiseDao())

        val (wfId, _) = repo.createChecklist(
            title = "Test Workflow",
            rawText = "1. First action.\n2. Second action.",
            category = "Workplace",
            engine = com.example.data.repository.ChecklistEngine.LOCAL_RULE_BASED
        )

        // Add a third step
        repo.addStep(wfId, "Third step", "Instruction for step 3")
        val stepsAfterAdd = db.stepwiseDao().getStepsForWorkflowSync(wfId)
        assertEquals(3, stepsAfterAdd.size)
        assertEquals(3, stepsAfterAdd.last().stepNumber)

        // Delete the second step
        val stepToDelete = stepsAfterAdd[1]
        repo.deleteStep(wfId, stepToDelete.id)
        val stepsAfterDelete = db.stepwiseDao().getStepsForWorkflowSync(wfId)
        assertEquals(2, stepsAfterDelete.size)
        assertEquals(1, stepsAfterDelete[0].stepNumber)
        assertEquals(2, stepsAfterDelete[1].stepNumber)
    }
}
