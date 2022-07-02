package com.augustbyrne.mdaspcompanion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.tooling.preview.Preview
import com.augustbyrne.mdaspcompanion.values.blue500
import kotlin.math.*

@Composable
fun ParametricEqUI(eq: AudioModel.ParametricEQ) {
    val n = 4

    MathGraph(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        xRange = 20f..20000f,
        yRange = -(10f.pow(18f / 20f))..10f.pow(18f / 20f),
        showAxis = false,
        showMarkers = true,
        designateMarkers = GraphMarkers(
            x = listOf(
                20f,
                50f,
                100f,
                200f,
                500f,
                1000f,
                2000f,
                5000f,
                10000f
            ), y = listOf(-18f, -12f, -6f, 0f, 6f, 12f, 18f)
        ),
        xLogBase = 2f,
        yLogBase = 10f,
        yLogMode = LogMode.Voltage,
        colorIntersect = true,
        audioFrequencyMode = true,
        equations =
        if (eq.hp && eq.lp) {
            arrayOf(
                { x: Float ->
                    (1f / sqrt(1f + (x / eq.lp_freq).pow(2 * n))) *
                            (1f / sqrt(1f + (eq.hp_freq / x).pow(2 * n)))
                }
            )
        } else if (eq.lp) {
            arrayOf({ x: Float ->
                1f / sqrt(1f + (x / eq.lp_freq).pow(2 * n))
            })
        } else if (eq.hp) {
            arrayOf({ x: Float ->
                1f / sqrt(1f + (eq.hp_freq / x).pow(2 * n))
            })
        } else {
            arrayOf({ 1f })
        }
    )
}



@Composable
fun MathGraph(
    modifier: Modifier = Modifier,
    xRange: ClosedFloatingPointRange<Float>,
    yRange: ClosedFloatingPointRange<Float>,
    showAxis: Boolean = true,
    showMarkers: Boolean = true,
    designateMarkers: GraphMarkers? = null,
    xLogBase: Float? = null,
    yLogBase: Float? = null,
    yLogMode: LogMode? = null,
    colorIntersect: Boolean = false,
    audioFrequencyMode: Boolean = false,
    vararg equations: (x: Float) -> Float
) {
    Canvas(modifier = modifier) {
        val y1: MutableList<Float> = mutableListOf()
        //graph paper
        drawRect(
            color = blue500,
            alpha = 0.5f,
            size = size.copy(width = size.width, height = 0.5f * size.width)
        )

        val origin = GraphOrigin(
            (size.width) * (-xRange.start) / (xRange.endInclusive - xRange.start),
            (0.5f * size.width) * ((yRange.start) / (yRange.endInclusive - yRange.start)) + (0.5f * size.width)
        )

        // TODO: make hatches along x and y

        val paths: MutableList<Path> = mutableListOf()
        for (eqn in equations) {

            val eqnPath = Path()
            var firstVal = true
            var x = xRange.start
            // TODO: Make dx be more accurate (for edge of function on screen)
            // screen increments dx and dy
            val dx: Float = (xRange.endInclusive - xRange.start) / size.width
            val dy: Float = 2f * (yRange.endInclusive - yRange.start) / size.width
            // this might have something to do with loc scale graphing issues
            while (x <= xRange.endInclusive) {
                val y: Float = if (yLogBase != null) {
                    when (yLogMode) {
                        LogMode.Voltage -> {
                            20f * log(eqn(x), yLogBase)
                        }
                        LogMode.Power -> {
                            10f * log(eqn(x), yLogBase)
                        }
                        else -> {
                            log(eqn(x), yLogBase)
                        }
                    }
                } else {
                    eqn(x)
                }
                val xLoc: Float = if (xLogBase != null) {
                    (log(x, xLogBase) - log(xRange.start, xLogBase)) / ((log(xRange.endInclusive, xLogBase) - log(xRange.start, xLogBase)) / size.width)
                } else {
                    // remove x starting offset, normalize, and then multiply by screen width
                    (x - xRange.start) / dx
                }

                // y location, normalize, then multiply by screen height, add starting y
                val yLoc: Float = (-y / dy) + origin.y

                if (yLoc in 0f..0.5f * size.width) {
                    if (firstVal) {
                        y1.add(yLoc)
                        eqnPath.moveTo(
                            x = xLoc,
                            y = yLoc
                        )
                        firstVal = false
                    } else {
                        eqnPath.lineTo(
                            x = xLoc,
                            y = yLoc
                        )
                    }
                }
                x += dx
            }
            if (!audioFrequencyMode) {
                drawPath(path = eqnPath, color = Color.Black, style = Stroke(width = 6f))
            } else {
                drawPath(path = eqnPath, color = Color.White, style = Stroke(width = 6f))
            }
            paths.add(eqnPath)
        }

        if (colorIntersect) {
            // Complete bounded shapes from paths
            for (index in 0..paths.lastIndex) {
                val rect = paths[index].getBounds()
                if (y1[index] >= (0.5f * size.width) - 5f) {
                    paths[index].lineTo(x = rect.bottomLeft.x, y = 0.5f * size.width)
                    paths[index].lineTo(size.width, 0.5f * size.width)
                    paths[index].close()
                } else {
                    paths[index].lineTo(x = rect.bottomRight.x, y = 0.5f * size.width)
                    paths[index].lineTo(0f, 0.5f * size.width)
                    paths[index].close()
                }
            }
            val pathDiff = paths[0]
            for (index in 1..paths.lastIndex) {
                pathDiff.op(pathDiff, paths[index], PathOperation.Intersect)
            }

            drawPath(path = pathDiff, color = Color.White.copy(alpha = 0.25f), style = Fill)
        }

        if (showAxis) {
            val xAxisLoc = origin.y.coerceIn(0f, 0.5f * size.width)
            val yAxisLoc = origin.x.coerceIn(0f, size.width)
            drawLine(
                color = if (xAxisLoc == 0f || xAxisLoc == size.width / 2f) Color.Red else Color.Black,
                strokeWidth = if (xAxisLoc == 0f || xAxisLoc == size.width / 2f) 8f else 4f,
                start = Offset(0f, xAxisLoc),
                end = Offset(
                    if (xAxisLoc == 0f || xAxisLoc == size.width / 2f) 20f else size.width,
                    xAxisLoc
                )
            )
            drawLine(
                color = if (yAxisLoc == 0f || yAxisLoc == size.width) Color.Red else Color.Black,
                strokeWidth = if (yAxisLoc == 0f || yAxisLoc == size.width) 8f else 4f,
                start = Offset(yAxisLoc, 0f),
                end = Offset(
                    yAxisLoc,
                    if (yAxisLoc == 0f || yAxisLoc == size.width) 20f else size.width / 2f
                )
            )
        }
        if (showMarkers) {
            val paint = Paint().asFrameworkPaint()
            paint.apply {
                isAntiAlias = true
                textSize = 24f
            }
            if (designateMarkers != null) {
                for (marker in designateMarkers.x) {
                    val xLoc = if (xLogBase != null) {
                        safeNumLog(
                            marker / xRange.start,
                            xRange.endInclusive / xRange.start
                        ) * size.width
                    } else {
                        marker * size.width / (xRange.endInclusive - xRange.start)
                    }
                    drawLine(
                        Color.Black,
                        Offset(x = xLoc, y = 0.5f * size.width),
                        Offset(x = xLoc, y = (0.5f * size.width) - 12f)
                    )
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(
                            if (abs(marker.toInt()) >= 1000) {
                                "${marker.toInt() / 1000}k"
                            } else {
                                "${marker.toInt()}"
                            },
                            xLoc.coerceIn(0f, size.width - 24f),
                            (0.5f * size.width) + 24f,
                            paint
                        )
                    }
                }
                for (marker in designateMarkers.y) {
                    val yLoc = if (yLogBase != null) {
                        (-marker) * 0.5f * size.width / (20f*(safeNumLog(
                            yRange.endInclusive,
                            yLogBase
                        ) - safeNumLog(yRange.start, yLogBase))) + origin.y
                        //safeNumLog(marker/yRange.start, yRange.endInclusive/yRange.start)*0.5f*size.width
                    } else {
                        marker * 0.5f * size.width / (yRange.endInclusive - yRange.start) + (0.5f * size.width)
                    }
                    drawLine(
                        Color.Black,
                        Offset(x = 0f, y = yLoc),
                        Offset(x = 10f, y = yLoc)
                    )
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(
                            if (abs(marker.toInt()) >= 1000) {
                                "${marker.toInt() / 1000}k"
                            } else {
                                "${marker.toInt()}"
                            },
                            12f,
                            yLoc.coerceIn(24f, 0.5f * size.width),
                            paint
                        )
                    }
                }
            } else {
                var x = xRange.start
                var xLoc = 0f
                while (x <= xRange.endInclusive) {
                    val dx = if (xLogBase != null) {
                        ((safeNumLog(x, xLogBase) - safeNumLog(
                            xRange.start,
                            xLogBase
                        )) / (safeNumLog(
                            xRange.endInclusive,
                            xLogBase
                        ) - safeNumLog(xRange.start, xLogBase))) * size.width
                    } else {
                        xLoc
                    }
                    drawLine(
                        Color.Black,
                        Offset(x = dx, y = size.width / 2f),
                        Offset(x = dx, y = (size.width / 2f) - 12f)
                    )
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(
                            if (abs(x) >= 1000) {
                                "${x / 1000}k"
                            } else {
                                "$x"
                            },
                            dx,
                            (size.width / 2f) + 24f,
                            paint
                        )
                    }
                    if (xLogBase != null) {
                        x *= xLogBase
                    } else {
                        x += (xRange.endInclusive - xRange.start) / 8f
                    }
                    xLoc += size.width / 8f
                }
                for (yNorm in 0..7) {
                    val y: Float = if (yLogBase != null) {
                        safeNumLog(
                            yRange.endInclusive.pow(yNorm / 7f) * yRange.start.pow(1f - (yNorm / 7f)),
                            yLogBase
                        )
                    } else {
                        yRange.start + (yNorm / 7f) * (yRange.endInclusive - yRange.start)
                    }
                    drawLine(
                        Color.Black,
                        Offset(x = 0f, y = (size.width / 2f) - ((yNorm / 7f) * size.width / 2f)),
                        Offset(x = 11f, y = (size.width / 2f) - ((yNorm / 7f) * size.width / 2f))
                    )
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(
                            if (abs(y) >= 1000f) {
                                "${y / 1000f}k"
                            } else {
                                "$y"
                            },
                            12f,
                            (size.width / 2f) - ((yNorm / 7f) * (size.width / 2f)),
                            paint
                        )
                    }
                }
            }
        }
    }
}

data class GraphOrigin(val x: Float, val y: Float)
data class GraphMarkers(val x: List<Float>, val y: List<Float>)

enum class LogMode {
    Power, Voltage
}

fun safeNumLog(num: Float, base: Float) =
    if (num < 0f) {
        -1f*log(abs(num), base)
    } else {
        log(num, base)
    }


@Composable
@Preview
fun EqUITest() {
    ParametricEqUI(AudioModel.ParametricEQ(lp_freq = 10000f, hp_freq = 1000f, hp = true, lp = true))
}