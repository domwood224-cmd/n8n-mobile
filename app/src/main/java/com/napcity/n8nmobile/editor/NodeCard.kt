package com.napcity.n8nmobile.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

val NodeCardWidth = 172.dp
val NodeCardHeight = 84.dp
private val PortTouchSize = 28.dp

/**
 * A draggable node card. Drag delta is divided by the canvas scale so the
 * card tracks the finger 1:1 in canvas coordinates.
 */
@Composable
fun NodeCard(
    node: EditorNode,
    scale: Float,
    isSelected: Boolean,
    inputSelected: Boolean,
    outputSelected: Boolean,
    categoryColor: Color,
    onMove: (Offset) -> Unit,
    onTap: () -> Unit,
    onPortTap: (port: String) -> Unit,
    onConfigure: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.offset {
            IntOffset(node.position.x.roundToInt(), node.position.y.roundToInt())
        }
    ) {
        Box(
            modifier = Modifier
                .size(NodeCardWidth, NodeCardHeight)
                .pointerInput(scale) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onMove(dragAmount / scale)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { showMenu = true },
                    )
                }
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(categoryColor)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            node.name,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        friendlyName(node.type),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }

            // Input port (left edge, vertically centered)
            PortDot(
                modifier = Modifier.align(Alignment.CenterStart).offset(x = -PortTouchSize / 2),
                selected = inputSelected,
                onTap = { onPortTap("input") },
            )
            // Output port (right edge, vertically centered)
            PortDot(
                modifier = Modifier.align(Alignment.CenterEnd).offset(x = PortTouchSize / 2),
                selected = outputSelected,
                onTap = { onPortTap("output") },
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Configure") },
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                onClick = { showMenu = false; onConfigure() },
            )
            DropdownMenuItem(
                text = { Text("Duplicate") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = { showMenu = false; onDuplicate() },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = { showMenu = false; onDelete() },
            )
        }
    }
}

/** 28dp touch target with a smaller visible dot; ring highlight when selected. */
@Composable
private fun PortDot(
    modifier: Modifier,
    selected: Boolean,
    onTap: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(PortTouchSize)
            .clip(CircleShape)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        val ringColor = MaterialTheme.colorScheme.primary
        Canvas(modifier = Modifier.size(18.dp)) {
            val r = size.minDimension / 2f
            if (selected) drawCircle(ringColor, r)
            drawCircle(Color(0xFF9E9E9E), if (selected) r - 4f else r * 0.55f)
        }
    }
}
