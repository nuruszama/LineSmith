package com.nuruszama.linesmith.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class BrushType(val label: String) {
    PEN("Tapered Pen"),
    MARKER("Marker"),
    PENCIL("Pencil"),
    CALLIGRAPHY("Chisel Nib")
}

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f
)

data class VectorStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<StrokePoint>,
    val color: Color,
    val baseWidth: Float,
    val brushType: BrushType
)

data class DrawingLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isVisible: Boolean = true,
    val opacity: Float = 1f,
    val strokes: List<VectorStroke> = emptyList()
)