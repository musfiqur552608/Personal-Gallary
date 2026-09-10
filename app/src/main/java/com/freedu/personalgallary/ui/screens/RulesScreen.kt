package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.data.local.RuleEntity
import com.freedu.personalgallary.data.model.RuleTypes

/** Set-and-forget tidiness rules, evaluated on every library refresh. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    rules: Map<String, RuleEntity>,
    onSet: (type: String, enabled: Boolean, days: Int) -> Unit
) {
    fun rule(type: String) = rules[type]

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Auto-rules", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Run quietly on every re-scan. Anything trashed stays restorable for 30 days.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(18.dp)) {
            Column {
                RuleRow(
                    title = "Auto-trash old screenshots",
                    desc = "Screenshots older than the threshold go to trash",
                    enabled = rule(RuleTypes.TRASH_SCREENSHOTS)?.enabled == true,
                    onToggle = { onSet(RuleTypes.TRASH_SCREENSHOTS, it, rule(RuleTypes.TRASH_SCREENSHOTS)?.daysParam ?: 90) },
                    days = rule(RuleTypes.TRASH_SCREENSHOTS)?.daysParam ?: 90,
                    onDays = { onSet(RuleTypes.TRASH_SCREENSHOTS, true, it) },
                    showDays = true
                )
                RuleRow(
                    title = "Auto-lock new WhatsApp videos",
                    desc = "Fresh arrivals from WhatsApp go straight to the vault",
                    enabled = rule(RuleTypes.LOCK_WHATSAPP)?.enabled == true,
                    onToggle = { onSet(RuleTypes.LOCK_WHATSAPP, it, 90) },
                    days = 90,
                    onDays = {},
                    showDays = false
                )
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleRow(
    title: String,
    desc: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    days: Int,
    onDays: (Int) -> Unit,
    showDays: Boolean
) {
    Column(Modifier.fillMaxWidth().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        if (showDays && enabled) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 90, 180).forEach { d ->
                    FilterChip(
                        selected = days == d,
                        onClick = { onDays(d) },
                        label = { Text("$d days") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}
