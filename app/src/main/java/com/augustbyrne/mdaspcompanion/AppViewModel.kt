package com.augustbyrne.mdaspcompanion

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.augustbyrne.mdaspcompanion.ble.ConnectionEventListener
import com.augustbyrne.mdaspcompanion.ble.ConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(

): ViewModel() {

    var scanListState: SnapshotStateList<ScanResult> = mutableStateListOf()
    var isScanning by mutableStateOf(false)

    var device = MutableLiveData<BluetoothDevice?>()

    val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery =
                scanListState.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanListState[indexQuery] = result
            } else {
                scanListState.add(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("onScanFailed: code $errorCode")
        }
    }

    var audioModel: AudioModel = AudioModel()

        val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onCharacteristicRead = { _, characteristic ->
                audioModel.setFromByteArray(characteristic.value)
            }
            onConnectionSetupComplete = {
                runBlocking {
                    withContext(Dispatchers.Default) {
                        delay(400)
                        device.value?.let { it1 ->
                            ConnectionManager.readCharacteristic(
                                it1,
                                gattCharacteristicRead
                            )
                        }
                    }
                }

            }
        }
    }

    private val gattCharacteristicRead = BluetoothGattCharacteristic(
        UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb"),
        BluetoothGattCharacteristic.PROPERTY_READ,
        BluetoothGattCharacteristic.PERMISSION_READ
    )


}