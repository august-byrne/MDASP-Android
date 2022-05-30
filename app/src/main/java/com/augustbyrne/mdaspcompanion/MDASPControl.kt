package com.augustbyrne.mdaspcompanion

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.augustbyrne.mdaspcompanion.ble.ConnectionEventListener
import com.augustbyrne.mdaspcompanion.ble.ConnectionManager
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.*

private val gattCharacteristic = BluetoothGattCharacteristic(
        UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb"),
        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
        BluetoothGattCharacteristic.PERMISSION_WRITE
    )




@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MDASPControlUI(viewModel: MainViewModel) {
    val device by viewModel.device.observeAsState()
    var equalizer by viewModel.audioModel.eq
    var compressor by viewModel.audioModel.comp
    //val characteristic = viewModel.characteristics
    var writeBytes by rememberSaveable { mutableStateOf("") }
    val iconTintEQ by animateColorAsState(
        targetValue = if (equalizer.passthrough) Color.Red else Color.Green,
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearEasing
        )
    )
    val iconTintComp by animateColorAsState(
        targetValue = if (compressor.passthrough) Color.Red else Color.Green,
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearEasing
        )
    )
    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(title = { Text("MDASP Audio Controller") })

        Card(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.wrapContentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Parametric EQ", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Default.Info, "")
                    }
                }
                Button(onClick = {
                    //equalizer.passthrough = !equalizer.passthrough
                    equalizer = equalizer.copy(passthrough = !equalizer.passthrough)
                    writeToGatt(device, "0000".hexToBytes(), equalizer.passthrough)
                }, colors = ButtonDefaults.buttonColors(containerColor = iconTintEQ)) {
                    Text(text = if (!equalizer.passthrough) "ON" else "OFF")
                }
            }
        }
        Card(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.wrapContentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Compressor", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Default.Info, "")
                    }
                }
                Button(onClick = {
                    compressor = compressor.copy(passthrough = !compressor.passthrough)
                    writeToGatt(device, "0100".hexToBytes(), compressor.passthrough)
                }, colors = ButtonDefaults.buttonColors(containerColor = iconTintComp)) {
                    Text(text = if (!compressor.passthrough) "ON" else "OFF")
                }
            }

        }
        TextField(
            value = writeBytes,
            onValueChange = {
                writeBytes = it
            }
        )
        Button(
            onClick = {
                //val bytes = hexToBytes()
                //Timber.d("Writing to ${characteristic.uuid}: ${bytes.toHexString()}")
                device?.let {
                    ConnectionManager.writeCharacteristic(
                        it,
                        gattCharacteristic,
                        writeBytes.hexToBytes()
                    )
                }
            }
        ) {
            Icon(Icons.Default.OfflineBolt, "")
        }
    }
}

fun writeToGatt(device: BluetoothDevice?, writeLocation: ByteArray, payload: Any) {
    device?.let {
        //var writeBytes: ByteArray? = null
        val writeBytes: ByteArray = when (payload) {
            is Float -> {
                floatToByteArray(payload)
            }
            is Boolean -> {
                payload.toByte()
            }
            else -> {
                return
            }
        }
        val buff = ByteBuffer.wrap(ByteArray(writeLocation.size + writeBytes.size))
        buff.put(writeLocation)
        buff.put(writeBytes)
        ConnectionManager.writeCharacteristic(
            it,
            gattCharacteristic,
            buff.array()
        )
    }
}

fun floatToByteArray(value: Float): ByteArray {
    val intBits = value.toRawBits()//java.lang.Float.floatToIntBits(value)
    return byteArrayOf(
        (intBits shr 24).toByte(),
        (intBits shr 16).toByte(),
        (intBits shr 8).toByte(),
        intBits.toByte()
    )
}

fun String.hexToBytes() = this.chunked(2).map { it.uppercase(Locale.US).toInt(16).toByte() }.toByteArray()

fun Byte.toBool() = this.toInt() != 0

fun Boolean.toByte() = if (this) "01".hexToBytes() else "00".hexToBytes()

data class AudioModel(
    var eq: MutableState<ParametricEQ> = mutableStateOf(ParametricEQ()),
    var comp: MutableState<AdvancedCompressor> = mutableStateOf(AdvancedCompressor())
) {

    private val sizeInBytes: Int = 51

    fun setFromByteArray(input: ByteArray) {
        val buffer = ByteBuffer.wrap(input).asReadOnlyBuffer()
        if (input.size == sizeInBytes || input.size == 100) {
            buffer.apply {
                eq.value = ParametricEQ(
                    passthrough = get().toBool(),
                    hp = get().toBool(),
                    hs = get().toBool(),
                    br = get().toBool(),
                    lp = get().toBool(),
                    ls = get().toBool().also { get() }.also { get() },
                    gain = float,
                    hp_freq = float,
                    hs_freq = float,
                    br_freq = float,
                    lp_freq = float,
                    ls_freq = float,
                    hs_amount = float,
                    br_amount = float,
                    ls_amount = float
                )
                comp.value = AdvancedCompressor(
                    passthrough = get().toBool().also { get() }.also { get() }.also { get() },
                    pregain = float,
                    threshold = float,
                    knee = float,
                    ratio = float,
                    attack = float,
                    release = float,
                    predelay = float,
                    releasezone1 = float,
                    releasezone2 = float,
                    releasezone3 = float,
                    releasezone4 = float,
                    postgain = float,
                    wet = float
                )
            }
            Timber.e("AudioModel is all set!")
        } else {
            Timber.e("Size of AudioModel is $sizeInBytes but input is size ${input.size}")
        }
    }

    data class ParametricEQ( // 15 elements -> 24 bytes
        val passthrough: Boolean = true,
        val hp: Boolean = false,
        val hs: Boolean = false,
        val br: Boolean = false,
        val lp: Boolean = false,
        val ls: Boolean = false,
        val gain: Float = 0f,
        val hp_freq: Float = 0f,
        val hs_freq: Float = 0f,
        val br_freq: Float = 0f,
        val lp_freq: Float = 0f,
        val ls_freq: Float = 0f,
        val hs_amount: Float = 0f,
        val br_amount: Float = 0f,
        val ls_amount: Float = 0f
    )

    data class AdvancedCompressor( // 14 elements -> 27 bytes
        val passthrough: Boolean = true,
        val pregain: Float = 0f,
        val threshold: Float = 0f,
        val knee: Float = 0f,
        val ratio: Float = 0f,
        val attack: Float = 0f,
        val release: Float = 0f,
        val predelay: Float = 0f,
        val releasezone1: Float = 0f,
        val releasezone2: Float = 0f,
        val releasezone3: Float = 0f,
        val releasezone4: Float = 0f,
        val postgain: Float = 0f,
        val wet: Float = 0f
    )
}