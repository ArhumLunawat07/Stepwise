package com.example.data.ai

import com.example.data.model.WorkflowStepEntity
import java.util.UUID

object RuleBasedParser {

    private val ACTION_VERBS = listOf(
        "complete", "fill out", "fill", "sign", "attach", "enclose", "mail", "send",
        "pay", "check", "verify", "gather", "review", "submit", "print", "download",
        "staple", "choose", "select", "obtain", "schedule", "bring", "contact"
    )

    private val AMBIGUITY_TRIGGERS = listOf(
        "if applicable" to "Instruction depends on personal conditions that are not explicitly stated.",
        "may vary" to "Timeline or requirement depends on variables not specified in the text.",
        "as needed" to "Discretionary requirement without specific criteria.",
        "as appropriate" to "Unclear standard for when this action is mandatory.",
        "in some cases" to "Condition applies only selectively; check whether it applies to you.",
        "depending on" to "Outcome changes based on external factors not detailed here.",
        "contact agency" to "Directs you to reach out, but does not provide a phone number or link.",
        "seasonal volume" to "Processing time cannot be guaranteed."
    )

    private val WARNING_TRIGGERS = listOf(
        "do not", "never", "must not", "cannot", "warning", "caution", "avoid", "only use", "prohibited"
    )

    fun parseInstructions(rawText: String, workflowId: String): List<WorkflowStepEntity> {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return emptyList()
        }

        val stepCandidates = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (line in lines) {
            val isHeader = line.endsWith(":") ||
                    line.contains("Instructions", ignoreCase = true) && line.length < 90 ||
                    line.contains("all of the following apply", ignoreCase = true)

            // Check if line starts with a list marker: 1., 1), [1], -, *, •, Step 1
            val isListItem = line.matches(Regex("""^(\d+[\.\)]|\-|\*|•|[Ss]tep\s+\d+[:\.]).*"""))
            val startsWithSequentialWord = line.matches(Regex("""^([Ff]irst|[Ss]econd|[Tt]hird|[Nn]ext|[Tt]hen|[Aa]fter\s+that|[Ff]inally|Optional:?)[,\s].*"""))

            if (isHeader) {
                if (currentChunk.isNotEmpty()) {
                    stepCandidates.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
                // Do not append headers as steps
                continue
            }

            if (isListItem || startsWithSequentialWord) {
                if (currentChunk.isNotEmpty()) {
                    stepCandidates.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
                currentChunk.append(line)
            } else {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append(" ").append(line)
                } else {
                    currentChunk.append(line)
                }
            }
        }
        if (currentChunk.isNotEmpty()) {
            stepCandidates.add(currentChunk.toString().trim())
        }

        // If candidates are still too few or single block, split by sentences
        val preliminaryChunks = if (stepCandidates.size <= 1 && rawText.length > 80) {
            rawText.split(Regex("""(?<=[.!?])\s+(?=[A-Z0-9])"""))
                .map { it.trim() }
                .filter { it.length > 10 }
        } else {
            stepCandidates
        }

        // Filter out any standalone preamble chunks
        val cleanChunks = preliminaryChunks.filter { chunk ->
            val stripped = chunk.trim()
            !stripped.endsWith(":") && stripped.length > 15
        }.ifEmpty { preliminaryChunks }

        return cleanChunks.mapIndexed { index, chunk ->
            // Clean list markers from start of chunk
            val stripped = chunk.replace(Regex("""^(\d+[\.\)]|\-|\*|•|[Ss]tep\s+\d+[:\.]|\bOptional[:\.]?)\s*"""), "").trim()

            // Detect warnings
            val warningNote = extractWarning(stripped)

            // Detect ambiguity
            val (isAmbiguous, ambiguityReason) = detectAmbiguity(stripped)

            // Formulate plain title
            val title = formulateTitle(stripped, index + 1)

            WorkflowStepEntity(
                id = UUID.randomUUID().toString(),
                workflowId = workflowId,
                stepNumber = index + 1,
                title = title,
                plainInstruction = stripped,
                details = "Extracted from source instructions via local rule parser.",
                warningNote = warningNote,
                isAmbiguous = isAmbiguous,
                ambiguityReason = ambiguityReason,
                isCompleted = false,
                isSkipped = false
            )
        }
    }

    private fun extractWarning(text: String): String? {
        val lower = text.lowercase()
        for (trigger in WARNING_TRIGGERS) {
            val idx = lower.indexOf(trigger)
            if (idx != -1) {
                // Return sentence or clause containing the warning
                val endIdx = text.indexOfAny(charArrayOf('.', ';', '\n'), startIndex = idx)
                return if (endIdx != -1) {
                    text.substring(idx, endIdx).trim()
                } else {
                    text.substring(idx).trim()
                }
            }
        }
        return null
    }

    private fun detectAmbiguity(text: String): Pair<Boolean, String?> {
        val lower = text.lowercase()
        for ((trigger, reason) in AMBIGUITY_TRIGGERS) {
            if (lower.contains(trigger)) {
                return true to reason
            }
        }
        return false to null
    }

    private fun formulateTitle(text: String, stepNumber: Int): String {
        val firstSentence = text.split(Regex("""[.?!;]""")).firstOrNull()?.trim() ?: text
        val lower = firstSentence.lowercase()

        for (verb in ACTION_VERBS) {
            val idx = lower.indexOf(verb)
            if (idx != -1) {
                val candidate = firstSentence.substring(idx)
                val words = candidate.split(" ")
                val shortTitle = words.take(6).joinToString(" ")
                return shortTitle.replaceFirstChar { it.uppercase() }
            }
        }

        // Fallback: take first 6-8 words of first sentence
        val words = firstSentence.split(" ")
        return if (words.size > 7) {
            words.take(7).joinToString(" ") + "..."
        } else {
            firstSentence.ifEmpty { "Step $stepNumber" }
        }
    }
}
