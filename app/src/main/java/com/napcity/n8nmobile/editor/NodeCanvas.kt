package com.napcity.n8nmobile.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.floor

/**
 * Full-screen node canvas.
 *
 * Gesture layers (outermost first):
 * - background: pinch-zoom + pan via detectTransformGestures, tap/double-tap
 *   via detectTapGestures (tap empty space deselects, double-tap quick-adds)
 * - transformed content layer: grid dots, connection beziers, node cards —
 *   all positioned in canvas coordinates
 * - NodeCards on top handle their own drag / tap / long-press
 */
@Composable
fun NodeCanvas(
    state: EditorCanvasState,
    modifier: Modifier = Modifier,
    onViewportSizeChange: (IntSize) -> Unit = {},
    onNodePortTap: (nodeId: String, port: String) -> Unit = { _, _ -> },
    onNodeTap: (EditorNode) -> Unit = {},
    onNodeConfigure: (EditorNode) -> Unit = {},
    onNodeDuplicate: (EditorNode) -> Unit = {},
    onNodeDelete: (EditorNode) -> Unit = {},
    onDoubleTapAdd: (Offset) -> Unit = {},
) {
    val density = LocalDensity.current
    val cardWpx = with(density) { NodeCardWidth.toPx() }
    val cardHpx = with(density) { NodeCardHeight.toPx() }
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
    val connColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier = modifier) {
        val vw = constraints.maxWidth
        val vh = constraints.maxHeight
        LaunchedEffect(vw, vh) {
            if (vw > 0 && vh > 0) onViewportSizeChange(IntSize(vw, vh))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        state.panBy(pan)
                        state.zoomBy(zoom, centroid)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            state.selectedPort = null
                            state.selectedNodeId = null
                        },
                        onDoubleTap = { tapOffset ->
                            onDoubleTapAdd(state.screenToCanvas(tapOffset))
                        },
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = state.offset.x
                        translationY = state.offset.y
                        scaleX = state.scale
                        scaleY = state.scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                // Dotted grid, drawn only over the visible canvas region
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 56f
                    val topLeft = state.screenToCanvas(Offset.Zero)
                    val bottomRight = state.screenToCanvas(Offset(size.width, size.height))
                    var x = floor(topLeft.x / step) * step
                    while (x <= bottomRight.x) {
                        var y = floor(topLeft.y / step) * step
                        while (y <= bottomRight.y) {
                            drawCircle(gridColor, radius = 2.5f, center = Offset(x, y))
                            y += step
                        }
                        x += step
                    }
                }

                // Connection curves (cubic beziers, port to port)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    state.connections.forEach { conn ->
                        val from = state.nodeById(conn.fromNodeId) ?: return@forEach
                        val to = state.nodeById(conn.toNodeId) ?: return@forEach
                        val start = Offset(
                            from.position.x + cardWpx,
                            from.position.y + cardHpx / 2f,
                        )
                        val end = Offset(
                            to.position.x,
                            to.position.y + cardHpx / 2f,
                        )
                        val control = abs(end.x - start.x) * 0.5f + 40f
                        val path = Path().apply {
                            moveTo(start.x, start.y)
                            cubicTo(
                                start.x + control, start.y,
                                end.x - control, end.y,
                                end.x, end.y,
                            )
                        }
                        drawPath(
                            path = path,
                            color = connColor,
                            style = Stroke(
                                width = 3.5f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            ),
                        )
                    }
                }

                // Node cards
                state.nodes.forEach { node ->
                    val selPort = state.selectedPort
                    NodeCard(
                        node = node,
                        scale = state.scale,
                        isSelected = state.selectedNodeId == node.id,
                        inputSelected = selPort?.first == node.id && selPort.second == "input",
                        outputSelected = selPort?.first == node.id && selPort.second == "output",
                        categoryColor = categoryColorFor(node.type),
                        onMove = { delta -> node.position += delta },
                        onTap = { onNodeTap(node) },
                        onPortTap = { port -> onNodePortTap(node.id, port) },
                        onConfigure = { onNodeConfigure(node) },
                        onDuplicate = { onNodeDuplicate(node) },
                        onDelete = { onNodeDelete(node) },
                    )
                }
            }
        }
    }
}
