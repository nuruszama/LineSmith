package com.nuruszama.linesmith.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nuruszama.linesmith.model.BrushType
import com.nuruszama.linesmith.model.VectorStroke
import kotlin.math.hypot

object StrokeRenderer {

    fun DrawScope.drawVectorStroke(stroke: VectorStroke, layerOpacity: Float) {
        val pts = stroke.points
        if (pts.isEmpty()) return

        val effectiveColor = stroke.color.copy(
            alpha = (stroke.color.alpha * layerOpacity * brushAlpha(stroke.brushType)).coerceIn(0f, 1f)
        )
        val blendMode = if (stroke.brushType == BrushType.MARKER) BlendMode.Multiply else BlendMode.SrcOver

        if (pts.size == 1) {
            drawCircle(
                color = effectiveColor,
                radius = stroke.baseWidth / 2f,
                center = Offset(pts[0].x, pts[0].y),
                blendMode = blendMode
            )
            return
        }

        // Smooth quadratic Bezier path for standard strokes
        if (stroke.brushType == BrushType.MARKER || stroke.brushType == BrushType.PENCIL) {
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size - 1) {
                    val midX = (pts[i].x + pts[i + 1].x) / 2f
                    val midY = (pts[i].y + pts[i + 1].y) / 2f
                    quadraticTo(pts[i].x, pts[i].y, midX, midY)
                }
                lineTo(pts.last().x, pts.last().y)
            }
            drawPath(
                path = path,
                color = effectiveColor,
                style = Stroke(
                    width = stroke.baseWidth,
                    cap = if (stroke.brushType == BrushType.MARKER) StrokeCap.Square else StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = blendMode
            )
        } else {
            // Dynamic tapered width based on velocity/pressure for Pen & Calligraphy
            for (i in 0 until pts.size - 1) {
                val p1 = pts[i]
                val p2 = pts[i + 1]
                val dist = hypot(p2.x - p1.x, p2.y - p1.y)
                val progress = i.toFloat() / pts.size.coerceAtLeast(1)

                val taper = when (stroke.brushType) {
                    BrushType.PEN -> {
                        val velocityFactor = (1f - (dist / 80f)).coerceIn(0.35f, 1.15f)
                        val endTaper = if (progress > 0.85f) (1f - progress) / 0.15f else 1f
                        velocityFactor * endTaper.coerceAtLeast(0.2f)
                    }
                    BrushType.CALLIGRAPHY -> {
                        val dx = kotlin.math.abs(p2.x - p1.x)
                        val dy = kotlin.math.abs(p2.y - p1.y)
                        if (dx > dy) 0.45f else 1.25f
                    }
                    else -> 1f
                }

                drawLine(
                    color = effectiveColor,
                    start = Offset(p1.x, p1.y),
                    end = Offset(p2.x, p2.y),
                    strokeWidth = (stroke.baseWidth * p1.pressure * taper).coerceAtLeast(1.5f),
                    cap = StrokeCap.Round,
                    blendMode = blendMode
                )
            }
        }
    }

    private fun brushAlpha(type: BrushType): Float = when (type) {
        BrushType.MARKER -> 0.45f
        BrushType.PENCIL -> 0.80f
        else -> 1.0f
    }
}