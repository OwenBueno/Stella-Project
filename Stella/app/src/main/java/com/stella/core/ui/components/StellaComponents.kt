package com.stella.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stella.core.ui.theme.Border
import com.stella.core.ui.theme.Divider
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.SurfaceCard
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary

@Composable
fun StellaLabel(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            color = when {
                accent -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
    )
}

@Composable
fun StellaDisplayTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-1).sp,
        ),
    )
}

@Composable
fun StellaSectionHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StellaLabel(text = eyebrow, accent = true)
        StellaDisplayTitle(text = title)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Divider),
        )
    }
}

@Composable
fun StellaCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceCard)
            .border(1.dp, Border, RoundedCornerShape(4.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
fun StellaStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    StellaCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StellaLabel(text = label)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
fun StellaTopBar(
    statusText: String = "ONLINE",
    modifier: Modifier = Modifier,
    showMenu: Boolean = true,
    onMenuClick: () -> Unit = {},
    showBack: Boolean = false,
    onBack: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when {
                    showBack -> {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    showMenu -> {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open menu",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Column(modifier = Modifier.padding(start = if (showBack || showMenu) 0.dp else 16.dp)) {
                    Text(
                        text = "STELLA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                        ),
                    )
                    StellaLabel(text = statusText, accent = true)
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Primary),
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
        value?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
fun stellaTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = Primary,
    unfocusedBorderColor = Divider,
    focusedLabelColor = Primary,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Primary,
    focusedPlaceholderColor = TextSecondary,
    unfocusedPlaceholderColor = TextSecondary,
)
