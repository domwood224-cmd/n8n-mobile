package com.napcity.n8nmobile.editor

import androidx.compose.ui.geometry.Offset
import com.napcity.n8nmobile.data.N8nWorkflowDetail
import java.util.UUID

/**
 * Serializes the canvas to a valid n8n workflow payload for
 * POST/PATCH /api/v1/workflows.
 *
 * n8n connection format (keyed by source node NAME):
 *   "connections": {
 *     "Source Name": { "main": [ [ {"node": "Target", "type": "main", "index": 0} ] ] }
 *   }
 */
fun toN8nWorkflowPayload(
    name: String,
    nodes: List<EditorNode>,
    connections: List<EditorConnection>,
): Map<String, Any> {
    val nodeList = nodes.map { node ->
        mapOf(
            "id" to node.id,
            "name" to node.name,
            "type" to node.type,
            "typeVersion" to node.typeVersion,
            "position" to listOf(node.position.x.toInt(), node.position.y.toInt()),
            "parameters" to node.parameters.toMap(),
        )
    }

    val idToNode = nodes.associateBy { it.id }
    val connMap = mutableMapOf<String, Any>()
    connections.groupBy { it.fromNodeId }.forEach { (fromId, conns) ->
        val fromNode = idToNode[fromId] ?: return@forEach
        val targets = conns.mapNotNull { conn ->
            val toNode = idToNode[conn.toNodeId] ?: return@mapNotNull null
            mapOf("node" to toNode.name, "type" to "main", "index" to 0)
        }
        if (targets.isNotEmpty()) {
            // "main" = list of outputs; v1 uses a single output -> one nested list
            connMap[fromNode.name] = mapOf("main" to listOf(targets))
        }
    }

    return mapOf(
        "name" to name,
        "nodes" to nodeList,
        "connections" to connMap,
        "active" to false,
        "settings" to emptyMap<String, Any>(),
    )
}

/**
 * Loads an existing workflow detail into a fresh canvas state.
 * Parses nodes and the connections map defensively — n8n shapes vary
 * (Gson LinkedTreeMaps, int vs double positions).
 */
fun editorStateFromDetail(detail: N8nWorkflowDetail): EditorCanvasState {
    val state = EditorCanvasState()
    val nameToId = mutableMapOf<String, String>()

    detail.nodes.forEach { n ->
        val id = n.id.ifBlank { UUID.randomUUID().toString() }
        val x = (n.position.getOrNull(0) as? Number)?.toFloat() ?: 0f
        val y = (n.position.getOrNull(1) as? Number)?.toFloat() ?: 0f
        val node = EditorNode(
            id = id,
            name = n.name.ifBlank { "Node" },
            type = n.type,
            typeVersion = 1,
            position = Offset(x, y),
        )
        (n.parameters as? Map<*, *>)?.forEach { (k, v) ->
            if (k is String) node.parameters[k] = v ?: ""
        }
        state.nodes.add(node)
        nameToId[n.name] = id
    }

    detail.connections.forEach { (fromName, connValue) ->
        val fromId = nameToId[fromName] ?: return@forEach
        val main = ((connValue as? Map<*, *>)?.get("main") as? List<*>) ?: return@forEach
        main.forEach { output ->
            (output as? List<*>)?.forEach { target ->
                val targetName = (target as? Map<*, *>)?.get("node") as? String ?: return@forEach
                val toId = nameToId[targetName] ?: return@forEach
                state.connections.add(
                    EditorConnection(fromNodeId = fromId, toNodeId = toId)
                )
            }
        }
    }

    return state
}
