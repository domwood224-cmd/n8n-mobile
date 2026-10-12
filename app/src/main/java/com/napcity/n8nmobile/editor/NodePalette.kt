package com.napcity.n8nmobile.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class NodeCategory(val label: String, val color: Color) {
    TRIGGER("Triggers", Color(0xFF4CAF50)),
    ACTION("Actions", Color(0xFF2196F3)),
    LOGIC("Logic", Color(0xFFFF9800)),
    COMM("Communication", Color(0xFF9C27B0)),
    DATA("Data", Color(0xFF009688)),
    AI("AI", Color(0xFFE91E63)),
}

data class PaletteNodeType(
    val type: String,
    val name: String,
    val category: NodeCategory,
)

/** Common n8n node types with their real API type strings. */
val paletteNodeTypes = listOf(
    // Triggers
    PaletteNodeType("n8n-nodes-base.start", "Start", NodeCategory.TRIGGER),
    PaletteNodeType("n8n-nodes-base.manualTrigger", "Manual Trigger", NodeCategory.TRIGGER),
    PaletteNodeType("n8n-nodes-base.webhook", "Webhook", NodeCategory.TRIGGER),
    PaletteNodeType("n8n-nodes-base.scheduleTrigger", "Schedule Trigger", NodeCategory.TRIGGER),
    // Actions
    PaletteNodeType("n8n-nodes-base.httpRequest", "HTTP Request", NodeCategory.ACTION),
    PaletteNodeType("n8n-nodes-base.set", "Set", NodeCategory.ACTION),
    PaletteNodeType("n8n-nodes-base.code", "Code", NodeCategory.ACTION),
    // Logic
    PaletteNodeType("n8n-nodes-base.if", "IF", NodeCategory.LOGIC),
    PaletteNodeType("n8n-nodes-base.switch", "Switch", NodeCategory.LOGIC),
    PaletteNodeType("n8n-nodes-base.merge", "Merge", NodeCategory.LOGIC),
    PaletteNodeType("n8n-nodes-base.noOp", "No Operation", NodeCategory.LOGIC),
    // Communication
    PaletteNodeType("n8n-nodes-base.telegram", "Telegram", NodeCategory.COMM),
    PaletteNodeType("n8n-nodes-base.gmail", "Gmail", NodeCategory.COMM),
    PaletteNodeType("n8n-nodes-base.slack", "Slack", NodeCategory.COMM),
    PaletteNodeType("n8n-nodes-base.discord", "Discord", NodeCategory.COMM),
    // Data
    PaletteNodeType("n8n-nodes-base.googleSheets", "Google Sheets", NodeCategory.DATA),
    PaletteNodeType("n8n-nodes-base.airtable", "Airtable", NodeCategory.DATA),
    PaletteNodeType("n8n-nodes-base.postgres", "Postgres", NodeCategory.DATA),
    PaletteNodeType("n8n-nodes-base.mySql", "MySQL", NodeCategory.DATA),
    PaletteNodeType("n8n-nodes-base.mongoDb", "MongoDB", NodeCategory.DATA),
    PaletteNodeType("n8n-nodes-base.redis", "Redis", NodeCategory.DATA),
    // AI
    PaletteNodeType("@n8n/n8n-nodes-langchain.openAi", "OpenAI", NodeCategory.AI),
)

fun categoryColorFor(type: String): Color =
    paletteNodeTypes.find { it.type == type }?.category?.color ?: Color(0xFF607D8B)

fun friendlyName(type: String): String =
    paletteNodeTypes.find { it.type == type }?.name ?: type.substringAfterLast(".")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodePaletteSheet(
    onDismiss: () -> Unit,
    onSelect: (PaletteNodeType) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) paletteNodeTypes
        else paletteNodeTypes.filter {
            it.name.contains(query, ignoreCase = true) || it.type.contains(query, ignoreCase = true)
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Add node", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search nodes") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(filtered, key = { it.type }) { nt ->
                    ListItem(
                        headlineContent = { Text(nt.name) },
                        supportingContent = { Text(nt.category.label) },
                        leadingContent = {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(nt.category.color)
                            }
                        },
                        modifier = Modifier.clickable { onSelect(nt) },
                    )
                }
            }
        }
    }
}
