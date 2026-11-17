package com.nuruszama.linesmith.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.nuruszama.linesmith.engine.StrokeRenderer.drawVectorStroke
import com.nuruszama.linesmith.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen() {
    var layers by remember {
        mutableStateOf(listOf(DrawingLayer(name = "Layer 1")))
    }
    var activeLayerIndex by remember { mutableIntStateOf(0) }
    var activePoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }

    var selectedBrush by remember { mutableStateOf(BrushType.PEN) }
    var selectedColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }
    var brushSize by remember { mutableFloatStateOf(10f) }
    var showLayersSheet by remember { mutableStateOf(false) }

    val palette = listOf(
        Color(0xFF1E1E1E),
        Color(0xFFE53935),
        Color(0xFF1E88E5),
        Color(0xFF43A047),
        Color(0xFFFB8C00),
        Color(0xFF8E24AA)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LineSmith") },
                actions = {
                    IconButton(
                        onClick = {
                            val current = layers.getOrNull(activeLayerIndex) ?: return@IconButton
                            if (current.strokes.isNotEmpty()) {
                                layers = layers.toMutableList().apply {
                                    this[activeLayerIndex] = current.copy(strokes = current.strokes.dropLast(1))
                                }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { showLayersSheet = true }) {
                        Icon(Icons.Default.Layers, contentDescription = "Layers")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Brush Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BrushType.entries.forEach { brush ->
                            FilterChip(
                                selected = selectedBrush == brush,
                                onClick = { selectedBrush = brush },
                                label = { Text(brush.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Color Palette + Size Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        palette.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColor == color) 3.dp else 1.dp,
                                        color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = brushSize,
                            onValueChange = { brushSize = it },
                            valueRange = 2f..48f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activeLayerIndex, selectedBrush, selectedColor, brushSize) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                activePoints = listOf(StrokePoint(offset.x, offset.y, 1f))
                            },
                            onDrag = { change, _ ->
                                val pos = change.position
                                val pressure = change.pressure.takeIf { it > 0f } ?: 1f
                                activePoints = activePoints + StrokePoint(pos.x, pos.y, pressure)
                            },
                            onDragEnd = {
                                val target = layers.getOrNull(activeLayerIndex)
                                if (target != null && target.isVisible && activePoints.isNotEmpty()) {
                                    val newStroke = VectorStroke(
                                        points = activePoints,
                                        color = selectedColor,
                                        baseWidth = brushSize,
                                        brushType = selectedBrush
                                    )
                                    layers = layers.toMutableList().apply {
                                        this[activeLayerIndex] = target.copy(strokes = target.strokes + newStroke)
                                    }
                                }
                                activePoints = emptyList()
                            }
                        )
                    }
            ) {
                layers.forEachIndexed { idx, layer ->
                    if (layer.isVisible) {
                        layer.strokes.forEach { stroke ->
                            drawVectorStroke(stroke, layer.opacity)
                        }
                        if (idx == activeLayerIndex && activePoints.isNotEmpty()) {
                            drawVectorStroke(
                                VectorStroke(
                                    points = activePoints,
                                    color = selectedColor,
                                    baseWidth = brushSize,
                                    brushType = selectedBrush
                                ),
                                layer.opacity
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLayersSheet) {
        ModalBottomSheet(onDismissRequest = { showLayersSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Layers", style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = {
                            val newLayer = DrawingLayer(name = "Layer ${layers.size + 1}")
                            layers = layers + newLayer
                            activeLayerIndex = layers.lastIndex
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Layer")
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn {
                    itemsIndexed(layers) { index, layer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (index == activeLayerIndex) MaterialTheme.colorScheme.secondaryContainer
                                    else Color.Transparent
                                )
                                .clickable { activeLayerIndex = index }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    layers = layers.toMutableList().apply {
                                        this[index] = layer.copy(isVisible = !layer.isVisible)
                                    }
                                }
                            ) {
                                Icon(
                                    if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility"
                                )
                            }
                            Text(layer.name, modifier = Modifier.weight(1f))
                            if (layers.size > 1) {
                                IconButton(
                                    onClick = {
                                        layers = layers.filterIndexed { i, _ -> i != index }
                                        activeLayerIndex = activeLayerIndex.coerceAtMost(layers.lastIndex)
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Layer")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}