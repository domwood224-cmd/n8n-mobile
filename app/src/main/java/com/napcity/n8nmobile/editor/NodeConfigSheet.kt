package com.napcity.n8nmobile.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet for editing a node: rename, generic key/value parameters,
 * and connection management (disconnect incoming/outgoing).
 * Edits are staged locally and written back on Done/dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeConfigSheet(
    node: EditorNode,
    state: EditorCanvasState,
    onDismiss: () -> Unit,
) {
    var name by remember(node.id) { mutableStateOf(node.name) }
    val params = remember(node.id) {
        mutableStateListOf<Pair<String, String>>().apply {
            node.parameters.forEach { (k, v) -> add(k to v.toString()) }
        }
    }

    fun writeBack() {
        if (name.isNotBlank()) node.name = name
        node.parameters.clear()
        params.forEach { (k, v) ->
            if (k.isNotBlank()) node.parameters[k] = v
        }
    }

    val outgoing = state.connections.filter { it.fromNodeId == node.id }
    val incoming = state.connections.filter { it.toNodeId == node.id }

    ModalBottomSheet(onDismissRequest = { writeBack(); onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Configure node",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                friendlyName(node.type),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Node name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text(
                "Parameters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            params.forEachIndexed { index, (k, v) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = k,
                        onValueChange = { params[index] = it to v },
                        label = { Text("Key") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(
                        value = v,
                        onValueChange = { params[index] = k to it },
                        label = { Text("Value") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    IconButton(onClick = { params.removeAt(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove parameter")
                    }
                }
            }
            TextButton(onClick = { params.add("" to "") }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                Text("Add parameter")
            }

            if (outgoing.isNotEmpty() || incoming.isNotEmpty()) {
                Text(
                    "Connections",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                outgoing.forEach { conn ->
                    val targetName = state.nodeById(conn.toNodeId)?.name ?: "?"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("→ $targetName", modifier = Modifier.weight(1f))
                        IconButton(onClick = { state.disconnect(conn.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Disconnect")
                        }
                    }
                }
                incoming.forEach { conn ->
                    val sourceName = state.nodeById(conn.fromNodeId)?.name ?: "?"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("← $sourceName", modifier = Modifier.weight(1f))
                        IconButton(onClick = { state.disconnect(conn.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Disconnect")
                        }
                    }
                }
            }

            Button(
                onClick = { writeBack(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Done")
            }
        }
    }
}
