package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val rawInstructions: String,
    val category: String = "General",
    val isSample: Boolean = false,
    val isAiGenerated: Boolean = false,
    val aiModelUsed: String = "local-rule",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "workflow_steps",
    foreignKeys = [
        ForeignKey(
            entity = WorkflowEntity::class,
            parentColumns = ["id"],
            childColumns = ["workflowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workflowId"])]
)
data class WorkflowStepEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workflowId: String,
    val stepNumber: Int,
    val title: String,
    val plainInstruction: String,
    val details: String = "",
    val warningNote: String? = null,
    val isAmbiguous: Boolean = false,
    val ambiguityReason: String? = null,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false
)

data class WorkflowWithSteps(
    val workflow: WorkflowEntity,
    val steps: List<WorkflowStepEntity>
) {
    val totalSteps: Int get() = steps.size
    val completedSteps: Int get() = steps.count { it.isCompleted }
    val skippedSteps: Int get() = steps.count { it.isSkipped }
    val progressFraction: Float
        get() = if (totalSteps == 0) 0f else (completedSteps.toFloat() / totalSteps.toFloat())
    val isAllFinished: Boolean
        get() = totalSteps > 0 && steps.all { it.isCompleted || it.isSkipped }
    val nextPendingStepIndex: Int
        get() {
            val idx = steps.indexOfFirst { !it.isCompleted && !it.isSkipped }
            return if (idx >= 0) idx else (steps.size - 1).coerceAtLeast(0)
        }
}

enum class TextSizeOption(val label: String, val scale: Float) {
    STANDARD("Standard (16sp)", 1.0f),
    LARGE("Large (19sp)", 1.20f),
    EXTRA_LARGE("Extra Large (23sp)", 1.45f)
}

data class AccessibilitySettings(
    val textSize: TextSizeOption = TextSizeOption.STANDARD,
    val isHighContrast: Boolean = false,
    val readingWidthComfortable: Boolean = true,
    val reducedMotion: Boolean = false
)

object SampleData {
    const val DEMO_TITLE = "Adult Passport Renewal (Form DS-82)"
    const val DEMO_CATEGORY = "Government & Legal Filing"

    const val DEMO_RAW_TEXT = """
U.S. Department of State — Passport Renewal by Mail (Form DS-82) Instructions:

You can only renew by mail using Form DS-82 if all of the following apply:
1. You can submit your most recent U.S. passport book and/or card with your application.
2. Your previous passport was issued when you were age 16 or older.
3. Your previous passport was issued within the past 15 years.
4. Your previous passport was not severely damaged or reported lost/stolen.
5. You use the exact same legal name, or can submit certified documentation (such as a marriage certificate or court order) proving your legal name change.

Required Materials and Steps:
- Complete Form DS-82 online or download the PDF. Fill it out completely using black ink only. Sign and date on page 1.
- Attach one standard 2x2 inch color passport photograph taken within the past 6 months. Staple the photo vertically using 4 staples in the corners as indicated. Do not bend the photo.
- Enclose your current undamaged passport. It will be cancelled and returned separately.
- Include a personal check or money order payable to "U.S. Department of State". The standard fee is $130 for a passport book. Write your full legal name and date of birth on the memo line. Do NOT send cash or postdated checks.
- Optional: If you need expedited service, add an extra $60 fee to the check and clearly write "EXPEDITE" on the outside of the mailing envelope. Processing times may vary depending on seasonal volume.
- Mail your envelope via USPS Priority Mail with tracking to the designated National Passport Processing Center address for your state. Do not use private couriers (FedEx/UPS) for PO Box delivery addresses.
"""

    fun createDemoSteps(workflowId: String): List<WorkflowStepEntity> {
        return listOf(
            WorkflowStepEntity(
                id = UUID.randomUUID().toString(),
                workflowId = workflowId,
                stepNumber = 1,
                title = "Check eligibility to renew by mail",
                plainInstruction = "Confirm you have your most recent passport, it was issued after age 16 and within the last 15 years, and it is not damaged.",
                details = "If your passport was issued when you were under 16 or more than 15 years ago, you cannot use Form DS-82 and must apply in person using Form DS-11.",
                warningNote = "Do not proceed with Form DS-82 if your passport was reported lost or stolen.",
                isAmbiguous = false,
                ambiguityReason = null
            ),
            WorkflowStepEntity(
                id = UUID.randomUUID().toString(),
                workflowId = workflowId,
                stepNumber = 2,
                title = "Fill out Form DS-82 completely",
                plainInstruction = "Fill in every field on Form DS-82 using black ink, then sign and date page 1.",
                details = "You can fill it online at pptform.state.gov and print it, or write clearly in black ink. Blue ink or pencil is not accepted.",
                warningNote = "An unsigned form will be returned to you unprocessed, delaying your passport by weeks.",
                isAmbiguous = false,
                ambiguityReason = null
            ),
            WorkflowStepEntity(
                id = UUID.randomUUID().toString(),
                workflowId = workflowId,
                stepNumber = 3,
                title = "Get a fresh 2x2 passport photo",
                plainInstruction = "Obtain one 2x2 inch color photo taken within the last 6 months against a plain white background.",
                details = "No glasses, uniforms, or head coverings unless religious or medical reasons are documented. Staple photo with 4 staples in the marked corners.",
                warningNote = "Do not glue or tape the photo. Only staple in the designated corner boxes.",
                isAmbiguous = false,
                ambiguityReason = null
            ),
            WorkflowStepEntity(
                id = UUID.randomUUID().toString(),
                workflowId = workflowId,
                stepNumber = 4,
                title = "Prepare the fee payment check",
                plainInstruction = "Write a check or money order for \$130 payable to 'U.S. Department of State'. Write your full name and birthdate on the memo line.",
                details = "Standard passport book renewal fee is \$130. If also adding a passport card, add \$30. For expedited service, add \$60.",
                warningNote = "Never send cash. Cash cannot be accepted and will void your application.",
                isAmbiguous = false,
                ambiguityReason = null
            ),
            WorkflowStepEntity(
                id = UUID.randomUUID().toString(),
                workflowId = workflowId,
                stepNumber = 5,
                title = "Decide on expedited processing",
                plainInstruction = "Choose whether you need regular service (6-8 weeks) or expedited service (2-3 weeks).",
                details = "If expediting, add \$60 to your payment check and mark 'EXPEDITE' clearly on the outside of your mailing envelope.",
                warningNote = null,
                isAmbiguous = true,
                ambiguityReason = "Instruction states 'Processing times may vary depending on seasonal volume' without giving guaranteed delivery dates."
            ),
            WorkflowStepEntity(
                id = UUID.randomUUID().toString(),
                workflowId = workflowId,
                stepNumber = 6,
                title = "Gather legal name change documents (if applicable)",
                plainInstruction = "If your current legal name differs from your previous passport, enclose an original certified marriage certificate or court order.",
                details = "Photocopies are not accepted. The original document will be reviewed and mailed back to you separately.",
                warningNote = null,
                isAmbiguous = true,
                ambiguityReason = "Conditional step: only required if your name changed since your last passport was issued."
            ),
            WorkflowStepEntity(
                id = UUID.randomUUID().toString(),
                workflowId = workflowId,
                stepNumber = 7,
                title = "Mail package via USPS Priority Mail with tracking",
                plainInstruction = "Put your old passport, signed DS-82 with stapled photo, and payment check into a secure envelope and send via USPS with tracking.",
                details = "Check the state-specific mailing address in the DS-82 instructions. Do not use FedEx or UPS because they cannot deliver to postal PO Boxes.",
                warningNote = "Save your USPS tracking receipt so you can monitor your package delivery online.",
                isAmbiguous = false,
                ambiguityReason = null
            )
        )
    }
}
