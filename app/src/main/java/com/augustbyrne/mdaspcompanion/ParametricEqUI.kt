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
import androidx.compose.ui.tooling.preview.Preview
import com.augustbyrne.mdaspcompanion.values.blue200
import timber.log.Timber
import kotlin.math.*

@Composable
fun ParametricEqUI(eq: AudioModel.ParametricEQ) {
    val n = 4
    val xRange: IntRange = IntRange(20, 20000)

    MathGraph(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        xRange = 1f..4f,//-4f..4f,
        yRange = 0f..1f,//-2f..4f,//10f.pow(-24/40)..1f,
        //xLogBase = 10f,
        //yLogBase = 10f,
        xLogMode = LogMode.Octave,
        yLogMode = LogMode.Voltage,
        colorIntersect = true,
        equations = arrayOf(
            { x: Float ->
                //1/sqrt(1+(x-10f.pow(eq.lp_freq/20)).pow(2*n))
                log10(x)

                //1/sqrt(1+(x-20*log10(eq.lp_freq)).pow(2*n))


                //-(x/2)+1
                //20*log10(1/ sqrt(1+10f.pow((x-eq.lp_freq)/40).pow(2*n)))
            },
            { x: Float ->
                -(x/6)+1
            }
        )
    )
}



@Composable
fun MathGraph(
    modifier: Modifier = Modifier,
    xRange: ClosedFloatingPointRange<Float>,
    yRange: ClosedFloatingPointRange<Float>,
    showAxis: Boolean = true,
    xLogBase: Float? = null,
    yLogBase: Float? = null,
    xLogMode: LogMode? = null,
    yLogMode: LogMode? = null,
    colorIntersect: Boolean = false,
    vararg equations: (x: Float) -> Float
) {
    //TODO: Finish creating xLog Scale Graphing: Making dx exponentially increase
    Canvas(modifier = modifier) {
        val y1: MutableList<Float> = mutableListOf()
        //graph paper
        drawRect(
            color = blue200,
            alpha = 0.5f,
            size = size.copy(width = size.width, height = size.width / 2)
        )

        val origin = GraphOrigin(
            (size.width) * (-xRange.start) / (xRange.endInclusive - xRange.start),
            (0.5f * size.width) * ((yRange.start) / (yRange.endInclusive - yRange.start)) + (0.5f * size.width)
        )

        if (showAxis) {
            val xAxisLoc = origin.y.coerceIn(0f, size.width / 2f)
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
        // TODO: make hatches along x and y

        val paths: MutableList<Path> = mutableListOf()
        for (eqn in equations) {

            val eqnPath = Path()
            var firstVal = true
            Timber.d("here: 1")
            var x = if (xLogBase != null) {
                if (xLogMode == LogMode.Octave) {
                    //log(xRange.start, xLogBase)
                    //xLogBase.pow(xRange.start)
                    log(xRange.start, xLogBase)
                } else {
                    log(xRange.start, xLogBase)
                }
            } else {
                xRange.start
            }
            // screen increments dx and dy
            val dx: Float = (xRange.endInclusive - xRange.start) / size.width
            val dy: Float = 2f * (yRange.endInclusive - yRange.start) / size.width
            Timber.d("here: 2")
            // this might have something to do with loc scale graphing issues
            val endX = if (xLogBase != null) {
                log(xRange.endInclusive, xLogBase)
            } else {
                xRange.endInclusive
            }
            while (x <= endX) {

                val y: Float = if (yLogBase != null) {
                    when (yLogMode) {
                        LogMode.Voltage -> {
                            20 * log(eqn(x), yLogBase)
                        }
                        LogMode.Power -> {
                            10 * log(eqn(x), yLogBase)
                        }
                        else -> {
                            log(eqn(x), yLogBase)
                        }
                    }
                } else {
                    eqn(x)
                }
                Timber.d("here: 3")
                // remove x starting offset, normalize, and then multiply by screen width
                val xLoc: Float = if (xLogBase != null) {
                    xLogBase.pow(x - xRange.start) / dx
                } else {
                    (x - xRange.start) / dx
                }

                // y location, normalize, then multiply by screen height, add starting y
                val yLoc: Float = if (yLogBase != null) {
                    (-y / dy) + origin.y
                } else {
                    (-y / dy) + origin.y
                }

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
                Timber.d("xValue:", x)
                Timber.d("yValue:", y)
                x += if (xLogBase != null) {
                    if (xLogMode == LogMode.Octave) {
                        log(dx, xLogBase)
                        //xLogBase.pow(dx)
                    } else {
                        log(dx, xLogBase)
                    }
                } else {
                    dx
                }
            }
            drawPath(path = eqnPath, color = Color.Black, style = Stroke(6f))
            paths.add(eqnPath)
        }

        if (colorIntersect) {
            // Complete bounded shapes from paths
            for (index in 0..paths.lastIndex) {
                paths[index].lineTo(size.width, size.width / 2)
                paths[index].lineTo(0f, size.width / 2)
                paths[index].lineTo(0f, y1[index])
            }
            val pathDiff = paths[0]
            for (index in 1..paths.lastIndex) {
                pathDiff.op(pathDiff, paths[index], PathOperation.Intersect)
            }

            drawPath(path = pathDiff, color = Color.Green, style = Fill)
        }
    }
}

data class GraphOrigin(val x: Float, val y: Float)

enum class LogMode {
    Power, Voltage, Octave
}


@Composable
@Preview
fun EqUITest() {
    ParametricEqUI(AudioModel.ParametricEQ(lp_freq = 10000f))
}