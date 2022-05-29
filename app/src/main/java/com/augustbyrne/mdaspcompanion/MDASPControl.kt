/*
 * Copyright 2019 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.augustbyrne.mdaspcompanion

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.augustbyrne.mdaspcompanion.ble.ConnectionManager
import java.util.*

/*class CharacteristicAdapter(
    private val items: List<BluetoothGattCharacteristic>,
    private val onClickListener: ((characteristic: BluetoothGattCharacteristic) -> Unit)
) : RecyclerView.Adapter<CharacteristicAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = parent.context.layoutInflater.inflate(
            R.layout.row_characteristic,
            parent,
            false
        )
        return ViewHolder(view, onClickListener)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    class ViewHolder(
        private val view: View,
        private val onClickListener: ((characteristic: BluetoothGattCharacteristic) -> Unit)
    ) : RecyclerView.ViewHolder(view) {

        fun bind(characteristic: BluetoothGattCharacteristic) {
            view.characteristic_uuid.text = characteristic.uuid.toString()
            view.characteristic_properties.text = characteristic.printProperties()
            view.setOnClickListener { onClickListener.invoke(characteristic) }
        }
    }
}*/

@SuppressLint("MissingPermission")
@Composable
fun MDASPControlUI(viewModel: MainViewModel) {
    val device by viewModel.device.observeAsState()

    //val characteristic = viewModel.characteristics
    var writeBytes by rememberSaveable { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(content = { Text("MDASP Audio Controller") })
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
                        BluetoothGattCharacteristic(
                            UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb"),
                            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                            BluetoothGattCharacteristic.PERMISSION_WRITE
                        ),
                        writeBytes.hexToBytes()
                    )
                }
            }
        ) {
            Icon(Icons.Default.OfflineBolt,"")
        }
    }
}

fun String.hexToBytes() = this.chunked(2).map { it.uppercase(Locale.US).toInt(16).toByte() }.toByteArray()

enum class CharacteristicProperty {
    Readable,
    Writable,
    WritableWithoutResponse,
    Notifiable,
    Indicatable;

    val action
        get() = when (this) {
            Readable -> "Read"
            Writable -> "Write"
            WritableWithoutResponse -> "Write Without Response"
            Notifiable -> "Toggle Notifications"
            Indicatable -> "Toggle Indications"
        }
}