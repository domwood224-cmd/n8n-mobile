package com.napcity.n8nmobile.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import java.util.UUID

/**
 * A node on the canvas. Position is in canvas coordinates (not screen pixels).
 * Name/position are observable so the canvas re-renders on drag/rename.
 */
class EditorNode(
    val id: String = UUID.randomUUID().toString(),
    name: String,
    val type: String,
    val typeVersion: Int = 1,
    position: Offset = Offset.Zero,
) {
    var name by mutableStateOf(name)
    var position by mutableStateOf(position)
    val parameters = mutableStateMapOf<String, Any>()
}

data class EditorConnection(
    val id: String = UUID.randomUUID().toString(),
    val fromNodeId: String,
    val fromPort: String = "output",
    val toNodeId: String,
    val toPort: String = "input",
)

/**
 * Holds the full editor state: nodes, connections, viewport (pan/zoom),
 * selection. All mutations go through here so the canvas stays consistent.
 */
class EditorCanvasState {
    val nodes = mutableStateListOf<EditorNode>()
    val connections = mutableStateListOf<EditorConnection>()

    var offset by mutableStateOf(Offset.Zero)
    var scale by mutableStateOf(1f)

    /** Pair(nodeId, "input"|"output") of the currently tapped port, if any. */
    var selectedPort by mutableStateOf<Pair<String, String>?>(null)
    var selectedNodeId by mutableStateOf<String?>(null)

    // --- Coordinate math ---

    fun screenToCanvas(screen: Offset): Offset = (screen - offset) / scale
    fun canvasToScreen(canvas: Offset): Offset = canvas * scale + offset

    // --- Nodes ---

    fun nodeById(id: String): EditorNode? = nodes.find { it.id == id }

    private fun uniqueName(base: String): String {
        if (nodes.none { it.name == base }) return base
        var i = 2
        while (nodes.any { it.name == "$base $i" }) i++
        return "$base $i"
    }

    fun addNode(type: PaletteNodeType, position: Offset): EditorNode {
        val node = EditorNode(name = uniqueName(type.name), type = type.type, position = position)
        nodes.add(node)
        return node
    }

    fun removeNode(id: String) {
        nodes.removeAll { it.id == id }
        connections.removeAll { it.fromNodeId == id || it.toNodeId == id }
        if (selectedNodeId == id) selectedNodeId = null
        if (selectedPort?.first == id) selectedPort = null
    }

    fun duplicateNode(id: String): EditorNode? {
        val src = nodeById(id) ?: return null
        val copy = EditorNode(
            name = uniqueName(src.name),
            type = src.type,
            typeVersion = src.typeVersion,
            position = src.position + Offset(40f, 40f),
        )
        copy.parameters.putAll(src.parameters)
        nodes.add(copy)
        return copy
    }

    // --- Connections ---

    /** Returns false when the connection is invalid or already exists. */
    fun connect(fromId: String, toId: String): Boolean {
        if (fromId == toId) return false
        if (nodeById(fromId) == null || nodeById(toId) == null) return false
        if (connections.any { it.fromNodeId == fromId && it.toNodeId == toId }) return false
        connections.add(EditorConnection(fromNodeId = fromId, toNodeId = toId))
        return true
    }

    fun disconnect(connectionId: String) {
        connections.removeAll { it.id == connectionId }
    }

    // --- Viewport ---

    fun panBy(delta: Offset) {
        offset += delta
    }

    /**
     * Zoom keeping the gesture centroid fixed in canvas space,
     * so pinch-zoom feels anchored under the fingers.
     */
    fun zoomBy(zoomFactor: Float, centroid: Offset) {
        val newScale = (scale * zoomFactor).coerceIn(0.3f, 3f)
        if (newScale == scale) return
        val canvasPoint = (centroid - offset) / scale
        offset = centroid - canvasPoint * newScale
        scale = newScale
    }
}

@Composable
fun rememberEditorCanvasState(): EditorCanvasState = remember { EditorCanvasState() }
