package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkflowStepEntity
import com.example.data.model.WorkflowWithSteps
import com.example.ui.components.AmbiguityCard
import com.example.ui.components.StepProgressBar
import com.example.ui.theme.LocalHighContrast
import com.example.ui.theme.StepwiseSuccess

@Composable
fun ChecklistReviewScreen(
    workflowWithSteps: WorkflowWithSteps?,
    onBack: () -> Unit,
    onStartStepMode: (stepIndex: Int) -> Unit,
    onToggleStep: (stepId: String) -> Unit,
    onUpdateStepContent: (stepId: String, title: String, instruction: String) -> Unit,
    onAddStep: (title: String, instruction: String) -> Unit,
    onDeleteStep: (stepId: String) -> Unit,
    onResetProgress: () -> Unit,
    comfortableWidth: Boolean,
    modifier: Modifier = Modifier
) {
    if (workflowWithSteps == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading checklist...")
        }
        return
    }

    val wf = workflowWithSteps.workflow
    val steps = workflowWithSteps.steps
    val highContrast = LocalHighContrast.current

    var editingStep by remember { mutableStateOf<WorkflowStepEntity?>(null) }
    var stepToDelete by remember { mutableStateOf<WorkflowStepEntity?>(null) }
    var isAddingStep by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (comfortableWidth) Modifier.widthIn(max = 680.dp) else Modifier)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_review_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to dashboard"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = wf.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${steps.size} steps • ${wf.category}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (wf.isAiGenerated) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• AI Simplified",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("btn_reset_all_steps")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset checklist progress",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Progress Bar
            item {
                StepProgressBar(
                    currentStepIndex = workflowWithSteps.nextPendingStepIndex,
                    totalSteps = workflowWithSteps.totalSteps,
                    completedCount = workflowWithSteps.completedSteps,
                    skippedCount = workflowWithSteps.skippedSteps
                )
            }

            // Primary Action: Start / Resume Step Focus Mode
            item {
                Button(
                    onClick = { onStartStepMode(workflowWithSteps.nextPendingStepIndex) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_start_step_focus_mode"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (workflowWithSteps.completedSteps > 0)
                            "Resume Step-by-Step Mode (Step ${workflowWithSteps.nextPendingStepIndex + 1})"
                        else
                            "Start Step-by-Step Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Steps List
            itemsIndexed(steps, key = { _, s -> s.id }) { index, step ->
                StepReviewCard(
                    step = step,
                    index = index,
                    onToggle = { onToggleStep(step.id) },
                    onEdit = { editingStep = step },
                    onDelete = { stepToDelete = step },
                    onStepClick = { onStartStepMode(index) }
                )
            }

            // Add Step Button
            item {
                OutlinedButton(
                    onClick = { isAddingStep = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_add_step"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Step", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Step Edit Dialog
    if (editingStep != null) {
        val s = editingStep!!
        var editTitle by remember { mutableStateOf(s.title) }
        var editInstruction by remember { mutableStateOf(s.plainInstruction) }

        AlertDialog(
            onDismissRequest = { editingStep = null },
            title = { Text("Edit Step ${s.stepNumber}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Step Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editInstruction,
                        onValueChange = { editInstruction = it },
                        label = { Text("Plain Instruction") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStepContent(s.id, editTitle, editInstruction)
                        editingStep = null
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingStep = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Checklist Progress?") },
            text = { Text("This will mark all steps as uncompleted so you can start the process from step 1 again. Your instructions and steps will not be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetProgress()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset Progress")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Step Dialog
    if (isAddingStep) {
        var newTitle by remember { mutableStateOf("") }
        var newInstruction by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { isAddingStep = false },
            title = { Text("Add New Step") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Step Title") },
                        placeholder = { Text("e.g. Schedule appointment") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newInstruction,
                        onValueChange = { newInstruction = it },
                        label = { Text("Plain-language Instruction") },
                        placeholder = { Text("Detailed instructions for this step...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() || newInstruction.isNotBlank()) {
                            onAddStep(newTitle, newInstruction)
                            isAddingStep = false
                        }
                    },
                    enabled = newTitle.isNotBlank() || newInstruction.isNotBlank()
                ) {
                    Text("Add Step")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingStep = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Step Dialog
    if (stepToDelete != null) {
        val s = stepToDelete!!
        AlertDialog(
            onDismissRequest = { stepToDelete = null },
            title = { Text("Delete Step ${s.stepNumber}?") },
            text = { Text("Are you sure you want to delete \"${s.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStep(s.id)
                        stepToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { stepToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StepReviewCard(
    step: WorkflowStepEntity,
    index: Int,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStepClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highContrast = LocalHighContrast.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("step_card_${step.stepNumber}"),
        colors = CardDefaults.cardColors(
            containerColor = if (step.isCompleted) {
                if (highContrast) Color.White else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Number Badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (step.isCompleted) StepwiseSuccess else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (step.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "${step.stepNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & plain instruction preview
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onStepClick)
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Edit Button
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Step ${step.stepNumber}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Button
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Step ${step.stepNumber}",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Checkbox
                Checkbox(
                    checked = step.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = StepwiseSuccess),
                    modifier = Modifier.testTag("check_step_${step.stepNumber}")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Plain Instruction Text
            Text(
                text = step.plainInstruction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onStepClick)
            )

            // Warning note if present
            if (!step.warningNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF0ED))
                        .border(1.dp, Color(0xFFC53030), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️ Warning: ${step.warningNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9B2C2C),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Ambiguity alert if flagged
            if (step.isAmbiguous && !step.ambiguityReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AmbiguityCard(reason = step.ambiguityReason)
            }
        }
    }
}
