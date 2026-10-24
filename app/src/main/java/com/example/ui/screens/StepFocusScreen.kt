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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkflowStepEntity
import com.example.data.model.WorkflowWithSteps
import com.example.ui.components.AmbiguityCard
import com.example.ui.components.StepProgressBar
import com.example.ui.theme.LocalHighContrast
import com.example.ui.theme.StepwiseSuccess
import com.example.ui.theme.StepwiseSuccessContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepFocusScreen(
    workflowWithSteps: WorkflowWithSteps?,
    currentStepIndex: Int,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onCompleteStep: () -> Unit,
    onSkipStep: () -> Unit,
    onSelectStepByIndex: (Int) -> Unit,
    onBreakDownStep: () -> Unit,
    onOpenAssistant: () -> Unit,
    onReviewAllSteps: () -> Unit,
    onResetProgress: () -> Unit,
    onHome: () -> Unit,
    isGenerating: Boolean,
    comfortableWidth: Boolean,
    modifier: Modifier = Modifier
) {
    if (workflowWithSteps == null || workflowWithSteps.steps.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No steps available.")
        }
        return
    }

    val wf = workflowWithSteps.workflow
    val steps = workflowWithSteps.steps
    val safeIndex = currentStepIndex.coerceIn(0, steps.size - 1)
    val step = steps[safeIndex]
    val highContrast = LocalHighContrast.current
    var showAllStepsSheet by remember { mutableStateOf(false) }

    // If all steps are completed or skipped, show celebratory task summary
    if (workflowWithSteps.isAllFinished) {
        TaskFinishedCelebrationView(
            workflowWithSteps = workflowWithSteps,
            onReviewSteps = onReviewAllSteps,
            onResetProgress = onResetProgress,
            onHome = onHome,
            comfortableWidth = comfortableWidth,
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (comfortableWidth) Modifier.widthIn(max = 680.dp) else Modifier)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onReviewAllSteps,
                        modifier = Modifier.testTag("btn_focus_back_to_review")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Review full checklist"
                        )
                    }

                    Text(
                        text = wf.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    OutlinedButton(
                        onClick = { showAllStepsSheet = true },
                        modifier = Modifier.testTag("btn_open_all_steps_drawer"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All (${steps.size})", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar
                StepProgressBar(
                    currentStepIndex = safeIndex,
                    totalSteps = steps.size,
                    completedCount = workflowWithSteps.completedSteps,
                    skippedCount = workflowWithSteps.skippedSteps
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Main Focus Step Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_step_focus_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(width = if (highContrast) 2.dp else 1.dp)
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        // Step number label & status badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "STEP ${step.stepNumber}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (step.isCompleted) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StepwiseSuccessContainer)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "✓ Completed",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StepwiseSuccess
                                    )
                                }
                            } else if (step.isSkipped) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Skipped",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Action Title
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics {
                                contentDescription = "Current Action: ${step.title}"
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Plain-Language Detailed Instruction
                        Text(
                            text = step.plainInstruction,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Secondary Details if present
                        if (step.details.isNotBlank() && step.details != step.plainInstruction) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = step.details,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Warning Box
                        if (!step.warningNote.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFF0ED))
                                    .border(1.dp, Color(0xFFC53030), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFC53030),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = step.warningNote,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF9B2C2C)
                                )
                            }
                        }

                        // Ambiguity Alert
                        if (step.isAmbiguous && !step.ambiguityReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            AmbiguityCard(
                                reason = step.ambiguityReason,
                                onTapReview = onOpenAssistant
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // In-step intelligent tools row: Break down & Ask Assistant
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBreakDownStep,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_breakdown_step"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Break into Smaller Steps", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            OutlinedButton(
                                onClick = onOpenAssistant,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_open_step_assistant"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask Assistant", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Bottom Step Navigation Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Complete & Advance Button (Large 56dp Touch Target)
                Button(
                    onClick = onCompleteStep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_complete_and_advance"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (highContrast) MaterialTheme.colorScheme.primary else StepwiseSuccess
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (safeIndex == steps.size - 1) "Complete Task" else "Mark Complete & Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Previous, Skip, Next Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Previous
                    OutlinedButton(
                        onClick = onPreviousStep,
                        enabled = safeIndex > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_previous_step"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }

                    // Skip
                    OutlinedButton(
                        onClick = onSkipStep,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_skip_step"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Skip")
                    }

                    // Next without completing
                    OutlinedButton(
                        onClick = onNextStep,
                        enabled = safeIndex < steps.size - 1,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_next_step"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // Modal Sheet: View & Jump to All Steps
    if (showAllStepsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { showAllStepsSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "All Steps (${steps.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    steps.forEachIndexed { idx, s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (idx == safeIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    onSelectStepByIndex(idx)
                                    showAllStepsSheet = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (s.isCompleted) StepwiseSuccess else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (s.isCompleted) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("${s.stepNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = s.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (idx == safeIndex) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun TaskFinishedCelebrationView(
    workflowWithSteps: WorkflowWithSteps,
    onReviewSteps: () -> Unit,
    onResetProgress: () -> Unit,
    onHome: () -> Unit,
    comfortableWidth: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (comfortableWidth) Modifier.widthIn(max = 680.dp) else Modifier)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(StepwiseSuccessContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StepwiseSuccess,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Task Accomplished!",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You completed ${workflowWithSteps.completedSteps} of ${workflowWithSteps.totalSteps} steps in \"${workflowWithSteps.workflow.title}\".",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Accomplishments Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    workflowWithSteps.steps.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (s.isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (s.isCompleted) StepwiseSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_finished_return_home"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Return to Dashboard", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onReviewSteps,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_finished_review_all_steps"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FormatListNumbered, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Review All Steps")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onResetProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_finished_start_again"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Task Again")
            }
        }
    }
}
