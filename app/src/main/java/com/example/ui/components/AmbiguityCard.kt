package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalHighContrast
import com.example.ui.theme.StepwiseAmberContainer
import com.example.ui.theme.StepwiseAmberOnContainer
import com.example.ui.theme.StepwiseAmberWarning

@Composable
fun AmbiguityCard(
    reason: String,
    onTapReview: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val highContrast = LocalHighContrast.current

    val bgColor = if (highContrast) Color(0xFFFFF7E6) else StepwiseAmberContainer
    val textColor = if (highContrast) Color(0xFF6B3100) else StepwiseAmberOnContainer
    val borderColor = if (highContrast) Color(0xFF000000) else StepwiseAmberWarning

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .then(
                if (onTapReview != null) {
                    Modifier.clickable(onClick = onTapReview)
                } else Modifier
            )
            .padding(14.dp)
            .testTag("ambiguity_alert_card")
            .semantics {
                contentDescription = "Ambiguity note: $reason"
            }
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Ambiguous Instruction Flagged",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}
