package com.napcity.n8nmobile.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.napcity.n8nmobile.data.N8nWorkflowDetail
import com.napcity.n8nmobile.ui.N8nViewModel
import kotlinx.coroutines.launch

/**
 * Full-screen visual workflow editor.
 *
 * @param existingDetail null for a brand-new workflow, or the loaded detail
 *                       of a workflow being edited in the canvas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: N8nViewModel,
    existingDetail: N8nWorkflowDetail?,
    onBack: () -> Unit,
) {
    val state = rememberEditorCanvasState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var workflowName by remember { mutableStateOf(existingDetail?.name ?: "New workflow") }
    var showPalette by remember { mutableStateOf(false) }
    var configNode by remember { mutableStateOf<EditorNode?>(null) }
    var pendingAddPosition by remember { mutableStateOf<Offset?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var saving by remember { mutableStateOf(false) }

    // One-time init: load existing workflow, or seed a Start node for new ones.
    LaunchedEffect(Unit) {
        val detail = existingDetail
        if (detail != null) {
            val loaded = editorStateFromDetail(detail)
            state.nodes.addAll(loaded.nodes)
            state.connections.addAll(loaded.connections)
            if (loaded.nodes.isNotEmpty()) {
                val minX = loaded.nodes.minOf { it.position.x }
                val minY = loaded.nodes.minOf { it.position.y }
                state.offset = Offset(120f - minX, 200f - minY)
            }
        } else if (state.nodes.isEmpty()) {
            val startType = paletteNodeTypes.first { it.type == "n8n-nodes-base.start" }
            state.addNode(startType, Offset(0f, 0f))
            state.offset = Offset(160f, 320f)
        }
    }

    fun viewportCenterCanvas(): Offset {
        if (viewportSize == IntSize.Zero) return Offset(200f, 300f)
        return state.screenToCanvas(
            Offset(viewportSize.width / 2f, viewportSize.height / 2f)
        )
    }

    fun onPortTap(nodeId: String, port: String) {
        val sel = state.selectedPort
        when {
            sel == null -> state.selectedPort = nodeId to port
            sel.first == nodeId && sel.second == port -> state.selectedPort = null
            else -> {
                val (selId, selPort) = sel
                if (selId != nodeId &&
                    ((selPort == "output" && port == "input") ||
                            (selPort == "input" && port == "output"))
                ) {
                    val fromId = if (selPort == "output") selId else nodeId
                    val toId = if (selPort == "output") nodeId else selId
                    if (state.connect(fromId, toId)) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                state.selectedPort = null
            }
        }
    }

    fun save() {
        val name = workflowName.trim()
        if (name.isBlank() || saving) return
        scope.launch {
            saving = true
            val result = viewModel.saveCanvasWorkflow(
                name = name,
                nodes = state.nodes.toList(),
                connections = state.connections.toList(),
                existingId = existingDetail?.id,
            )
            saving = false
            if (result.isSuccess) onBack()
            // On failure the ViewModel already surfaced the error message.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    TextField(
                        value = workflowName,
                        onValueChange = { workflowName = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                actions = {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .size(24.dp),
                        )
                    } else {
                        TextButton(
                            onClick = { save() },
                            enabled = workflowName.isNotBlank(),
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pendingAddPosition = viewportCenterCanvas()
                    showPalette = true
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add node")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            NodeCanvas(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onViewportSizeChange = { viewportSize = it },
                onNodePortTap = ::onPortTap,
                onNodeTap = { node ->
                    state.selectedNodeId = node.id
                    state.selectedPort = null
                },
                onNodeConfigure = { configNode = it },
                onNodeDuplicate = { state.duplicateNode(it.id) },
                onNodeDelete = { state.removeNode(it.id) },
                onDoubleTapAdd = { canvasPos ->
                    pendingAddPosition = canvasPos
                    showPalette = true
                },
            )
        }
    }

    if (showPalette) {
        NodePaletteSheet(
            onDismiss = {
                showPalette = false
                pendingAddPosition = null
            },
            onSelect = { type ->
                val pos = pendingAddPosition ?: viewportCenterCanvas()
                state.addNode(type, pos)
                pendingAddPosition = null
                showPalette = false
            },
        )
    }

    configNode?.let { node ->
        NodeConfigSheet(
            node = node,
            state = state,
            onDismiss = { configNode = null },
        )
    }
}
