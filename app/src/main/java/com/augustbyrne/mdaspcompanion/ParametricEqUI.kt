package com.augustbyrne.mdaspcompanion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augustbyrne.mdaspcompanion.values.blue100
import com.augustbyrne.mdaspcompanion.values.blue500
import kotlin.math.*

@Composable
fun ParametricEqUI(eq: AudioModel.ParametricEQ) {
    val n = 4

    MathGraph(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(
                ratio = 2f,
                matchHeightConstraintsFirst = true
            ),
        xRange = 20f..20000f,
        yRange = -(10f.pow(18f / 20f))..10f.pow(18f / 20f),
        showAxis = false,
        showMarkers = true,
        designateMarkers = GraphMarkers(
            xValues = listOf(
                20f,
                50f,
                100f,
                200f,
                500f,
                1000f,
                2000f,
                5000f,
                10000f
            ),
            xAxisMarker = "Frequency (Hz)",
            yValues = listOf(-18f, -12f, -6f, 0f, 6f, 12f, 18f),
            yAxisMarker = "Magnitude (dB)"
        ),
        xLogBase = 2f,
        yLogBase = 10f,
        yLogMode = LogMode.Power,
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
    val textColor = MaterialTheme.colorScheme.onSurface
    Row(modifier) {
        Canvas(Modifier.width(32.dp).fillMaxHeight()) {
            if (showMarkers) {
                val realSize = size.copy(height = size.height - 24.dp.toPx())
                val paint = Paint().asFrameworkPaint()
                paint.apply {
                    isAntiAlias = true
                    textSize = 12.sp.toPx()
                    color = textColor.toArgb()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                if (designateMarkers != null) {
                    for (marker in designateMarkers.yValues) {
                        val yLoc = if (yLogBase != null) {
                            (-marker) * realSize.height / (20f * (safeNumLog(
                                yRange.endInclusive,
                                yLogBase
                            ) - safeNumLog(yRange.start, yLogBase))) + (realSize.height * (yRange.start / (yRange.endInclusive - yRange.start)) + realSize.height)
                            //safeNumLog(marker/yRange.start, yRange.endInclusive/yRange.start)*0.5f*size.width
                        } else {
                            marker * realSize.height / (yRange.endInclusive - yRange.start) + realSize.height
                        }
                        drawIntoCanvas {
                            it.nativeCanvas.drawText(
                                if (abs(marker.toInt()) >= 1000) {
                                    "${marker.toInt() / 1000}k"
                                } else {
                                    "${marker.toInt()}"
                                },
                                realSize.width - 4.dp.toPx(),
                                (yLoc+12f).coerceIn(26f, realSize.height),
                                paint
                            )
                        }
                    }
                    rotate(270f) {
                        drawIntoCanvas {
                            it.nativeCanvas.drawText(
                                designateMarkers.yAxisMarker,
                                0f,
                                realSize.height/2f,
                                paint.apply { textAlign = android.graphics.Paint.Align.CENTER }
                            )
                        }
                    }
                } else {

                }
            }
        }
        Column(Modifier.fillMaxWidth().wrapContentHeight()) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f, true)) {
                val startEndCoordinates = GraphPair()
                //graph paper
                drawRect(
                    color = blue500,
                    size = size
                )

                val origin = GraphCoordinate(
                    size.width * (-xRange.start) / (xRange.endInclusive - xRange.start),
                    size.height * ((yRange.start) / (yRange.endInclusive - yRange.start)) + size.height
                )

                // TODO: make hatches along x and y

                val paths: MutableList<Path> = mutableListOf()
                val coords: MutableList<GraphPair> = mutableListOf()
                for (eqn in equations) {
                    val linearXRange = if (xLogBase != null) safeNumLog(xRange.start, xLogBase)..safeNumLog(xRange.endInclusive,xLogBase) else xRange
                    val eqnPath = Path()
                    var firstVal = true
                    var linX = linearXRange.start
                    // TODO: Make dx be more accurate (for edge of function on screen)
                    // screen increments dx and dy
                    val dx: Float = (linearXRange.endInclusive - linearXRange.start) / size.width
                    val dy: Float = (yRange.endInclusive - yRange.start) / size.height
                    // this might have something to do with loc scale graphing issues
                    while (linX <= linearXRange.endInclusive) {
                        val x = xLogBase?.pow(linX) ?: linX
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

                        // remove x starting offset, normalize, and then multiply by screen width
                        val xLoc: Float = (linX - linearXRange.start) / dx
                        // y location, normalize, then multiply by screen height, add starting y
                        val yLoc: Float = (-y / dy) + origin.y

                        if (yLoc in 0f..size.height) {
                            if (firstVal) {
                                firstVal = false
                                startEndCoordinates.start = GraphCoordinate(x = xLoc, y = yLoc)
                                eqnPath.moveTo(
                                    x = xLoc,
                                    y = yLoc
                                )
                            } else {
                                startEndCoordinates.end = GraphCoordinate(x = xLoc, y = yLoc)
                                eqnPath.lineTo(
                                    x = xLoc,
                                    y = yLoc
                                )
                            }
                        }
                        linX += dx
                    }
                    if (!audioFrequencyMode) {
                        drawPath(path = eqnPath, color = Color.Black, style = Stroke(width = 6f))
                    } else {
                        drawPath(path = eqnPath, color = Color.White, style = Stroke(width = 6f))
                    }
                    paths.add(eqnPath)
                    coords.add(startEndCoordinates)
                }

                if (colorIntersect) {
                    // Complete bounded shapes from paths
                    if (coords.size != 0) {
                        for (index in 0..paths.lastIndex) {
                            paths[index].lineTo(x = coords[index].end.x, y = size.height)
                            paths[index].lineTo(x = coords[index].start.x, y = size.height)
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
                    val xAxisLoc = origin.y.coerceIn(0f, size.height)
                    val yAxisLoc = origin.x.coerceIn(0f, size.width)
                    drawLine(
                        color = if (xAxisLoc == 0f || xAxisLoc == size.height) Color.Red else Color.Black,
                        strokeWidth = if (xAxisLoc == 0f || xAxisLoc == size.height) 8f else 4f,
                        start = Offset(0f, xAxisLoc),
                        end = Offset(
                            if (xAxisLoc == 0f || xAxisLoc == size.height) 20f else size.width,
                            xAxisLoc
                        )
                    )
                    drawLine(
                        color = if (yAxisLoc == 0f || yAxisLoc == size.width) Color.Red else Color.Black,
                        strokeWidth = if (yAxisLoc == 0f || yAxisLoc == size.width) 8f else 4f,
                        start = Offset(yAxisLoc, 0f),
                        end = Offset(
                            yAxisLoc,
                            if (yAxisLoc == 0f || yAxisLoc == size.width) 20f else size.height
                        )
                    )
                }
                if (showMarkers) {
                    val paint = Paint().asFrameworkPaint()
                    paint.apply {
                        isAntiAlias = true
                        textSize = 12.sp.toPx()
                        color = textColor.toArgb()
                    }
                    if (designateMarkers != null) {
                        for (marker in designateMarkers.xValues) {
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
                                Offset(x = xLoc, y = size.height),
                                Offset(x = xLoc, y = size.height - 12f)
                            )
/*                        drawIntoCanvas {
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
                        }*/
                        }
                        for (marker in designateMarkers.yValues) {
                            val yLoc = if (yLogBase != null) {
                                (-marker) * size.height / (20f * (safeNumLog(
                                    yRange.endInclusive,
                                    yLogBase
                                ) - safeNumLog(yRange.start, yLogBase))) + origin.y
                                //safeNumLog(marker/yRange.start, yRange.endInclusive/yRange.start)*0.5f*size.width
                            } else {
                                marker * size.height / (yRange.endInclusive - yRange.start) + size.height
                            }
                            drawLine(
                                Color.Black,
                                Offset(x = 0f, y = yLoc),
                                Offset(x = 10f, y = yLoc)
                            )
/*                        drawIntoCanvas {
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
                        }*/
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
                                Offset(x = dx, y = size.height),
                                Offset(x = dx, y = size.height - 12f)
                            )
/*                        drawIntoCanvas {
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
                        }*/
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
                                Offset(
                                    x = 0f,
                                    y = size.height - ((yNorm / 7f) * size.height)
                                ),
                                Offset(
                                    x = 11f,
                                    y = size.height - ((yNorm / 7f) * size.height)
                                )
                            )
/*                        drawIntoCanvas {
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
                        }*/
                        }
                    }
                }
            }
            Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                if (showMarkers) {
/*                    drawRect(
                        color = Color.White,
                        size = size
                    )*/
                    val paint = Paint().asFrameworkPaint()
                    paint.apply {
                        isAntiAlias = true
                        textSize = 12.sp.toPx()
                        color = textColor.toArgb()
                    }
                    if (designateMarkers != null) {
                        for (marker in designateMarkers.xValues) {
                            val xLoc = if (xLogBase != null) {
                                safeNumLog(
                                    marker / xRange.start,
                                    xRange.endInclusive / xRange.start
                                ) * size.width
                            } else {
                                marker * size.width / (xRange.endInclusive - xRange.start)
                            }
                            drawIntoCanvas {
                                it.nativeCanvas.drawText(
                                    if (abs(marker.toInt()) >= 1000) {
                                        "${marker.toInt() / 1000}k"
                                    } else {
                                        "${marker.toInt()}"
                                    },
                                    (xLoc-12f).coerceIn(0f, size.width - 32f),
                                    12.dp.toPx(),
                                    paint
                                )
                            }
                        }
                        drawIntoCanvas {
                            it.nativeCanvas.drawText(
                                designateMarkers.xAxisMarker,
                                size.width / 2f,
                                size.height,
                                paint
                            )
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
                            drawIntoCanvas {
                                it.nativeCanvas.drawText(
                                    if (abs(x) >= 1000) {
                                        "${x / 1000}k"
                                    } else {
                                        "$x"
                                    },
                                    (dx-12f).coerceIn(0f, size.width),
                                    28f,
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
                    }
                }
            }
        }
    }
}

data class GraphCoordinate(val x: Float, val y: Float)
data class GraphPair(var start: GraphCoordinate = GraphCoordinate(0f,0f), var end: GraphCoordinate = GraphCoordinate(0f,0f))
data class GraphMarkers(val xValues: List<Float>, val xAxisMarker: String, val yValues: List<Float>, val yAxisMarker: String)

enum class LogMode {
    Power, Voltage
}

fun safeNumLog(num: Float, base: Float) =
    if (num < 0f) {
        -1f*log(abs(num), base)
    } else {
        log(num, base)
    }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun EqUITest() {
    var equalizer by remember {
        mutableStateOf(
            AudioModel.ParametricEQ(
                lp_freq = 10000f,
                hp_freq = 100f,
                hp = true,
                lp = true
            )
        )
    }
    Column(Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.5f))) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.9f
                )
            )
        ) {
            Column(Modifier.padding(8.dp)) {
                Text("Frequency Response Graph")
                Spacer(Modifier.height(8.dp))
                ParametricEqUI(equalizer)
            }
        }
        LabeledSlider(
            labels = listOf(
                "10Hz",
                "20",
                "50",
                "100",
                "200",
                "500",
                "1k",
                "2k",
                "5k",
                "10k",
                "20kHz"
            ),
            enabled = equalizer.lp,
            value = if (equalizer.lp) {
                log2(equalizer.lp_freq)
            } else {
                14.2877f
            },
            onValueChange = { logVal ->
                if (equalizer.lp) {
                    equalizer = equalizer.copy(lp_freq = 2f.pow(logVal))
                }
            },
            onValueChangeFinished = {},
            valueRange = 4.3219f..14.2877f  //aprox 20 to 20k in log2 (octave) scale
        )
        Text("LP Val: ${equalizer.lp_freq} Hz")
    }
}