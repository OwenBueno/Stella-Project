package com.stella.core.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stella.core.ui.theme.Background
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextSecondary

data class StellaNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun StellaNavigationDrawer(
    items: List<StellaNavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(
        modifier = modifier.width(280.dp),
        drawerContainerColor = Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 24.dp),
        ) {
            Text(
                text = "STELLA",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                ),
            )
            StellaLabel(
                text = "Navigation",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onItemClick(item.route) }
                            .background(
                                if (selected) {
                                    Primary.copy(alpha = 0.12f)
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                },
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) Primary else TextSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                TextSecondary
                            },
                        )
                    }
                }
            }
        }
    }
}
