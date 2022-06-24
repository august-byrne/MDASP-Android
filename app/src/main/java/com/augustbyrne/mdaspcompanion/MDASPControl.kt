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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
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
    val uriHandler = LocalUriHandler.current
    val device by viewModel.device.observeAsState()
    //val presetsList by viewModel.presetsList.observeAsState()
    var volume by viewModel.audioModel.volume
    var equalizer by viewModel.audioModel.eq
    var compressor by viewModel.audioModel.comp
    var delay by viewModel.audioModel.delay
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
    var showCompressorAdvanced by rememberSaveable { mutableStateOf(false) }
    var presetMenuToggle by rememberSaveable { mutableStateOf(false) }
    var newPresetDialog by rememberSaveable { mutableStateOf(false) }
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
                    modifier = Modifier.padding(end = 4.dp),
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
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Presets",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        DropdownMenu(expanded = presetMenuToggle, onDismissRequest = { presetMenuToggle = false }) {
                            // TODO: Create presets dropdown
/*                            for (presets in presetsList) {
                                DropdownMenuItem(text = { *//*TODO*//* }, onClick = {

                                })
                            }*/
                            DropdownMenuItem(
                                leadingIcon = { Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add a new preset"
                                ) },
                                text = { Text("New Preset") },
                                onClick = {
                                    presetMenuToggle = false
                                    newPresetDialog = true
                            })
                        }
                    }
                }
            }
            /** Volume Slider UI **/
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(8.dp)
                ) {
                    Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)) {
                        Text(
                            text = "Master Volume",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Slider(
                            value = volume,
                            onValueChange = {
                                volume = it
                                if (liveMode) writeToGatt(
                                    device,
                                    "0200".hexToBytes(),
                                    volume
                                )
                            },
                            valueRange = (-90f..0f),
                            steps = 0,
                            colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.surface),
                            onValueChangeFinished = {
                                if (!liveMode) writeToGatt(
                                    device,
                                    "0200".hexToBytes(),
                                    volume
                                )
                            }
                        )
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
/*                            .padding(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(
                                    alpha = 0.9f
                                )
                            )*/
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvanced = !showAdvanced }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Text("advanced settings")
                                if (!showAdvanced) {
                                    Icon(Icons.Default.ArrowDropDown, "")
                                } else {
                                    Icon(Icons.Default.ArrowDropUp, "")
                                }
                            }
                        }
                        AnimatedVisibility(visible = showAdvanced) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = delay.toString(),
                                        onValueChange = {
                                            it.toIntOrNull()?.let { value ->
                                                if (value in 0..400) {
                                                    delay = value
                                                }
                                            }
                                        },
                                        label = { Text("Delay (0 to 400 millisec)") },
                                        trailingIcon = {
                                            Button(
                                                modifier = Modifier.padding(end = 4.dp),
                                                onClick = {
                                                    writeToGatt(
                                                        device,
                                                        "0300".hexToBytes(),
                                                        delay
                                                    )
                                                }) { Text("apply") }
                                        }
                                    )
                                }
                                // TODO: Untested
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Set current audio parameters as device default")
                                    Button(onClick = { writeToGatt(device, "CAFE".hexToBytes()) }) {
                                        Text("apply")
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                            IconButton(onClick = {
                                uriHandler.openUri("https://en.wikipedia.org/wiki/Equalization_(audio)")
                            }) {
                                Icon(Icons.Default.Info, "EQ Info")
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
                                                        ls = false
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0005".hexToBytes(),
                                                        false
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0001".hexToBytes(),
                                                        equalizer.hp,
                                                        true
                                                    )
                                                })
                                            FilterChip(
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                label = { Text("shelf") },
                                                selected = equalizer.ls,
                                                onClick = {
                                                    equalizer = equalizer.copy(
                                                        ls = !equalizer.ls,
                                                        hp = false
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0001".hexToBytes(),
                                                        false
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0005".hexToBytes(),
                                                        equalizer.ls,
                                                        true
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
                                        enabled = (equalizer.hp || equalizer.ls),
                                        invertDirection = true,
                                        value = if (equalizer.hp) {
                                            log2(equalizer.hp_freq)
                                        } else if (equalizer.ls) {
                                            log2(equalizer.ls_freq)
                                        } else {
                                            4.3219f
                                               },
                                        onValueChange = { logVal ->
                                            if (equalizer.hp) {
                                                equalizer = equalizer.copy(hp_freq = 2f.pow(logVal))
                                                if (liveMode) writeToGatt(
                                                    device,
                                                    "0007".hexToBytes(),
                                                    equalizer.hp_freq / 48000f
                                                )
                                            } else if (equalizer.ls) {
                                                equalizer = equalizer.copy(ls_freq = 2f.pow(logVal))
                                                if (liveMode) writeToGatt(
                                                    device,
                                                    "000B".hexToBytes(),
                                                    equalizer.ls_freq / 48000f
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
                                            } else if (equalizer.ls) {
                                                if (!liveMode) writeToGatt(
                                                    device,
                                                    "000B".hexToBytes(),
                                                    equalizer.ls_freq / 48000f
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
                                                        hs = false
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0002".hexToBytes(),
                                                        false
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0004".hexToBytes(),
                                                        equalizer.lp,
                                                        true
                                                    )
                                                })
                                            FilterChip(
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                label = { Text("shelf") },
                                                selected = equalizer.hs,
                                                onClick = {
                                                    equalizer = equalizer.copy(
                                                        hs = !equalizer.hs,
                                                        lp = false
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0004".hexToBytes(),
                                                        false
                                                    )
                                                    writeToGatt(
                                                        device,
                                                        "0002".hexToBytes(),
                                                        equalizer.hs,
                                                        true
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
                                        enabled = (equalizer.lp || equalizer.hs),
                                        value = if (equalizer.lp) {
                                            log2(equalizer.lp_freq)
                                        } else if (equalizer.hs){
                                            log2(equalizer.hs_freq)
                                        } else {
                                            14.2877f
                                               },
                                        onValueChange = { logVal ->
                                            if (equalizer.lp) {
                                                equalizer = equalizer.copy(lp_freq = 2f.pow(logVal))
                                                if (liveMode) writeToGatt(
                                                    device,
                                                    "000A".hexToBytes(),
                                                    equalizer.lp_freq / 48000f
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
                                            if (equalizer.lp) {
                                                if (!liveMode) writeToGatt(
                                                    device,
                                                    "000A".hexToBytes(),
                                                    equalizer.lp_freq / 48000f
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
                            IconButton(onClick = {
                                uriHandler.openUri("https://en.wikipedia.org/wiki/Dynamic_range_compression")
                            }) {
                                Icon(Icons.Default.Info, "DRC Info")
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
                                        labels = listOf(
                                            "0dB",
                                            "6",
                                            "12",
                                            "18",
                                            "24",
                                            "30",
                                            "36",
                                            "42",
                                            "48dB"
                                        ),
                                        value = compressor.pregain,
                                        onValueChange = {
                                            compressor = compressor.copy(pregain = it)
                                            if (liveMode) writeToGatt(
                                                device,
                                                "0101".hexToBytes(),
                                                compressor.pregain
                                            )
                                        },
                                        valueRange = (0f..48f),
                                        steps = 47,
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("Threshold")
                                        FilterChip(
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            label = { Text("makeup gain") },
                                            selected = compressor.makeupgain,
                                            onClick = {
                                                compressor = compressor.copy(makeupgain = !compressor.makeupgain)
                                                writeToGatt(
                                                    device,
                                                    "010E".hexToBytes(),
                                                    compressor.makeupgain
                                                )
                                            })
                                    }
                                    LabeledSlider(
                                        labels = listOf(
                                            "-96dB",
                                            "-84",
                                            "-72",
                                            "-60",
                                            "-48",
                                            "-36",
                                            "-24",
                                            "-12",
                                            "0dB"
                                        ),
                                        value = compressor.threshold,
                                        onValueChange = {
                                            compressor = compressor.copy(threshold = it)
                                            if (liveMode) writeToGatt(
                                                device,
                                                "0102".hexToBytes(),
                                                compressor.threshold
                                            )
                                        },
                                        valueRange = -96f..0f,
                                        steps = 95,
                                        onValueChangeFinished = {
                                            if (!liveMode) writeToGatt(
                                                device,
                                                "0102".hexToBytes(),
                                                compressor.threshold
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
                                    Text("Knee")
                                    LabeledSlider(
                                        labels = listOf(
                                            "0dB",
                                            "10",
                                            "20",
                                            "30",
                                            "40dB"
                                        ),
                                        value = compressor.knee,
                                        onValueChange = {
                                            compressor = compressor.copy(knee = it)
                                            if (liveMode) writeToGatt(
                                                device,
                                                "0103".hexToBytes(),
                                                compressor.knee
                                            )
                                        },
                                        valueRange = (0f..40f),
                                        steps = 39,
                                        onValueChangeFinished = {
                                            if (!liveMode) writeToGatt(
                                                device,
                                                "0103".hexToBytes(),
                                                compressor.knee
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
                                    Text("Ratio")
                                    LabeledSlider(
                                        labels = listOf(
                                            "1:1",
                                            "4:1",
                                            "7:1",
                                            "10:1",
                                            "13:1",
                                            "16:1",
                                            "20:1"
                                        ),
                                        value = compressor.ratio,
                                        onValueChange = {
                                            compressor = compressor.copy(ratio = it)
                                            if (liveMode) writeToGatt(
                                                device,
                                                "0104".hexToBytes(),
                                                compressor.ratio
                                            )
                                        },
                                        valueRange = (1f..20f),
                                        steps = 18,
                                        onValueChangeFinished = {
                                            if (!liveMode) writeToGatt(
                                                device,
                                                "0104".hexToBytes(),
                                                compressor.ratio
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
                                    Text("Postgain")
                                    LabeledSlider(
                                        labels = listOf(
                                            "0dB",
                                            "6",
                                            "12",
                                            "18",
                                            "24",
                                            "30",
                                            "36",
                                            "42",
                                            "48dB"
                                        ),
                                        value = compressor.postgain,
                                        onValueChange = {
                                            compressor = compressor.copy(postgain = it)
                                            if (liveMode) writeToGatt(
                                                device,
                                                "010C".hexToBytes(),
                                                compressor.postgain
                                            )
                                        },
                                        valueRange = (0f..48f),
                                        steps = 47,
                                        onValueChangeFinished = {
                                            if (!liveMode) writeToGatt(
                                                device,
                                                "010C".hexToBytes(),
                                                compressor.postgain
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showCompressorAdvanced = !showCompressorAdvanced
                                        }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text("advanced compressor settings")
                                    if (!showCompressorAdvanced) {
                                        Icon(Icons.Default.ArrowDropDown, "")
                                    } else {
                                        Icon(Icons.Default.ArrowDropUp, "")
                                    }
                                }
                                AnimatedVisibility(visible = showCompressorAdvanced) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                modifier = Modifier.fillMaxWidth(),
                                                value = compressor.attack.toString(),
                                                onValueChange = {
                                                    it.toFloatOrNull()?.let { value ->
                                                        if (value in 0f..1f) {
                                                            compressor =
                                                                compressor.copy(attack = value)
                                                        }
                                                    }
                                                },
                                                label = { Text("Attack (0 to 1 sec)") },
                                                trailingIcon = {
                                                    Button(
                                                        modifier = Modifier.padding(end = 4.dp),
                                                        onClick = {
                                                        writeToGatt(
                                                            device,
                                                            "0105".hexToBytes(),
                                                            compressor.attack
                                                        )
                                                    }) { Text("apply") }
                                                }
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                modifier = Modifier.fillMaxWidth(),
                                                value = compressor.release.toString(),
                                                onValueChange = {
                                                    it.toFloatOrNull()?.let { value ->
                                                        if (value in 0f..1f) {
                                                            compressor =
                                                                compressor.copy(release = value)
                                                        }
                                                    }
                                                },
                                                label = { Text("Release (0 to 1 sec)") },
                                                trailingIcon = {
                                                    Button(
                                                        modifier = Modifier.padding(end = 4.dp),
                                                        onClick = {
                                                        writeToGatt(
                                                            device,
                                                            "0106".hexToBytes(),
                                                            compressor.release
                                                        )
                                                    }) { Text("apply") }
                                                }
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                modifier = Modifier.fillMaxWidth(),
                                                value = compressor.predelay.toString(),
                                                onValueChange = {
                                                    it.toFloatOrNull()?.let { value ->
                                                        if (value in 0f..1f) {
                                                            compressor =
                                                                compressor.copy(predelay = value)
                                                        }
                                                    }
                                                },
                                                label = { Text("Predelay (0 to 1 sec)") },
                                                trailingIcon = {
                                                    Button(
                                                        modifier = Modifier.padding(end = 4.dp),
                                                        onClick = {
                                                        writeToGatt(
                                                            device,
                                                            "0107".hexToBytes(),
                                                            compressor.predelay
                                                        )
                                                    }) { Text("apply") }
                                                }
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                modifier = Modifier.fillMaxWidth(),
                                                value = compressor.wet.toString(),
                                                onValueChange = {
                                                    it.toFloatOrNull()?.let { value ->
                                                        if (value in 0f..1f) {
                                                            compressor =
                                                                compressor.copy(wet = value)
                                                        }
                                                    }
                                                },
                                                label = { Text("Wet (full dry at 0 to full wet at 1)") },
                                                trailingIcon = {
                                                    Button(
                                                        modifier = Modifier.padding(end = 4.dp),
                                                        onClick = {
                                                        writeToGatt(
                                                            device,
                                                            "010D".hexToBytes(),
                                                            compressor.wet
                                                        )
                                                    }) { Text("apply") }
                                                }
                                            )
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

private var prevTime: Long = 0L

fun writeToGatt(device: BluetoothDevice?, writeLocation: ByteArray, payload: Any? = null, ignoreDelay: Boolean = false) {
    val newTime: Long = System.currentTimeMillis()
    if (ignoreDelay || (newTime - prevTime) > 14) {
        prevTime = newTime
        device?.let {
            val writeBytes: ByteArray? = when (payload) {
                is Float -> {
                    floatToByteArray(payload)
                }
                is Boolean -> {
                    byteArrayOf(payload.toByte())
                }
                is Int -> {
                    intToByteArray(payload)
                }
                else -> {
                    null
                }
            }
            val buff = ByteBuffer.wrap(ByteArray(writeLocation.size + (writeBytes?.size ?: 0)))
            buff.put(writeLocation)
            writeBytes?.let { it1 -> buff.put(it1) }
            ConnectionManager.writeCharacteristic(
                it,
                gattCharacteristic,
                buff.array()
            )
        }
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

fun intToByteArray(value: Int): ByteArray {
    return byteArrayOf(
        value.toByte(),
        (value shr 8).toByte(),
        (value shr 16).toByte(),
        (value shr 24).toByte()
    )
}

fun String.hexToBytes() = this.chunked(2).map { it.uppercase(Locale.US).toInt(16).toByte() }.toByteArray()

fun Byte.toBool() = this.toInt() != 0

fun Boolean.toByte() = if (this) 1.toByte() else 0.toByte()

data class AudioModel(
    var volume: MutableState<Float> = mutableStateOf(0.0f),
    var delay: MutableState<Int> = mutableStateOf(0),
    var eq: MutableState<ParametricEQ> = mutableStateOf(ParametricEQ()),
    var comp: MutableState<AdvancedCompressor> = mutableStateOf(AdvancedCompressor())
) {

    fun setFromByteArray(input: ByteArray) {
        val buffer = ByteBuffer.wrap(input).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        if (input.size == 108) {
            buffer.apply {
                eq.value = ParametricEQ(
                    passthrough = get().toBool().let { Timber.e("eq_passthrough $it"); it },
                    hp = get().toBool().let { Timber.e("hp: $it"); it },
                    hs = get().toBool().let { Timber.e("hs: $it"); it },
                    br = get().toBool().let { Timber.e("br: $it"); it },
                    lp = get().toBool().let { Timber.e("lp: $it"); it },
                    ls = get().toBool().also { position(8) },
                    gain = float.let { Timber.e("eqgain $it"); it },
                    hp_freq = float.let { Timber.e("hp_freq $it"); it },
                    hs_freq = float.let { Timber.e("hs_freq $it"); it },
                    br_freq = float,
                    lp_freq = float,
                    ls_freq = float,
                    hs_amount = float,
                    br_amount = float,
                    ls_amount = float
                )
                comp.value = AdvancedCompressor(
                    passthrough = get().toBool(),
                    makeupgain = get().toBool().also { get() }.also { get() },
                    pregain = float.let { Timber.e("comp_pregain (12) $it"); it },
                    threshold = float.let { Timber.e("comp_threshold (-64) $it"); it },
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
                volume.value = float
                delay.value = int
            }
            Timber.e("AudioModel is all set!")
        } else {
            Timber.e("Size of AudioModel should be 108 bytes but input is size ${input.size}")
        }
    }

    data class ParametricEQ(
        val passthrough: Boolean = true,
        val hp: Boolean = false,
        val hs: Boolean = false,
        val br: Boolean = false,
        val lp: Boolean = false,
        val ls: Boolean = false,
        val gain: Float = 0f,
        val hp_freq: Float = 0f,
        val hs_freq: Float = 24000.0f,
        val br_freq: Float = 0f,
        val lp_freq: Float = 24000.0f,
        val ls_freq: Float = 0f,
        val hs_amount: Float = -20.0f,
        val br_amount: Float = -20.0f,
        val ls_amount: Float = -20.0f
    )

    data class AdvancedCompressor(
        val passthrough: Boolean = true,
        val makeupgain: Boolean = true,
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