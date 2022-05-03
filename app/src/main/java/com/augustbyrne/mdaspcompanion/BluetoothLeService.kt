package com.augustbyrne.mdaspcompanion

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class BluetoothLeService : Service() {

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService() : BluetoothLeService {
            return this@BluetoothLeService
        }
    }
}