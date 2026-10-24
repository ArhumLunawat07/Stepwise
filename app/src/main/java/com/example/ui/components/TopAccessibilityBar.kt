package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import com.example.data.model.AccessibilitySettings
import com.example.data.model.TextSizeOption

@Composable
fun TopAccessibilityBar(
    settings: AccessibilitySettings,
    onHomeClick: () -> Unit,
    onCycleTextSize: () -> Unit,
    onToggleHighContrast: () -> Unit,
    onToggleReadingWidth: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Home / Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onHomeClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("nav_home_button")
                    .semantics { contentDescription = "Stepwise Home" }
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Stepwise",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Quick Accessibility Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Text size indicator button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(onClick = onCycleTextSize)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("btn_cycle_text_size")
                        .semantics {
                            contentDescription = "Text size: ${settings.textSize.label}. Tap to enlarge."
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (settings.textSize) {
                                TextSizeOption.STANDARD -> "A"
                                TextSizeOption.LARGE -> "A+"
                                TextSizeOption.EXTRA_LARGE -> "A++"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // High Contrast Button
                IconButton(
                    onClick = onToggleHighContrast,
                    modifier = Modifier
                        .testTag("btn_toggle_high_contrast")
                        .semantics {
                            contentDescription = if (settings.isHighContrast)
                                "High contrast enabled. Tap for standard theme."
                            else
                                "Tap to enable high contrast theme."
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Contrast,
                        contentDescription = "High Contrast Mode",
                        tint = if (settings.isHighContrast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Reading Width Button
                IconButton(
                    onClick = onToggleReadingWidth,
                    modifier = Modifier
                        .testTag("btn_toggle_reading_width")
                        .semantics {
                            contentDescription = if (settings.readingWidthComfortable)
                                "Comfortable reading width active. Tap for wide layout."
                            else
                                "Wide reading layout active. Tap for comfortable width."
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewHeadline,
                        contentDescription = "Reading Width",
                        tint = if (settings.readingWidthComfortable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .testTag("btn_open_settings")
                        .semantics { contentDescription = "Accessibility and app settings" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
