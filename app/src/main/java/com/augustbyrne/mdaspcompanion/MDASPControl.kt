package com.augustbyrne.mdaspcompanion

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augustbyrne.mdaspcompanion.ble.ConnectionManager
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.log2
import kotlin.math.pow

private val gattCharacteristic = BluetoothGattCharacteristic(
        UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb"),
        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
        BluetoothGattCharacteristic.PERMISSION_WRITE
    )

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MDASPControlUI(viewModel: MainViewModel, onNavBack: () -> Unit) {
    val device by viewModel.device.observeAsState()
    var equalizer by viewModel.audioModel.eq
    var compressor by viewModel.audioModel.comp
    val iconTintEQ by animateColorAsState(
        targetValue = if (equalizer.passthrough) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        )
    )
    val iconTextTintEQ by animateColorAsState(
        targetValue = if (equalizer.passthrough) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        )
    )
    val iconTintComp by animateColorAsState(
        targetValue = if (compressor.passthrough) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        )
    )
    val iconTextTintComp by animateColorAsState(
        targetValue = if (compressor.passthrough) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        )
    )
    var liveMode by rememberSaveable { mutableStateOf(false) }
    val liveModeTint by animateColorAsState(
        targetValue = if (!liveMode) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        )
    )
    val liveModeTextTint by animateColorAsState(
        targetValue = if (!liveMode) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        )
    )
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        SmallTopAppBar(
            navigationIcon = {
                IconButton(
                    onClick = onNavBack
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = "back"
                    )
                }
            },
            title = { Text("MDASP Controller") },
            actions = {
                Button(
                    onClick = { liveMode = !liveMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = liveModeTint,
                        contentColor = liveModeTextTint
                    )
                ) {
                    Text(text = "Live Mode")
                }
            }
        )
        LazyColumn(
            Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            /** Parametric Equalizer UI **/
            item {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier.wrapContentSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Parametric EQ",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(Icons.Default.Info, "")
                            }
                        }
                        Button(
                            onClick = {
                                equalizer = equalizer.copy(passthrough = !equalizer.passthrough)
                                writeToGatt(device, "0000".hexToBytes(), equalizer.passthrough)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = iconTintEQ,
                                contentColor = iconTextTintEQ
                            )
                        ) {
                            Text(text = if (!equalizer.passthrough) "ON" else "OFF")
                        }
                    }
                    AnimatedVisibility(visible = !equalizer.passthrough) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("Highpass Filter")
                                        Row {
                                            FilterChip(
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                label = { Text("cut") },
                                                selected = equalizer.hp,
                                                onClick = {
                                                    equalizer = equalizer.copy(
                                                        hp = !equalizer.hp,
                                                        hs = false
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0002".hexToBytes(),
                                                        equalizer.hs
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0001".hexToBytes(),
                                                        equalizer.hp
                                                    )
                                                })
                                            FilterChip(
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                label = { Text("shelf") },
                                                selected = equalizer.hs,
                                                onClick = {
                                                    equalizer = equalizer.copy(
                                                        hs = !equalizer.hs,
                                                        hp = false
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0001".hexToBytes(),
                                                        equalizer.hp
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0002".hexToBytes(),
                                                        equalizer.hs
                                                    )
                                                })
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
                                        snapToDiscrete = false,
                                        value = if (equalizer.hp) log2(equalizer.hp_freq) else log2(
                                            equalizer.hs_freq
                                        ),
                                        onValueChange = { logVal ->
                                            if (equalizer.hp) {
                                                equalizer = equalizer.copy(hp_freq = 2f.pow(logVal))
                                                if (liveMode) writeToGatt(
                                                    device,
                                                    "0007".hexToBytes(),
                                                    equalizer.hp_freq / 48000f
                                                )
                                            } else if (equalizer.hs) {
                                                equalizer = equalizer.copy(hs_freq = 2f.pow(logVal))
                                                if (liveMode) writeToGatt(
                                                    device,
                                                    "0008".hexToBytes(),
                                                    equalizer.hs_freq / 48000f
                                                )
                                            }
                                        },
                                        onValueChangeFinished = {
                                            if (equalizer.hp) {
                                                if (!liveMode) writeToGatt(
                                                    device,
                                                    "0007".hexToBytes(),
                                                    equalizer.hp_freq / 48000f
                                                )
                                            } else if (equalizer.hs) {
                                                if (!liveMode) writeToGatt(
                                                    device,
                                                    "0008".hexToBytes(),
                                                    equalizer.hs_freq / 48000f
                                                )
                                            }
                                        },
                                        valueRange = 4.3219f..14.2877f  //aprox 20 to 20k in log2 (octave) scale
                                    )
                                }
                            }
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("Lowpass Filter")
                                        Row {
                                            FilterChip(
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                label = { Text("cut") },
                                                selected = equalizer.lp,
                                                onClick = {
                                                    equalizer = equalizer.copy(
                                                        lp = !equalizer.lp,
                                                        ls = false
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0005".hexToBytes(),
                                                        equalizer.ls
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0004".hexToBytes(),
                                                        equalizer.lp
                                                    )
                                                })
                                            FilterChip(
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                label = { Text("shelf") },
                                                selected = equalizer.ls,
                                                onClick = {
                                                    equalizer = equalizer.copy(
                                                        ls = !equalizer.ls,
                                                        lp = false
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0004".hexToBytes(),
                                                        equalizer.lp
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0005".hexToBytes(),
                                                        equalizer.ls
                                                    )
                                                })
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
                                        snapToDiscrete = false,
                                        value = if (equalizer.lp) log2(equalizer.lp_freq) else log2(
                                            equalizer.ls_freq
                                        ),
                                        onValueChange = { logVal ->
                                            equalizer = if (equalizer.lp) {
                                                equalizer.copy(lp_freq = 2f.pow(logVal))
                                            } else {
                                                equalizer.copy(ls_freq = 2f.pow(logVal))
                                            }
                                        },
                                        onValueChangeFinished = {
                                            if (equalizer.lp) {
                                                if (!liveMode) writeToGatt(
                                                    device,
                                                    "000A".hexToBytes(),
                                                    equalizer.hp_freq / 48000f
                                                )
                                            } else if (equalizer.ls) {
                                                if (!liveMode) writeToGatt(
                                                    device,
                                                    "000B".hexToBytes(),
                                                    equalizer.hs_freq / 48000f
                                                )
                                            }
                                        },
                                        valueRange = 4.3219f..14.2877f  //aprox 20 to 20k in log2 (octave) scale
                                    )
                                }
                            }
                        }
                    }
                }
            }
            /** Compressor UI **/
            item {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier.wrapContentSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Compressor",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(Icons.Default.Info, "")
                            }
                        }
                        Button(
                            onClick = {
                                compressor = compressor.copy(passthrough = !compressor.passthrough)
                                writeToGatt(device, "0100".hexToBytes(), compressor.passthrough)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = iconTintComp,
                                contentColor = iconTextTintComp
                            )
                        ) {
                            Text(text = if (!compressor.passthrough) "ON" else "OFF")
                        }
                    }
                    AnimatedVisibility(visible = !compressor.passthrough) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
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
                                    Text("Pregain")
                                    LabeledSlider(
                                        labels = listOf("0dB", "6", "12", "18", "24", "30", "36", "42", "48dB"),
                                        value = compressor.pregain,
                                        onValueChange = {
                                            compressor = compressor.copy(pregain = it)
                                            if (liveMode) writeToGatt(
                                                device,
                                                "0101".hexToBytes(),
                                                compressor.pregain
                                            )
                                        },
                                        snapToDiscrete = false,
                                        valueRange = (0f..48f),
                                        steps = 49,
                                        onValueChangeFinished = {
                                            if (!liveMode) writeToGatt(
                                                device,
                                                "0101".hexToBytes(),
                                                compressor.pregain
                                            )
                                        }
                                    )
                                }
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.9f
                                    )
                                )
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Text("Threshold")
                                    Slider(
                                        value = compressor.threshold,
                                        onValueChange = {
                                            compressor = compressor.copy(threshold = it)
                                            if (liveMode) writeToGatt(
                                                device,
                                                "0102".hexToBytes(),
                                                compressor.threshold
                                            )
                                        },
                                        enabled = !compressor.passthrough,
                                        valueRange = -96f..0f,
                                        steps = 97,
                                        onValueChangeFinished = {
                                            if (!liveMode) writeToGatt(
                                                device,
                                                "0102".hexToBytes(),
                                                compressor.threshold
                                            )
                                        }
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "-96dB",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = "0dB",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.9f
                                    )
                                )
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Text("Knee")
                                    Slider(
                                        value = compressor.knee,
                                        onValueChange = {
                                            compressor = compressor.copy(knee = it)
                                            if (liveMode) writeToGatt(
                                                device,
                                                "0103".hexToBytes(),
                                                compressor.knee
                                            )
                                        },
                                        enabled = !compressor.passthrough,
                                        valueRange = (0f..40f),
                                        steps = 41,
                                        onValueChangeFinished = {
                                            if (!liveMode) writeToGatt(
                                                device,
                                                "0103".hexToBytes(),
                                                compressor.knee
                                            )
                                        }
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "0dB",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = "40dB",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.9f
                                    )
                                )
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Text("Ratio")
                                    Slider(
                                        value = compressor.ratio,
                                        onValueChange = {
                                            compressor = compressor.copy(ratio = it)
                                            if (liveMode) writeToGatt(
                                                device,
                                                "0103".hexToBytes(),
                                                compressor.ratio
                                            )
                                        },
                                        enabled = !compressor.passthrough,
                                        valueRange = (1f..20f),
                                        steps = 20,
                                        onValueChangeFinished = {
                                            if (!liveMode) writeToGatt(
                                                device,
                                                "0103".hexToBytes(),
                                                compressor.ratio
                                            )
                                        }
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "1dB",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = "20dB",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable { showAdvanced = !showAdvanced },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.9f
                                    )
                                )
                            ) {
                                Row(Modifier.padding(8.dp)) {
                                    Text("advanced compressor settings")
                                    Icon(Icons.Default.ArrowDropDown, "")
                                }
                                AnimatedVisibility(visible = showAdvanced) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                value = compressor.attack.toString(),
                                                onValueChange = {
                                                    it.toFloatOrNull()?.let { value ->
                                                        if (value >= 0f) {
                                                            compressor =
                                                                compressor.copy(attack = value)
                                                        }
                                                    }
                                                },
                                                label = { Text("Attack (0 to 1 sec)") }
                                            )
                                            Button(onClick = {
                                                writeToGatt(
                                                    device,
                                                    "0104".hexToBytes(),
                                                    compressor.attack
                                                )
                                            }) { Text("apply") }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                value = compressor.release.toString(),
                                                onValueChange = {
                                                    it.toFloatOrNull()?.let { value ->
                                                        if (value >= 0f) {
                                                            compressor =
                                                                compressor.copy(release = value)
                                                        }
                                                    }
                                                },
                                                label = { Text("Release (0 to 1 sec)") }
                                            )
                                            Button(onClick = {
                                                writeToGatt(
                                                    device,
                                                    "0105".hexToBytes(),
                                                    compressor.release
                                                )
                                            }) { Text("apply") }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                value = compressor.predelay.toString(),
                                                onValueChange = {
                                                    it.toFloatOrNull()?.let { value ->
                                                        if (value >= 0f) {
                                                            compressor =
                                                                compressor.copy(predelay = value)
                                                        }
                                                    }
                                                },
                                                label = { Text("Predelay (0 to 1 sec)") }
                                            )
                                            Button(onClick = {
                                                writeToGatt(
                                                    device,
                                                    "0106".hexToBytes(),
                                                    compressor.predelay
                                                )
                                            }) { Text("apply") }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                value = compressor.attack.toString(),
                                                onValueChange = {
                                                    it.toFloatOrNull()?.let { value ->
                                                        if (value >= 0f) {
                                                            compressor =
                                                                compressor.copy(wet = value)
                                                        }
                                                    }
                                                },
                                                label = { Text("Wet (full dry at 0 to full wet at 1)") }
                                            )
                                            Button(onClick = {
                                                writeToGatt(
                                                    device,
                                                    "010C".hexToBytes(),
                                                    compressor.wet
                                                )
                                            }) { Text("apply") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun writeToGatt(device: BluetoothDevice?, writeLocation: ByteArray, payload: Any) {
    device?.let {
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
    val intBits = value.toRawBits()
    return byteArrayOf(
        intBits.toByte(),
        (intBits shr 8).toByte(),
        (intBits shr 16).toByte(),
        (intBits shr 24).toByte()
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
        val buffer = ByteBuffer.wrap(input).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        //byte1 = (myShort shr 8) as Byte
        //byte2 =(myShort shr 0) as Byte
        if (input.size == sizeInBytes || input.size == 100) {
            buffer.apply {
                eq.value = ParametricEQ(
                    passthrough = get().toBool().let { Timber.e("eqpass $it"); it },
                    hp = get().toBool().let { Timber.e("hp (1) $it"); it },
                    hs = get().toBool().let { Timber.e("hs (0) $it"); it },
                    br = get().toBool().let { Timber.e("br (0) $it"); it },
                    lp = get().toBool().let { Timber.e("lp (1) $it"); it },
                    ls = get().toBool().also { position(8) },//.apply { get() }.apply { get() },
                    gain = float.let { Timber.e("eqgain $it or (int) ${it.toInt()}"); it },
                    hp_freq = float.let { Timber.e("hp_freq $it or (int) ${it.toInt()}"); it },
                    hs_freq = float.let { Timber.e("hs_freq $it or (int) ${it.toInt()}"); it },
                    br_freq = float,
                    lp_freq = float,
                    ls_freq = float,
                    hs_amount = float,
                    br_amount = float,
                    ls_amount = float
                )
                comp.value = AdvancedCompressor(
                    passthrough = get().toBool().also { get() }.also { get() }.also { get() },
                    pregain = float.let { Timber.e("comp_pregain (12) $it or (int) ${it.toInt()}"); it },
                    threshold = float.let { Timber.e("comp_threshold (-64) $it or (int) ${it.toInt()}"); it },
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