package com.augustbyrne.mdaspcompanion

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.augustbyrne.mdaspcompanion.ble.ConnectionManager
import com.google.accompanist.permissions.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun ScanListUI(viewModel: MainViewModel, onClickDevice: (ScanResult) -> Unit) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val btAdapter = bluetoothManager.adapter
    val btScanner = btAdapter.bluetoothLeScanner
    val requiredPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rememberMultiplePermissionsState(
                permissions = listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            rememberMultiplePermissionsState(permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = {
                Text("MDASP Controller")
            }
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 72.dp)
        ) {
            items(viewModel.scanListState.sortedByDescending { it.rssi }) { scannedItem: ScanResult ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    onClick = {
                        stopBleScan(viewModel, btScanner)
                        onClickDevice(scannedItem)
                    }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            //Text("MDASP Audio Processor")
                            Text(scannedItem.device.name ?: "Unknown Name")
                            Spacer(Modifier.height(4.dp))
                            Text("MAC Address: ${scannedItem.device.address}")
                        }
                        Text("${scannedItem.rssi} dBm")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ExtendedFloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            onClick = {
                if (!viewModel.isScanning) startBleScan(
                    viewModel,
                    btScanner,
                    requiredPermissions
                ) else stopBleScan(viewModel, btScanner)
            }
        ) {
            Text(text = "Scan for MDASP")
            if (!viewModel.isScanning) Icon(
                Icons.Default.PlayArrow,
                ""
            ) else Icon(Icons.Default.Stop, "")
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
fun startBleScan(myViewModel: MainViewModel, btScanner: BluetoothLeScanner, permissions: MultiplePermissionsState) {
    if (!permissions.allPermissionsGranted) {
        permissions.launchMultiplePermissionRequest()
    } else {
        myViewModel.scanListState.clear()
        myViewModel.device.value?.let { ConnectionManager.teardownConnection(it) }
        btScanner.startScan(
            listOf(ScanFilter.Builder().setDeviceName("MDASP Audio Processor").build()),
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
            myViewModel.scanCallback
        )
        myViewModel.isScanning = true
    }
}

@SuppressLint("MissingPermission")
fun stopBleScan(viewModel: MainViewModel, btScanner: BluetoothLeScanner) {
    btScanner.flushPendingScanResults(viewModel.scanCallback)
    btScanner.stopScan(viewModel.scanCallback)
    viewModel.isScanning = false
}
