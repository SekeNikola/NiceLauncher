package com.nicelauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun PixelIcon(
    color: Color,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val rows = 7
        val cols = 11
        val cellW = size.width / cols
        val cellH = size.height / rows

        val rx = 5.2f
        val ry = 3.2f

        for (r in 0 until rows) {
            val y = r - 3f
            for (c in 0 until cols) {
                val x = c - 5f

                val normX = x / rx
                val normY = y / ry
                val dist = normX * normX + normY * normY

                if (dist <= 1.15f) {
                    // 1. Check if it's the outline of the sphere
                    val isOutline = dist >= 0.82f

                    // 2. Check if it's the equator
                    val isEquator = r == 3

                    // 3. Check if it's a longitude line
                    var isLongitude = false
                    // We calculate local horizontal radius of the slice at height y
                    val radFactor = 1f - (y * y) / (ry * ry + 0.1f)
                    val localRadiusX = rx * sqrt(if (radFactor < 0f) 0f else radFactor)

                    // 4 longitude lines around the globe (separated by 90 degrees)
                    for (k in 0..3) {
                        val angle = rotationAngle + k * (Math.PI.toFloat() / 2f)
                        val cosAngle = cos(angle)
                        // Only draw longitude lines on the front-facing side of the sphere
                        if (cosAngle > 0f) {
                            val projX = localRadiusX * sin(angle)
                            if (abs(x - projX) < 0.65f) {
                                isLongitude = true
                            }
                        }
                    }

                    if (isOutline || isEquator || isLongitude) {
                        drawRect(
                            color = color,
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW + 0.3f, cellH + 0.3f)
                        )
                    }
                }
            }
        }
    }
}
